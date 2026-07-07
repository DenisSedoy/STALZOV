package com.czo.client;

import com.czo.CZO;
import com.czo.anomaly.AnomalyType;
import com.czo.anomaly.CzoAnomalies;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoAnomalyClientEvents {
    private static final double VORTEX_RADIUS = 2.0D;
    private static final double CAROUSEL_RADIUS = 2.0D;
    private static final double ELECTRO_RADIUS = 2.0D;
    private static final double ELECTROSTATIC_RADIUS = 2.0D;
    private static final double GAS_APPROACH_RADIUS = 5.0D;
    private static final double GAS_TRIGGER_RADIUS = 2.0D;
    private static final int GAS_CLIENT_COOLDOWN_TICKS = 60;
    private static final int ELECTRO_CLIENT_COOLDOWN_TICKS = 60;

    private static final Map<Long, Long> GAS_PARTICLE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<Long, Long> ELECTRO_PARTICLE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    private CzoAnomalyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;

        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }

        BlockPos playerBlockPos = player.blockPosition();
        BlockPos min = playerBlockPos.offset(-10, -5, -10);
        BlockPos max = playerBlockPos.offset(10, 6, 10);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            AnomalyType type = CzoAnomalies.getType(level.getBlockState(pos).getBlock());

            if (type != null) {
                spawnParticles(level, player, pos.immutable(), type);
                continue;
            }

            String path = czoBlockPath(level, pos);
            if (path.isEmpty() || !path.contains("anomaly")) {
                continue;
            }

            spawnStandaloneAnomalyParticles(level, pos.immutable(), path);
        }
    }


    private static void spawnStandaloneAnomalyParticles(ClientLevel level, BlockPos pos, String path) {
        if (isZeroGravity(path)) {
            spawnZeroGravityParticles(level, pos);
        }

        if (isCobra(path)) {
            spawnCobraParticles(level, pos);
        }

        if (isBubble(path)) {
            spawnBubbleParticles(level, pos);
        }
    }

    private static String czoBlockPath(ClientLevel level, BlockPos pos) {
        var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (id == null || !CZO.MODID.equals(id.getNamespace())) {
            return "";
        }
        return id.getPath().toLowerCase(Locale.ROOT);
    }

    private static boolean isZeroGravity(String path) {
        return path.contains("zero_gravity") || path.contains("null_gravity") || path.contains("nevesomost") || path.contains("невесом");
    }

    private static boolean isCobra(String path) {
        return path.contains("cobra") || path.contains("кобра");
    }

    private static boolean isBubble(String path) {
        return path.contains("bubble") || path.contains("пузыр");
    }

    private static void spawnParticles(ClientLevel level, Player player, BlockPos pos, AnomalyType type) {
        switch (type) {
            case BURNER -> spawnBurnerParticles(level, player, pos, ParticleTypes.FLAME, ParticleTypes.LAVA, 1.0D);
            case INFERNO -> spawnBurnerParticles(level, player, pos, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SOUL_FIRE_FLAME, 1.35D);
            case STEAM -> spawnSteamParticles(level, player, pos);
            case GAS -> spawnGasParticles(level, player, pos);

            case VORTEX -> spawnVortexParticles(level, pos);
            case CAROUSEL -> spawnCarouselParticles(level, pos);
            case HYDRAULIC -> spawnHydraulicParticles(level, player, pos);

            case ELECTRO -> spawnElectroParticles(level, player, pos);
            case ELECTROSTATIC -> spawnElectrostaticParticles(level, pos);

            case ACID -> spawnAcidParticles(level, pos);
            case SPRINGBOARD -> spawnSpringboardParticles(level, pos);
        }
    }

    private static void spawnBurnerParticles(
            ClientLevel level,
            Player player,
            BlockPos pos,
            ParticleOptions flameParticle,
            ParticleOptions burstParticle,
            double intensity
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        int idleCount = Math.max(1, (int) Math.round(3 * intensity));

        for (int i = 0; i < idleCount; i++) {
            if (random.nextFloat() > 0.55F) {
                continue;
            }

            double x = center.x + random.nextDouble(-0.55D, 0.55D);
            double y = pos.getY() + random.nextDouble(0.05D, 0.85D);
            double z = center.z + random.nextDouble(-0.55D, 0.55D);
            level.addParticle(flameParticle, x, y, z, 0.0D, random.nextDouble(0.015D, 0.065D) * intensity, 0.0D);
        }

        if (random.nextFloat() < 0.10F * intensity) {
            level.addParticle(burstParticle, center.x, pos.getY() + 0.1D, center.z, 0.0D, 0.0D, 0.0D);
        }

        if (player.position().distanceTo(center) <= 1.55D) {
            int burstCount = Math.max(14, (int) Math.round(20 * intensity));

            for (int i = 0; i < burstCount; i++) {
                double height = random.nextDouble(0.05D, 2.0D);
                double radius = random.nextDouble(0.05D, 0.42D);
                double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
                double x = center.x + Math.cos(angle) * radius;
                double y = pos.getY() + height;
                double z = center.z + Math.sin(angle) * radius;
                level.addParticle(i % 3 == 0 ? burstParticle : flameParticle, x, y, z, 0.0D, random.nextDouble(0.04D, 0.16D), 0.0D);
            }
        }
    }

    private static void spawnSteamParticles(ClientLevel level, Player player, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);
        boolean contact = player.position().distanceTo(center) <= 1.55D;

        if (!contact) {
            // В покое — редкая спокойная паровая дымка.
            if (random.nextFloat() > 0.45F) {
                return;
            }

            level.addParticle(
                    ParticleTypes.CLOUD,
                    center.x + random.nextDouble(-0.35D, 0.35D),
                    pos.getY() + random.nextDouble(0.08D, 0.55D),
                    center.z + random.nextDouble(-0.35D, 0.35D),
                    random.nextDouble(-0.006D, 0.006D),
                    random.nextDouble(0.018D, 0.045D),
                    random.nextDouble(-0.006D, 0.006D)
            );
            return;
        }

        // При контакте — столб пара, как у Жарки, но без огня.
        for (int i = 0; i < 22; i++) {
            double height = random.nextDouble(0.04D, 2.0D);
            double radius = random.nextDouble(0.03D, 0.45D);
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double x = center.x + Math.cos(angle) * radius;
            double y = pos.getY() + height;
            double z = center.z + Math.sin(angle) * radius;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    random.nextDouble(-0.018D, 0.018D),
                    random.nextDouble(0.075D, 0.175D),
                    random.nextDouble(-0.018D, 0.018D)
            );
        }
    }

    private static void spawnGasParticles(ClientLevel level, Player player, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);
        long now = level.getGameTime();
        long key = pos.asLong();
        long cooldownUntil = GAS_PARTICLE_COOLDOWN_UNTIL.getOrDefault(key, 0L);

        if (now < cooldownUntil) {
            return;
        }

        double distance = player.position().distanceTo(center);

        if (distance <= GAS_TRIGGER_RADIUS) {
            GAS_PARTICLE_COOLDOWN_UNTIL.put(key, now + GAS_CLIENT_COOLDOWN_TICKS);
            spawnGasBurstParticles(level, pos);
            return;
        }

        double proximity = 1.0D - Math.min(distance / GAS_APPROACH_RADIUS, 1.0D);
        double chargedProximity = proximity * proximity;
        int count = 3 + (int) Math.round(chargedProximity * 62.0D);

        for (int i = 0; i < count; i++) {
            double radius = random.nextDouble(0.08D, 0.55D + proximity * 1.95D);
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double x = center.x + Math.cos(angle) * radius;
            double y = pos.getY() + random.nextDouble(0.05D, 0.65D + proximity * 1.65D);
            double z = center.z + Math.sin(angle) * radius;
            ParticleOptions particle = i % 5 == 0 ? ParticleTypes.FLAME : i % 3 == 0 ? ParticleTypes.CLOUD : ParticleTypes.SMOKE;
            level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    random.nextDouble(-0.025D, 0.025D),
                    random.nextDouble(0.015D, 0.080D),
                    random.nextDouble(-0.025D, 0.025D)
            );
        }
    }

    private static void spawnGasBurstParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        for (int i = 0; i < 86; i++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double radius = random.nextDouble(0.05D, 0.34D);
            double x = center.x + Math.cos(angle) * radius;
            double y = pos.getY() + random.nextDouble(0.08D, 1.25D);
            double z = center.z + Math.sin(angle) * radius;
            double speed = random.nextDouble(0.09D, 0.34D);
            double vy = random.nextDouble(0.035D, 0.18D);
            ParticleOptions particle = i % 4 == 0 ? ParticleTypes.FLAME : i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD;

            level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    Math.cos(angle) * speed,
                    vy,
                    Math.sin(angle) * speed
            );
        }
    }

    private static void spawnVortexParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos).add(0.0D, 1.0D, 0.0D);

        // Не плотный шар, а лёгкая зелёная дымка по сфере радиусом 2 блока.
        for (int i = 0; i < 5; i++) {
            Vec3 point = randomPointInSphere(center, VORTEX_RADIUS, random);
            Vec3 toCenter = center.subtract(point);
            Vec3 speed = toCenter.lengthSqr() <= 0.0001D ? Vec3.ZERO : toCenter.normalize().scale(0.012D);
            level.addParticle(
                    i % 3 == 0 ? ParticleTypes.COMPOSTER : ParticleTypes.HAPPY_VILLAGER,
                    point.x,
                    point.y,
                    point.z,
                    speed.x,
                    speed.y,
                    speed.z
            );
        }
    }

    private static void spawnCarouselParticles(ClientLevel level, BlockPos pos) {
        Vec3 center = centerOf(pos);
        long time = level.getGameTime();

        // Очень низкий серый вихрь: пыль по полу, без огромного облака.
        for (int i = 0; i < 3; i++) {
            double radius = 0.75D + (i % 3) * 0.42D;
            double angle = -(time * 0.095D + i * 2.1D);
            double x = center.x + Math.cos(angle) * radius;
            double y = pos.getY() + 0.035D + (i % 2) * 0.018D;
            double z = center.z + Math.sin(angle) * radius;
            double vx = -Math.sin(angle) * 0.010D;
            double vz = Math.cos(angle) * 0.010D;

            level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, 0.0D, vz);
        }
    }

    private static void spawnHydraulicParticles(ClientLevel level, Player player, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);
        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        double flatDistance = flatDistance(playerCenter, center);
        double yDelta = center.y - playerCenter.y;
        boolean dischargePreview = flatDistance <= 1.25D && yDelta >= 0.0D && yDelta <= 3.0D;

        if (!dischargePreview) {
            // В покое — буквально несколько серых частиц под потолком/ядром.
            if (random.nextFloat() > 0.16F) {
                return;
            }

            level.addParticle(
                    ParticleTypes.CLOUD,
                    center.x + random.nextDouble(-0.45D, 0.45D),
                    center.y - random.nextDouble(0.05D, 0.28D),
                    center.z + random.nextDouble(-0.45D, 0.45D),
                    random.nextDouble(-0.006D, 0.006D),
                    random.nextDouble(-0.010D, 0.004D),
                    random.nextDouble(-0.006D, 0.006D)
            );
            return;
        }

        // При срабатывании/приближении — плоскость сжатого воздуха, не вертикальный столб.
        for (int i = 0; i < 11; i++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double radius = random.nextDouble(0.15D, 1.35D);
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y - 0.85D + random.nextDouble(-0.035D, 0.035D);
            double z = center.z + Math.sin(angle) * radius;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    Math.cos(angle) * 0.035D,
                    -0.030D,
                    Math.sin(angle) * 0.035D
            );
        }
    }

    private static void spawnElectroParticles(ClientLevel level, Player player, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);
        long now = level.getGameTime();
        long key = pos.asLong();
        long cooldownUntil = ELECTRO_PARTICLE_COOLDOWN_UNTIL.getOrDefault(key, 0L);

        if (now < cooldownUntil) {
            return;
        }

        double flatDistance = flatDistance(player.position(), center);

        if (flatDistance <= ELECTRO_RADIUS && Math.abs(player.getY() - pos.getY()) <= 2.0D) {
            ELECTRO_PARTICLE_COOLDOWN_UNTIL.put(key, now + ELECTRO_CLIENT_COOLDOWN_TICKS);
            spawnElectroDischargeParticles(level, pos);
            return;
        }

        long phase = Math.floorMod(now + pos.asLong(), 60L);
        int count = 1 + (int) Math.round((phase / 60.0D) * 2.0D);

        if (random.nextFloat() > 0.55F) {
            return;
        }

        for (int i = 0; i < count; i++) {
            double radius = random.nextDouble(0.05D, ELECTRO_RADIUS);
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double x = center.x + Math.cos(angle) * radius;
            double y = pos.getY() + 0.08D + random.nextDouble(0.0D, 0.28D);
            double z = center.z + Math.sin(angle) * radius;

            level.addParticle(
                    i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                    x,
                    y,
                    z,
                    random.nextDouble(-0.014D, 0.014D),
                    random.nextDouble(0.00D, 0.018D),
                    random.nextDouble(-0.014D, 0.014D)
            );
        }
    }

    private static void spawnElectroDischargeParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double startRadius = random.nextDouble(0.02D, 0.22D);
            double x = center.x + Math.cos(angle) * startRadius;
            double y = pos.getY() + random.nextDouble(0.10D, 0.62D);
            double z = center.z + Math.sin(angle) * startRadius;
            double speed = random.nextDouble(0.055D, 0.155D);

            if (i % 3 == 0) {
                level.addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.18F, 0.78F, 1.0F),
                        x,
                        y,
                        z,
                        0.0D,
                        0.0D,
                        0.0D
                );
            } else {
                level.addParticle(
                        i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                        x,
                        y,
                        z,
                        Math.cos(angle) * speed,
                        random.nextDouble(0.0D, 0.045D),
                        Math.sin(angle) * speed
                );
            }
        }
    }

    private static void spawnElectrostaticParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        for (int i = 0; i < 9; i++) {
            Vec3 point = randomPointInSphere(center, ELECTROSTATIC_RADIUS, random);
            level.addParticle(
                    i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                    point.x,
                    point.y,
                    point.z,
                    random.nextDouble(-0.015D, 0.015D),
                    random.nextDouble(-0.005D, 0.025D),
                    random.nextDouble(-0.015D, 0.015D)
            );
        }
    }

    private static void spawnAcidParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        // Кисель теперь невидимым тех-блоком, поэтому саму "лужу" рисуем
        // частицами: редкое лаймовое бурление + кислотная испарина. Количество
        // примерно в 2 раза ниже старого варианта, чтобы не забивать кадр.
        if (random.nextFloat() > 0.70F) {
            // Визуально расползается примерно по зоне 3x3.
            double x = center.x + random.nextDouble(-1.42D, 1.42D);
            double z = center.z + random.nextDouble(-1.42D, 1.42D);
            level.addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.42F, 1.0F, 0.10F),
                    x,
                    pos.getY() + 0.065D + random.nextDouble(0.0D, 0.055D),
                    z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        for (int i = 0; i < 2; i++) {
            if (random.nextFloat() > 0.62F) {
                continue;
            }

            double x = center.x + random.nextDouble(-1.32D, 1.32D);
            double y = pos.getY() + 0.06D + random.nextDouble(0.0D, 0.18D);
            double z = center.z + random.nextDouble(-1.32D, 1.32D);
            ParticleOptions particle = i == 0 ? ParticleTypes.ITEM_SLIME : ParticleTypes.COMPOSTER;
            level.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    random.nextDouble(-0.004D, 0.004D),
                    random.nextDouble(0.006D, 0.028D),
                    random.nextDouble(-0.004D, 0.004D)
            );
        }

        if (random.nextFloat() < 0.10F) {
            double x = center.x + random.nextDouble(-1.28D, 1.28D);
            double z = center.z + random.nextDouble(-1.28D, 1.28D);
            level.addParticle(
                    ParticleTypes.SMOKE,
                    x,
                    pos.getY() + 0.14D,
                    z,
                    random.nextDouble(-0.002D, 0.002D),
                    random.nextDouble(0.010D, 0.026D),
                    random.nextDouble(-0.002D, 0.002D)
            );
        }
    }

    private static void spawnSpringboardParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long phase = Math.floorMod(level.getGameTime() + pos.asLong(), 55L);
        double charge = phase / 55.0D;

        if (random.nextFloat() > 0.10F + charge * 0.35F) {
            return;
        }

        Vec3 center = centerOf(pos);
        level.addParticle(
                ParticleTypes.POOF,
                center.x + random.nextDouble(-0.45D, 0.45D),
                pos.getY() + 0.08D,
                center.z + random.nextDouble(-0.45D, 0.45D),
                0.0D,
                0.04D + charge * 0.035D,
                0.0D
        );
    }

    private static void spawnZeroGravityParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos);

        if (random.nextFloat() > 0.42F) {
            return;
        }

        int count = random.nextInt(1, 3);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double radius = random.nextDouble(0.05D, 1.95D);
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + random.nextDouble(-0.65D, 1.35D);
            double z = center.z + Math.sin(angle) * radius;
            level.addParticle(
                    i % 2 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH,
                    x,
                    y,
                    z,
                    random.nextDouble(-0.004D, 0.004D),
                    random.nextDouble(0.006D, 0.024D),
                    random.nextDouble(-0.004D, 0.004D)
            );
        }
    }

    private static void spawnBubbleParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos).add(0.0D, 0.3D, 0.0D);

        // Грязная пыльная дымка: частиц больше, но без плотного облака на пол-экрана.
        for (int i = 0; i < 5; i++) {
            if (random.nextFloat() > 0.72F) {
                continue;
            }

            Vec3 point = randomPointInSphere(center, random.nextDouble(0.9D, 3.35D), random);
            ParticleOptions particle = switch (i % 4) {
                case 0 -> ParticleTypes.DUST_PLUME;
                case 1 -> ParticleTypes.ASH;
                case 2 -> ParticleTypes.SMOKE;
                default -> ParticleTypes.MYCELIUM;
            };

            level.addParticle(
                    particle,
                    point.x,
                    Math.max(pos.getY() + 0.04D, point.y),
                    point.z,
                    random.nextDouble(-0.010D, 0.010D),
                    random.nextDouble(0.000D, 0.018D),
                    random.nextDouble(-0.010D, 0.010D)
            );
        }
    }

    private static void spawnCobraParticles(ClientLevel level, BlockPos pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 center = centerOf(pos).add(0.0D, 0.85D, 0.0D);

        if (random.nextFloat() > 0.62F) {
            return;
        }

        int count = random.nextFloat() < 0.25F ? 2 : 1;
        for (int i = 0; i < count; i++) {
            double x = center.x + random.nextDouble(-0.13D, 0.13D);
            double y = center.y + random.nextDouble(0.00D, 0.32D);
            double z = center.z + random.nextDouble(-0.13D, 0.13D);

            if (i % 2 == 0) {
                level.addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.55F, 1.0F, 0.08F),
                        x,
                        y,
                        z,
                        0.0D,
                        -0.035D,
                        0.0D
                );
            } else {
                level.addParticle(
                        ParticleTypes.COMPOSTER,
                        x,
                        y,
                        z,
                        random.nextDouble(-0.003D, 0.003D),
                        -0.028D,
                        random.nextDouble(-0.003D, 0.003D)
                );
            }
        }
    }

    private static Vec3 randomPointInSphere(Vec3 center, double radius, ThreadLocalRandom random) {
        double u = random.nextDouble(-1.0D, 1.0D);
        double theta = random.nextDouble(0.0D, Math.PI * 2.0D);
        double r = radius * Math.cbrt(random.nextDouble());
        double flat = Math.sqrt(1.0D - u * u);

        return new Vec3(
                center.x + r * flat * Math.cos(theta),
                center.y + r * u,
                center.z + r * flat * Math.sin(theta)
        );
    }

    private static double flatDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }
}
