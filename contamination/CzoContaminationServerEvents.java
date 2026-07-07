package com.czo.contamination;

import com.czo.CZO;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CZO.MODID)
public final class CzoContaminationServerEvents {
    // Зона заражения действует только в своём X/Z-блоке, но по всей высоте колонны.
    private static final int COLUMN_SCAN_RADIUS = 0;
    private static final int COLUMN_MIN_Y = -64;
    private static final int COLUMN_MAX_Y = 500;
    private static final double DECAY_PER_SECOND = 10.0D;
    private static final int MILK_CLEAR_MIN_TICKS = 24;

    private static final double LIGHT_THRESHOLD = 1_000.0D;
    private static final double MEDIUM_THRESHOLD = 10_000.0D;
    private static final double HEAVY_THRESHOLD = 100_000.0D;
    private static final double LETHAL_THRESHOLD = 1_000_000.0D;

    private static final Map<UUID, ContaminationData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MILK_USE_TICKS = new ConcurrentHashMap<>();

    private CzoContaminationServerEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearContamination(player, false);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity rawEntity = event.getEntity();

        if (!(rawEntity instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!player.isAlive()) {
            clearContamination(player, false);
            return;
        }

        if (handleMilkUse(player)) {
            return;
        }

        if (player.isSpectator() || player.isCreative()) {
            MILK_USE_TICKS.remove(player.getUUID());
            return;
        }

        long gameTime = level.getGameTime();

        if (gameTime % 20L != 0L) {
            return;
        }

        ContaminationData data = PLAYER_DATA.computeIfAbsent(player.getUUID(), id -> new ContaminationData());
        Map<ContaminationType, Integer> exposure = scanExposure(level, player);

        for (ContaminationType type : ContaminationType.values()) {
            int zoneLevel = exposure.getOrDefault(type, 0);

            if (zoneLevel > 0) {
                int protection = ContaminationProtection.getProtection(player, type);
                double gain = gainPerSecond(zoneLevel, protection);
                data.add(type, gain);
            } else {
                data.reduce(type, DECAY_PER_SECOND);
            }
        }

        applySymptoms(level, player, data);
        // Stage 7A: числовой статус заражения больше не спамит actionbar.
        // Визуальный статус теперь рисуется клиентским CZO HUD-виджетом над шкалой HP.

        if (data.isEmpty()) {
            PLAYER_DATA.remove(player.getUUID());
        }
    }

    private static boolean handleMilkUse(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isUsingItem() && player.getUseItem().is(Items.MILK_BUCKET)) {
            int ticks = MILK_USE_TICKS.getOrDefault(playerId, 0) + 1;
            MILK_USE_TICKS.put(playerId, ticks);
            return false;
        }

        Integer usedTicks = MILK_USE_TICKS.remove(playerId);

        if (usedTicks != null && usedTicks >= MILK_CLEAR_MIN_TICKS) {
            clearContamination(player, true);
            return true;
        }

