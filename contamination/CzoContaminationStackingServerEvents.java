package com.czo.contamination;

import com.czo.CZO;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Supplemental contamination stacking layer.
 *
 * <p>The old contamination logic can still handle one active source normally. This class only
 * adds the missing "bad place got worse" behaviour: when two or more different contamination
 * types overlap on the player, each unprotected type contributes its own status/effect packet.
 * That lets RAD + THERMAL, RAD + PSY, RAD + BIO + THERMAL etc. stack instead of one type
 * visually/medically masking the others.</p>
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoContaminationStackingServerEvents {
    private static final int COLUMN_MIN_Y = -64;
    private static final int COLUMN_MAX_Y = 500;

    private CzoContaminationStackingServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }

        // Once per second is enough: contamination accumulation is per-second by design.
        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        EnumMap<ContaminationType, Integer> active = scanCurrentColumn(level, player);
        removeFullyProtectedTypes(player, active);
        if (active.size() < 2) {
            return;
        }

        int overlapCount = active.size();
        for (Map.Entry<ContaminationType, Integer> entry : active.entrySet()) {
            applyStackingEffect(level, player, entry.getKey(), Math.max(1, entry.getValue()), overlapCount);
        }
    }

    private static EnumMap<ContaminationType, Integer> scanCurrentColumn(ServerLevel level, ServerPlayer player) {
        EnumMap<ContaminationType, Integer> result = new EnumMap<>(ContaminationType.class);
        BlockPos base = player.blockPosition();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

        int minY = Math.max(level.getMinY(), COLUMN_MIN_Y);
        int maxY = Math.min(level.getMinY() + level.getHeight() - 1, COLUMN_MAX_Y);
        for (int y = minY; y <= maxY; y++) {
            scan.set(base.getX(), y, base.getZ());
            BlockState state = level.getBlockState(scan);
            ContaminationType type = CzoContaminations.getType(state.getBlock());
            if (type == null) {
                continue;
            }
            int zoneLevel = Math.max(1, CzoContaminations.getLevel(state.getBlock()));
            result.merge(type, zoneLevel, Math::max);
        }
        return result;
    }

    private static void removeFullyProtectedTypes(ServerPlayer player, EnumMap<ContaminationType, Integer> active) {
        active.entrySet().removeIf(entry -> {
            int required = Math.max(1, Math.min(5, entry.getValue())) * 100;
            int protection = Math.max(0, ContaminationProtection.getProtection(player, entry.getKey()));
            return protection >= required;
        });
    }

    private static void applyStackingEffect(ServerLevel level, ServerPlayer player, ContaminationType type, int zoneLevel, int overlapCount) {
        int amp = Math.max(0, Math.min(3, zoneLevel - 1));
        int duration = 45;
        float pressure = Math.max(1.0F, zoneLevel * Math.max(1, overlapCount - 1));

        switch (type) {
            case RADIATION -> {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, amp, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, Math.min(2, amp), false, true, true));
                hurt(player, level, pressure);
            }
            case BIOLOGICAL -> {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, Math.min(2, amp), false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, Math.min(2, amp), false, true, true));
                hurt(player, level, pressure);
            }
            case THERMAL -> {
                player.setRemainingFireTicks(45);
                hurt(player, level, pressure * 2.0F);
            }
            case PSY -> {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, Math.min(3, amp + 1), false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, Math.min(3, amp + 1), false, true, true));
                hurt(player, level, pressure);
            }
        }
    }

    private static void hurt(ServerPlayer player, ServerLevel level, float amount) {
        player.hurtServer(level, player.damageSources().generic(), Math.max(0.5F, amount));
    }
}
