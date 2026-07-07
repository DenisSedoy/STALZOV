package com.czo.client.sound;

import com.czo.CZO;
import com.czo.contamination.ContaminationType;
import com.czo.contamination.CzoContaminations;
import com.czo.sound.CzoSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Stage 8C audio layer:
 * - anomaly idle is audible up to 15 blocks with distance attenuation;
 * - Inferno/Zharka use gravi idle + zhar_blow active and keep the active state for ~12 sec;
 * - Steam uses steam active only while the player stays inside;
 * - Gas uses gravi idle + gravi blast;
 * - Electra uses only electra_idle/electra_blast and triggers inside the damage radius;
 * - hydraulic sounds mirror springboard/trampoline.
 */
@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoZoneSoundClientEvents {
    private static final int ANOMALY_SCAN_RADIUS = 16;
    private static final int ANOMALY_Y_RADIUS = 4;
    private static final double ANOMALY_IDLE_RADIUS = 15.0D;
    private static final double ANOMALY_DEFAULT_ACTIVE_RADIUS = 2.35D;
    private static final double ANOMALY_TOUCH_RADIUS = 0.82D;

    private static final int THERMAL_ACTIVE_TICKS = 240; // 12 sec at 20 tps.
    private static final int STEAM_ACTIVE_TICKS = 14; // Stage 8D: steam loops/layers every ~0.7 sec while inside.

    private static final int CONTAMINATION_SOUND_SCAN_RADIUS = 5;
    private static final int CONTAMINATION_COLUMN_MIN_Y = -64;
    private static final int CONTAMINATION_COLUMN_MAX_Y = 500;
    private static final float CONTAMINATION_SOUND_VOLUME_MULTIPLIER = 0.70F;
    private static final float PSI_SOUND_VOLUME_MULTIPLIER = 1.00F;

    private static final SoundEvent[] GEIGER_SEQUENCE = new SoundEvent[]{
            CzoSounds.RADIATION_GEIGER_1,
            CzoSounds.RADIATION_GEIGER_2,
            CzoSounds.RADIATION_GEIGER_3
    };

    private static NearbyAnomaly lastNearestAnomaly;
    private static ContaminationSoundSources lastContaminationSources = ContaminationSoundSources.empty();

    private static int anomalyScanCooldownTicks;
    private static int contaminationScanCooldownTicks;
    private static int anomalyIdleCooldownTicks;
    private static int anomalyActiveCooldownTicks;
    private static int geigerCooldownTicks;
    private static int thermalDetectorCooldownTicks;
    private static int bioDetectorCooldownTicks;
    private static int psyEnterCooldownTicks;
    private static int psyLoopCooldownTicks;
    private static int geigerSequenceIndex;

    private static double psySignalStrength;
    private static boolean wasInPsy;

    private static final Map<BlockPos, Integer> runningThermalAnomalies = new HashMap<>();
    private static final Map<BlockPos, Integer> runningSteamAnomalies = new HashMap<>();
    private static final Map<BlockPos, Integer> anomalyLocalCooldowns = new HashMap<>();

    private CzoZoneSoundClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        if (!minecraft.player.isAlive() || minecraft.player.isSpectator()) {
            reset();
            return;
        }

        tickCooldowns();
        tickRunningAnomalies();
        tickContaminationSounds(minecraft);
        tickAnomalySounds(minecraft);
    }

    private static void tickCooldowns() {
        if (anomalyScanCooldownTicks > 0) anomalyScanCooldownTicks--;
        if (contaminationScanCooldownTicks > 0) contaminationScanCooldownTicks--;
        if (anomalyIdleCooldownTicks > 0) anomalyIdleCooldownTicks--;
        if (anomalyActiveCooldownTicks > 0) anomalyActiveCooldownTicks--;
        if (geigerCooldownTicks > 0) geigerCooldownTicks--;
        if (thermalDetectorCooldownTicks > 0) thermalDetectorCooldownTicks--;
        if (bioDetectorCooldownTicks > 0) bioDetectorCooldownTicks--;
        if (psyEnterCooldownTicks > 0) psyEnterCooldownTicks--;
        if (psyLoopCooldownTicks > 0) psyLoopCooldownTicks--;

        anomalyLocalCooldowns.replaceAll((pos, ticks) -> ticks - 1);
        anomalyLocalCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    private static void tickRunningAnomalies() {
        runningThermalAnomalies.replaceAll((pos, ticks) -> ticks - 1);
        runningThermalAnomalies.entrySet().removeIf(entry -> entry.getValue() <= 0);
        runningSteamAnomalies.replaceAll((pos, ticks) -> ticks - 1);
        runningSteamAnomalies.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    private static void tickContaminationSounds(Minecraft minecraft) {
        if (contaminationScanCooldownTicks <= 0) {
            lastContaminationSources = scanContaminationSources(minecraft);
            contaminationScanCooldownTicks = 5;
        }

        tickRadiationDetector(minecraft, lastContaminationSources.radiation());
        tickPsyAudio(minecraft, lastContaminationSources.psy());
        tickApproachDetector(minecraft, lastContaminationSources.thermal(), CzoSounds.DETECTOR_THERMAL, ContaminationType.THERMAL, true);
        tickApproachDetector(minecraft, lastContaminationSources.biological(), CzoSounds.DETECTOR_BIO, ContaminationType.BIOLOGICAL, false);
    }

    private static void tickRadiationDetector(Minecraft minecraft, ContaminationSource radiation) {
        if (radiation == null || radiation.level() <= 0 || radiation.distanceToEdge() > 5.0D) {
            return;
        }

        // Stage 8F.9: детектор молчит, если защита полностью закрывает уровень зоны.
        // Пример: 300 RAD против RAD-3 = тишина; 250 RAD против RAD-3 = сигнал.
        if (protectionCoversZone(minecraft, ContaminationType.RADIATION, radiation.level())) {
            return;
        }

        if (geigerCooldownTicks > 0) {
            return;
        }

        SoundEvent sound = GEIGER_SEQUENCE[geigerSequenceIndex % GEIGER_SEQUENCE.length];
        geigerSequenceIndex++;

        float distanceVolume = (float) clamp(1.0D - radiation.distanceToEdge() / 5.5D, 0.30D, 1.0D);
        float levelVolume = 0.46F + Math.min(5, radiation.level()) * 0.08F;
        playAtPlayer(minecraft, sound, Math.min(1.0F, distanceVolume * levelVolume), randomPitch(minecraft, 0.96F, 1.05F));
        geigerCooldownTicks = geigerProximityIntervalTicks(radiation.distanceToEdge());
    }

    private static int getProtectionOrZero(Minecraft minecraft, ContaminationType type) {
        if (minecraft.player == null) {
            return 0;
        }

        try {
            Class<?> protectionClass = Class.forName("com.czo.contamination.ContaminationProtection");
            for (Method method : protectionClass.getMethods()) {
                if (!"getProtection".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                if (!method.getParameterTypes()[1].isAssignableFrom(ContaminationType.class)) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(minecraft.player.getClass())) {
                    continue;
                }

                Object value = method.invoke(null, minecraft.player, type);
                if (value instanceof Number number) {
                    return Math.max(0, number.intValue());
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return 0;
    }

    private static boolean protectionCoversZone(Minecraft minecraft, ContaminationType type, int zoneLevel) {
        int protection = Math.max(0, getProtectionOrZero(minecraft, type));
        int requiredProtection = Math.max(1, Math.min(5, zoneLevel)) * 100;
        return protection >= requiredProtection;
    }

    private static void tickPsyAudio(Minecraft minecraft, ContaminationSource psy) {
        boolean inPsy = psy != null && psy.level() > 0 && psy.inCurrentColumn();
        if (inPsy) {
            // ПСИ должно врубаться резко сразу при входе в колонну, когда начинается
            // накопление, а не подниматься еле слышно как Терма/Био.
            psySignalStrength = clamp(psySignalStrength + 0.40D * Math.max(1, psy.level()), 1.0D, 100.0D);
        } else {
            psySignalStrength = Math.max(0.0D, psySignalStrength - 0.8D);
        }

        if (inPsy && !wasInPsy && psyEnterCooldownTicks <= 0) {
            playAtPlayerPsi(minecraft, CzoSounds.PSY_ENTER, 0.85F, randomPitch(minecraft, 0.97F, 1.03F));
            // Сразу даём первый loop-удар, чтобы игрок услышал зону моментально.
            playAtPlayerPsi(minecraft, CzoSounds.PSY_LOOP, 0.55F, randomPitch(minecraft, 0.96F, 1.04F));
            psyEnterCooldownTicks = 120;
            psyLoopCooldownTicks = 35;
        }

        if (inPsy && psySignalStrength > 1.0D && psyLoopCooldownTicks <= 0) {
            float volume = (float) clamp(psySignalStrength / 100.0D, 0.18D, 0.90D);
            playAtPlayerPsi(minecraft, CzoSounds.PSY_LOOP, volume, randomPitch(minecraft, 0.96F, 1.04F));
            psyLoopCooldownTicks = 60;
        }

        wasInPsy = inPsy;
    }

    private static void tickApproachDetector(Minecraft minecraft, ContaminationSource source, SoundEvent sound, ContaminationType type, boolean thermal) {
        if (source == null || source.level() <= 0 || source.distanceToEdge() > 5.0D) {
            return;
        }

        // Терма/Био теперь работают по той же идее, что и Радейка:
        // если защита закрывает уровень зоны — детектор молчит;
        // если не закрывает — частота зависит от расстояния до источника.
        if (protectionCoversZone(minecraft, type, source.level())) {
            return;
        }

        if (thermal) {
            if (thermalDetectorCooldownTicks > 0) return;
            thermalDetectorCooldownTicks = thermalBioProximityIntervalTicks(source.distanceToEdge());
        } else {
            if (bioDetectorCooldownTicks > 0) return;
            bioDetectorCooldownTicks = thermalBioProximityIntervalTicks(source.distanceToEdge());
        }

        float volume = (float) clamp(1.0D - source.distanceToEdge() / 5.5D, 0.28D, 0.90D);
        playAtPlayer(minecraft, sound, volume, randomPitch(minecraft, 0.97F, 1.04F));
    }

    private static int geigerProximityIntervalTicks(double distance) {
        // Радейку пока оставляем резкой: 5 блоков — 1 раз/сек; 4 — 0.7 сек;
        // 3 — 0.5 сек; 2 — 0.2 сек; 1/внутри источника — 0.05 сек.
        if (distance <= 1.0D) return 1;
        if (distance <= 2.0D) return 4;
        if (distance <= 3.0D) return 10;
        if (distance <= 4.0D) return 14;
        return 20;
    }

    private static int thermalBioProximityIntervalTicks(double distance) {
        // Терма/Био мягче: 5 блоков — 1.5 сек; 4 — 1.2 сек;
        // 3 — 0.9 сек; 2 — 0.7 сек; 1/внутри источника — 0.5 сек.
        if (distance <= 1.0D) return 10;
        if (distance <= 2.0D) return 14;
        if (distance <= 3.0D) return 18;
        if (distance <= 4.0D) return 24;
        return 30;
    }

    private static void tickAnomalySounds(Minecraft minecraft) {
        if (anomalyScanCooldownTicks <= 0) {
            lastNearestAnomaly = findNearestAnomaly(minecraft);
            anomalyScanCooldownTicks = 4;
        }

        NearbyAnomaly anomaly = lastNearestAnomaly;
        if (anomaly == null || anomaly.distance() > ANOMALY_IDLE_RADIUS) {
            return;
        }

        if (shouldPlayActive(minecraft, anomaly)) {
            playActiveAnomaly(minecraft, anomaly);
            return;
        }

        if (anomalyIdleCooldownTicks <= 0) {
            double distance = anomaly.distance();
            float volume = attenuatedAnomalyVolume(distance, 0.68F);
            if (volume > 0.025F) {
                playAtBlock(minecraft, anomaly.pos(), anomaly.soundType().idle(), volume, randomPitch(minecraft, 0.95F, 1.05F));
            }
            anomalyIdleCooldownTicks = 46 + minecraft.level.getRandom().nextInt(35);
        }
    }

    private static boolean shouldPlayActive(Minecraft minecraft, NearbyAnomaly anomaly) {
        if (anomalyLocalCooldowns.containsKey(anomaly.pos())) {
            return false;
        }

        AnomalySoundType type = anomaly.soundType();
        boolean touching = anomaly.distance() <= ANOMALY_TOUCH_RADIUS;

        if (type == AnomalySoundType.STEAM) {
            return touching;
        }

        if (type.thermalLike()) {
            boolean isRunning = runningThermalAnomalies.getOrDefault(anomaly.pos(), 0) > 0;
            return touching && !isRunning;
        }

        if (type.touchOnly()) {
            return touching;
        }

        if (type == AnomalySoundType.VORONKA) {
            return anomaly.distance() <= 2.35D;
        }

        if (type == AnomalySoundType.CAROUSEL) {
            return anomaly.distance() <= 3.25D;
        }

        return anomaly.distance() <= ANOMALY_DEFAULT_ACTIVE_RADIUS && anomalyActiveCooldownTicks <= 0;
    }

    private static void playActiveAnomaly(Minecraft minecraft, NearbyAnomaly anomaly) {
        float volume = attenuatedAnomalyVolume(anomaly.distance(), 0.92F);
        playAtBlock(minecraft, anomaly.pos(), anomaly.soundType().active(), volume, randomPitch(minecraft, 0.92F, 1.08F));
        anomalyIdleCooldownTicks = Math.max(anomalyIdleCooldownTicks, 35);

        if (anomaly.soundType().thermalLike()) {
            runningThermalAnomalies.put(anomaly.pos(), THERMAL_ACTIVE_TICKS);
            anomalyLocalCooldowns.put(anomaly.pos(), THERMAL_ACTIVE_TICKS);
        } else if (anomaly.soundType() == AnomalySoundType.STEAM) {
            // Steam intentionally overlaps a little and repeats while the player remains inside.
            runningSteamAnomalies.put(anomaly.pos(), STEAM_ACTIVE_TICKS);
            anomalyLocalCooldowns.put(anomaly.pos(), STEAM_ACTIVE_TICKS);
        } else if (anomaly.soundType() == AnomalySoundType.VORONKA) {
            anomalyActiveCooldownTicks = 34;
            anomalyLocalCooldowns.put(anomaly.pos(), 200);
        } else if (anomaly.soundType() == AnomalySoundType.CAROUSEL) {
            anomalyActiveCooldownTicks = 28;
            anomalyLocalCooldowns.put(anomaly.pos(), 28);
        } else if (anomaly.soundType() == AnomalySoundType.ACID) {
            // Кисель шипит чаще: его серверная перезарядка теперь короче 0.5 секунды.
            anomalyActiveCooldownTicks = 8;
            anomalyLocalCooldowns.put(anomaly.pos(), 8);
        } else {
            anomalyActiveCooldownTicks = 34;
            anomalyLocalCooldowns.put(anomaly.pos(), 34);
        }
    }

    private static float attenuatedAnomalyVolume(double distance, float maxVolume) {
        double t = clamp(1.0D - distance / ANOMALY_IDLE_RADIUS, 0.0D, 1.0D);
        double shaped = t * t * (3.0D - 2.0D * t);
        return (float) clamp(0.035D + shaped * maxVolume, 0.0D, maxVolume);
    }

    private static NearbyAnomaly findNearestAnomaly(Minecraft minecraft) {
        BlockPos base = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        NearbyAnomaly nearest = null;

        for (int dx = -ANOMALY_SCAN_RADIUS; dx <= ANOMALY_SCAN_RADIUS; dx++) {
            for (int dz = -ANOMALY_SCAN_RADIUS; dz <= ANOMALY_SCAN_RADIUS; dz++) {
                for (int dy = -ANOMALY_Y_RADIUS; dy <= ANOMALY_Y_RADIUS; dy++) {
                    scan.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    String path = czoBlockPath(minecraft, scan);
                    if (!path.contains("anomaly")) {
                        continue;
                    }

                    AnomalySoundType soundType = soundTypeForPath(path);
                    if (soundType == null) {
                        continue;
                    }

                    double distance = distanceToBlockCenter(minecraft, scan);
                    if (nearest == null || distance < nearest.distance()) {
                        nearest = new NearbyAnomaly(scan.immutable(), soundType, distance);
                    }
                }
            }
        }

        return nearest;
    }

    private static ContaminationSoundSources scanContaminationSources(Minecraft minecraft) {
        BlockPos base = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        ContaminationSource radiation = null;
        ContaminationSource psy = null;
        ContaminationSource thermal = null;
        ContaminationSource biological = null;

        for (int x = base.getX() - CONTAMINATION_SOUND_SCAN_RADIUS; x <= base.getX() + CONTAMINATION_SOUND_SCAN_RADIUS; x++) {
            for (int z = base.getZ() - CONTAMINATION_SOUND_SCAN_RADIUS; z <= base.getZ() + CONTAMINATION_SOUND_SCAN_RADIUS; z++) {
                double edgeDistance = distanceToColumnEdge(minecraft, x, z);
                boolean inCurrentColumn = x == base.getX() && z == base.getZ();

                for (int y = CONTAMINATION_COLUMN_MIN_Y; y <= CONTAMINATION_COLUMN_MAX_Y; y++) {
                    scan.set(x, y, z);
                    BlockState state = minecraft.level.getBlockState(scan);
                    ContaminationType type = CzoContaminations.getType(state.getBlock());
                    if (type == null) {
                        continue;
                    }

                    int level = Math.max(1, CzoContaminations.getLevel(state.getBlock()));
                    ContaminationSource source = new ContaminationSource(scan.immutable(), level, edgeDistance, inCurrentColumn);
                    switch (type) {
                        case RADIATION -> radiation = chooseNearerOrStronger(radiation, source);
                        case PSY -> psy = chooseStrongerOrNearer(psy, source);
                        case THERMAL -> thermal = chooseNearerOrStronger(thermal, source);
                        case BIOLOGICAL -> biological = chooseNearerOrStronger(biological, source);
                    }
                }
            }
        }

        return new ContaminationSoundSources(radiation, psy, thermal, biological);
    }

    private static ContaminationSource chooseStrongerOrNearer(ContaminationSource current, ContaminationSource candidate) {
        if (current == null) return candidate;
        if (candidate.level() > current.level()) return candidate;
        if (candidate.level() == current.level() && candidate.distanceToEdge() < current.distanceToEdge()) return candidate;
        return current;
    }


    private static ContaminationSource chooseNearerOrStronger(ContaminationSource current, ContaminationSource candidate) {
        if (current == null) return candidate;
        if (candidate.distanceToEdge() < current.distanceToEdge()) return candidate;
        if (candidate.distanceToEdge() == current.distanceToEdge() && candidate.level() > current.level()) return candidate;
        return current;
    }

    private static String czoBlockPath(Minecraft minecraft, BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null || !CZO.MODID.equals(id.getNamespace())) {
            return "";
        }
        return id.getPath().toLowerCase(Locale.ROOT);
    }

    private static AnomalySoundType soundTypeForPath(String path) {
        if (path.contains("obr_116") || path.contains("obr116") || path.contains("toxic_moss")) {
            // Образец 116 — визуальный/контактный объект без постоянного idle-звука.
            return null;
        }
        if (path.contains("electro") || path.contains("electra")) {
            return AnomalySoundType.ELECTRO;
        }
        if (path.contains("gas")) {
            return AnomalySoundType.GAS;
        }
        if (path.contains("acid") || path.contains("kisel") || path.contains("cobra") || path.contains("moss") || path.contains("obr") || path.contains("bio")) {
            return AnomalySoundType.ACID;
        }
        if (path.contains("hydraulic") || path.contains("gidrav") || path.contains("гидрав")) {
            return AnomalySoundType.HYDRAULIC_TOUCH;
        }
        if (path.contains("steam") || path.contains("par")) {
            return AnomalySoundType.STEAM;
        }
        if (path.contains("burner") || path.contains("inferno") || path.contains("zhar") || path.contains("жарк") || path.contains("fireball")) {
            return AnomalySoundType.THERMAL;
        }
        if (path.contains("vortex") || path.contains("voronka") || path.contains("funnel") || path.contains("ворон")) {
            return AnomalySoundType.VORONKA;
        }
        if (path.contains("carousel") || path.contains("karusel") || path.contains("vikh") || path.contains("whirl") || path.contains("вихр") || path.contains("карус")) {
            return AnomalySoundType.CAROUSEL;
        }
        if (path.contains("springboard") || path.contains("tramplin") || path.contains("трамп")) {
            return AnomalySoundType.GRAVI_TOUCH;
        }
        return AnomalySoundType.GRAVI;
    }

    private static double distanceToBlockCenter(Minecraft minecraft, BlockPos pos) {
        double dx = minecraft.player.getX() - (pos.getX() + 0.5D);
        double dy = minecraft.player.getY() + 0.5D - (pos.getY() + 0.5D);
        double dz = minecraft.player.getZ() - (pos.getZ() + 0.5D);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double distanceToColumnEdge(Minecraft minecraft, int blockX, int blockZ) {
        double dx = Math.max(0.0D, Math.abs(minecraft.player.getX() - (blockX + 0.5D)) - 0.5D);
        double dz = Math.max(0.0D, Math.abs(minecraft.player.getZ() - (blockZ + 0.5D)) - 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void playAtBlock(Minecraft minecraft, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (minecraft.level == null) return;
        minecraft.level.playLocalSound(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                sound,
                SoundSource.AMBIENT,
                volume * CONTAMINATION_SOUND_VOLUME_MULTIPLIER,
                pitch,
                false
        );
    }

    private static void playAtPlayer(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        if (minecraft.level == null || minecraft.player == null) return;
        minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                sound,
                SoundSource.AMBIENT,
                volume * CONTAMINATION_SOUND_VOLUME_MULTIPLIER,
                pitch,
                false
        );
    }

    private static void playAtPlayerPsi(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        if (minecraft.level == null || minecraft.player == null) return;
        minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                sound,
                SoundSource.AMBIENT,
                volume * PSI_SOUND_VOLUME_MULTIPLIER,
                pitch,
                false
        );
    }

    private static float randomPitch(Minecraft minecraft, float min, float max) {
        return min + minecraft.level.getRandom().nextFloat() * (max - min);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void reset() {
        lastNearestAnomaly = null;
        lastContaminationSources = ContaminationSoundSources.empty();
        anomalyScanCooldownTicks = 0;
        contaminationScanCooldownTicks = 0;
        anomalyIdleCooldownTicks = 0;
        anomalyActiveCooldownTicks = 0;
        geigerCooldownTicks = 0;
        thermalDetectorCooldownTicks = 0;
        bioDetectorCooldownTicks = 0;
        psyEnterCooldownTicks = 0;
        psyLoopCooldownTicks = 0;
        geigerSequenceIndex = 0;
        psySignalStrength = 0.0D;
        wasInPsy = false;
        runningThermalAnomalies.clear();
        runningSteamAnomalies.clear();
        anomalyLocalCooldowns.clear();
    }

    private enum AnomalySoundType {
        GRAVI(CzoSounds.ANOMALY_GRAVI_IDLE, CzoSounds.ANOMALY_GRAVI_ACTIVE, false, false),
        GRAVI_TOUCH(CzoSounds.ANOMALY_GRAVI_IDLE, CzoSounds.ANOMALY_GRAVI_ACTIVE, true, false),
        HYDRAULIC_TOUCH(CzoSounds.ANOMALY_GRAVI_IDLE, CzoSounds.ANOMALY_GRAVI_ACTIVE, true, false),
        VORONKA(CzoSounds.ANOMALY_GRAVI_IDLE, CzoSounds.ANOMALY_GRAVI_RUMBLE, false, false),
        CAROUSEL(CzoSounds.ANOMALY_GRAVI_IDLE, CzoSounds.ANOMALY_GRAVI_MINCER_ACTIVE, false, false),
        THERMAL(CzoSounds.ANOMALY_THERMAL_IDLE, CzoSounds.ANOMALY_THERMAL_ACTIVE, true, true),
        STEAM(CzoSounds.ANOMALY_STEAM_IDLE, CzoSounds.ANOMALY_STEAM_ACTIVE, true, false),
        GAS(CzoSounds.ANOMALY_GAS_IDLE, CzoSounds.ANOMALY_GAS_ACTIVE, false, false),
        ELECTRO(CzoSounds.ANOMALY_ELECTRO_IDLE, CzoSounds.ANOMALY_ELECTRO_ACTIVE, false, false),
        ACID(CzoSounds.ANOMALY_ACID_IDLE, CzoSounds.ANOMALY_ACID_ACTIVE, false, false);

        private final SoundEvent idle;
        private final SoundEvent active;
        private final boolean touchOnly;
        private final boolean thermalLike;

        AnomalySoundType(SoundEvent idle, SoundEvent active, boolean touchOnly, boolean thermalLike) {
            this.idle = idle;
            this.active = active;
            this.touchOnly = touchOnly;
            this.thermalLike = thermalLike;
        }

        SoundEvent idle() {
            return idle;
        }

        SoundEvent active() {
            return active;
        }

        boolean touchOnly() {
            return touchOnly;
        }

        boolean thermalLike() {
            return thermalLike;
        }
    }

    private record NearbyAnomaly(BlockPos pos, AnomalySoundType soundType, double distance) {
    }

    private record ContaminationSource(BlockPos pos, int level, double distanceToEdge, boolean inCurrentColumn) {
    }

    private record ContaminationSoundSources(
            ContaminationSource radiation,
            ContaminationSource psy,
            ContaminationSource thermal,
            ContaminationSource biological
    ) {
        static ContaminationSoundSources empty() {
            return new ContaminationSoundSources(null, null, null, null);
        }
    }
}