        return false;
    }

    private static void clearContamination(ServerPlayer player, boolean notify) {
        UUID playerId = player.getUUID();
        boolean hadData = PLAYER_DATA.remove(playerId) != null;
        MILK_USE_TICKS.remove(playerId);

        if (notify && hadData) {
            player.connection.send(
                    new ClientboundSetActionBarTextPacket(Component.literal("Заражения очищены"))
            );
        }
    }

    private static Map<ContaminationType, Integer> scanExposure(ServerLevel level, ServerPlayer player) {
        Map<ContaminationType, Integer> exposure = new EnumMap<>(ContaminationType.class);

        BlockPos basePos = player.blockPosition();
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();

        // Заражение работает как вертикальная колонна строго в своём X/Z-блоке:
        // блок зоны можно поставить под/над локацией, но радиуса 2 блока больше нет.
        for (int x = basePos.getX() - COLUMN_SCAN_RADIUS; x <= basePos.getX() + COLUMN_SCAN_RADIUS; x++) {
            for (int z = basePos.getZ() - COLUMN_SCAN_RADIUS; z <= basePos.getZ() + COLUMN_SCAN_RADIUS; z++) {
                for (int y = COLUMN_MIN_Y; y <= COLUMN_MAX_Y; y++) {
                    scanPos.set(x, y, z);
                    ContaminationType type = CzoContaminations.getType(level.getBlockState(scanPos).getBlock());

                    if (type == null) {
                        continue;
                    }

                    int levelValue = CzoContaminations.getLevel(level.getBlockState(scanPos).getBlock());
                    int current = exposure.getOrDefault(type, 0);

                    if (levelValue > current) {
                        exposure.put(type, levelValue);
                    }
                }
            }
        }

        return exposure;
    }

    private static double gainPerSecond(int zoneLevel, int protection) {
        int safeProtection = Math.max(0, protection);
        int fullProtectedLevels = safeProtection / 100;
        int partialProtection = safeProtection % 100;

        if (zoneLevel <= fullProtectedLevels) {
            return 0.0D;
        }

        int effectiveLevel = zoneLevel - fullProtectedLevels;
        double baseGain = Math.pow(10.0D, effectiveLevel);

        if (effectiveLevel == 1 && partialProtection > 0) {
            return baseGain * (100.0D - partialProtection) / 100.0D;
        }

        return baseGain;
    }

    private static void applySymptoms(ServerLevel level, ServerPlayer player, ContaminationData data) {
        for (ContaminationType type : ContaminationType.values()) {
            double value = data.get(type);

            if (value >= LETHAL_THRESHOLD) {
                kill(level, player);
                return;
            }

            switch (type) {
                case RADIATION -> applyRadiation(level, player, value);
                case BIOLOGICAL -> applyBiological(level, player, value);
                case THERMAL -> applyThermal(level, player, value);
                case PSY -> applyPsy(level, player, value);
            }
        }
    }

    private static void applyRadiation(ServerLevel level, ServerPlayer player, double value) {
        if (value >= HEAVY_THRESHOLD) {
            addNausea(player, 2);
            hurt(level, player, 10.0F);
        } else if (value >= MEDIUM_THRESHOLD) {
            addNausea(player, 1);
            hurt(level, player, 1.0F);
        } else if (value >= LIGHT_THRESHOLD) {
            addSlowdown(player, 0);
            addNausea(player, 0);
        }
    }

    private static void applyBiological(ServerLevel level, ServerPlayer player, double value) {
        if (value >= HEAVY_THRESHOLD) {
            addNausea(player, 2);
            hurt(level, player, 10.0F);
        } else if (value >= MEDIUM_THRESHOLD) {
            addNausea(player, 1);
            hurt(level, player, 1.0F);
        } else if (value >= LIGHT_THRESHOLD) {
            addSlowdown(player, 0);
            addNausea(player, 0);
        }
    }

    private static void applyThermal(ServerLevel level, ServerPlayer player, double value) {
        if (value >= LIGHT_THRESHOLD) {
            player.igniteForSeconds(2.0F);
        }

        if (value >= HEAVY_THRESHOLD) {
            hurt(level, player, 10.0F);
        } else if (value >= MEDIUM_THRESHOLD) {
            hurt(level, player, 1.0F);
        }
    }

    private static void applyPsy(ServerLevel level, ServerPlayer player, double value) {
        if (value >= HEAVY_THRESHOLD) {
            addSlowdown(player, 2);
            addNausea(player, 2);
        } else if (value >= MEDIUM_THRESHOLD) {
            addSlowdown(player, 1);
            addNausea(player, 1);
        } else if (value >= LIGHT_THRESHOLD) {
            addSlowdown(player, 0);
            addNausea(player, 0);
        }
    }

    private static void addSlowdown(ServerPlayer player, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, amplifier, false, true, true));
    }

    private static void addNausea(ServerPlayer player, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 45, amplifier, false, true, true));
    }

    private static void sendStatus(ServerPlayer player, ContaminationData data, Map<ContaminationType, Integer> exposure) {
        boolean shouldShow = false;

        for (ContaminationType type : ContaminationType.values()) {
            if (data.get(type) >= LIGHT_THRESHOLD || exposure.getOrDefault(type, 0) > 0) {
                shouldShow = true;
                break;
            }
        }

        if (!shouldShow) {
            return;
        }

        String text = formatStatus(data);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }

    private static String formatStatus(ContaminationData data) {
        return ContaminationType.RADIATION.symbol() + " " + formatValue(data.get(ContaminationType.RADIATION))
                + "  " + ContaminationType.PSY.symbol() + " " + formatValue(data.get(ContaminationType.PSY))
                + "  " + ContaminationType.BIOLOGICAL.symbol() + " " + formatValue(data.get(ContaminationType.BIOLOGICAL))
                + "  " + ContaminationType.THERMAL.symbol() + " " + formatValue(data.get(ContaminationType.THERMAL));
    }

    private static String formatValue(double value) {
        if (value <= 0.0D) {
            return "0";
        }

        if (value >= 1000.0D) {
            return String.valueOf((int) Math.round(value));
        }

        return String.valueOf(Math.round(value * 10.0D) / 10.0D);
    }

    private static double flatDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void hurt(ServerLevel level, ServerPlayer player, float damage) {
        player.hurtServer(level, player.damageSources().generic(), damage);
    }

    private static void kill(ServerLevel level, ServerPlayer player) {
        clearContamination(player, false);
        player.hurtServer(level, player.damageSources().generic(), Math.max(1000.0F, player.getMaxHealth() * 20.0F));
    }
}
