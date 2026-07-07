package com.czo.client;

import com.czo.CZO;
import com.czo.contamination.ContaminationType;
import com.czo.contamination.CzoContaminations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoContaminationClientEvents {
    // Зона заражения действует только в своём X/Z-блоке, но по всей высоте колонны.
    private static final int COLUMN_SCAN_RADIUS = 0;
    private static final int COLUMN_MIN_Y = -64;
    private static final int COLUMN_MAX_Y = 500;
    private static final int MARKER_SCAN_RADIUS = 8;
    private static final int MARKER_SCAN_INTERVAL_TICKS = 8;

    private static final double DECAY_PER_TICK = 10.0D / 20.0D;
    private static final double VISUAL_FULL_THRESHOLD = 100.0D;
    private static final double VISUAL_MAX_VALUE = 1_000_000.0D;
    private static final int MILK_CLEAR_MIN_TICKS = 24;

    // Те же ступени опасности, что использует серверная система симптомов.
    // HUD показывает иконку уже при ненулевом локальном заражении, а цвет повышает по этим порогам.
    private static final double HUD_MEDIUM_THRESHOLD = 10_000.0D;
    private static final double HUD_HEAVY_THRESHOLD = 100_000.0D;
    private static final double HUD_LETHAL_THRESHOLD = 1_000_000.0D;

    // Это клиентская оценка заражения для визуала. Сервер всё равно остаётся источником правды
    // для урона, эффектов, смерти и настоящих чисел заражения.
    private static final Map<ContaminationType, Double> ESTIMATED_VALUES = new EnumMap<>(ContaminationType.class);
    private static final Map<ContaminationType, Double> OVERLAY_STRENGTHS = new EnumMap<>(ContaminationType.class);

    private static int localMilkUseTicks;

    private CzoContaminationClientEvents() {
    }

    /**
     * Возвращает уровень опасности для нового HUD без вывода чисел в у.е.
     * 0 — иконку не показывать, 1 — лёгкий, 2 — средний, 3 — тяжёлый, 4 — смертельный.
     */
    public static int getHudSeverityLevel(ContaminationType type) {
        double value = ESTIMATED_VALUES.getOrDefault(type, 0.0D);

        if (value <= 0.0D) {
            return 0;
        }

        if (value >= HUD_LETHAL_THRESHOLD) {
            return 4;
        }

        if (value >= HUD_HEAVY_THRESHOLD) {
            return 3;
        }

        if (value >= HUD_MEDIUM_THRESHOLD) {
            return 2;
        }

        return 1;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            clearVisuals();
            return;
        }

        if (!minecraft.player.isAlive() || minecraft.player.isSpectator() || minecraft.player.isCreative()) {
            clearVisuals();
            return;
        }

        if (handleLocalMilkClear(minecraft)) {
            return;
        }

        Map<ContaminationType, Integer> exposure = scanExposure(minecraft);
        spawnZoneMarkersIfHoldingZoneItem(minecraft);

        for (ContaminationType type : ContaminationType.values()) {
            double value = ESTIMATED_VALUES.getOrDefault(type, 0.0D);
            int zoneLevel = exposure.getOrDefault(type, 0);

            if (zoneLevel > 0) {
                value = Math.min(VISUAL_MAX_VALUE, value + gainPerTick(zoneLevel));
            } else {
                value = Math.max(0.0D, value - DECAY_PER_TICK);
            }

            if (value <= 0.0D) {
                ESTIMATED_VALUES.remove(type);
            } else {
                ESTIMATED_VALUES.put(type, value);
            }

            double targetStrength = targetOverlayStrength(value);
            double currentStrength = OVERLAY_STRENGTHS.getOrDefault(type, 0.0D);

            // Плавное появление и плавное исчезновение. Без резкого первого кадра.
            double smoothing = targetStrength > currentStrength ? 0.045D : 0.035D;
            currentStrength += (targetStrength - currentStrength) * smoothing;

            if (value <= 0.0D && currentStrength <= 0.01D) {
                OVERLAY_STRENGTHS.remove(type);
            } else {
                OVERLAY_STRENGTHS.put(type, clamp(currentStrength, 0.0D, 1.0D));
            }
        }
    }

    private static boolean handleLocalMilkClear(Minecraft minecraft) {
        if (minecraft.player.isUsingItem() && minecraft.player.getUseItem().is(Items.MILK_BUCKET)) {
            localMilkUseTicks++;
            return false;
        }

        if (localMilkUseTicks >= MILK_CLEAR_MIN_TICKS) {
            clearVisuals();
            return true;
        }

        localMilkUseTicks = 0;
        return false;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        ContaminationType strongestType = null;
        double strongestStrength = 0.0D;

        for (Map.Entry<ContaminationType, Double> entry : OVERLAY_STRENGTHS.entrySet()) {
            if (entry.getValue() > strongestStrength) {
                strongestStrength = entry.getValue();
                strongestType = entry.getKey();
            }
        }

        if (strongestType == null || strongestStrength <= 0.01D) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        drawContaminationOverlay(graphics, strongestType, strongestStrength, width, height, minecraft.level.getGameTime());
    }

    private static Map<ContaminationType, Integer> scanExposure(Minecraft minecraft) {
        Map<ContaminationType, Integer> exposure = new EnumMap<>(ContaminationType.class);

        BlockPos basePos = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();

        // Клиентский warning-эффект повторяет серверную колонную логику:
        // зона действует строго в своём X/Z-блоке и по всей высоте от -64 до 500.
        for (int x = basePos.getX() - COLUMN_SCAN_RADIUS; x <= basePos.getX() + COLUMN_SCAN_RADIUS; x++) {
            for (int z = basePos.getZ() - COLUMN_SCAN_RADIUS; z <= basePos.getZ() + COLUMN_SCAN_RADIUS; z++) {
                for (int y = COLUMN_MIN_Y; y <= COLUMN_MAX_Y; y++) {
                    scanPos.set(x, y, z);
                    ContaminationType type = CzoContaminations.getType(minecraft.level.getBlockState(scanPos).getBlock());

                    if (type == null) {
                        continue;
                    }

                    int levelValue = CzoContaminations.getLevel(minecraft.level.getBlockState(scanPos).getBlock());
                    int current = exposure.getOrDefault(type, 0);

                    if (levelValue > current) {
                        exposure.put(type, levelValue);
                    }
                }
            }
        }

        return exposure;
    }

    private static double gainPerTick(int zoneLevel) {
        return Math.pow(10.0D, Math.max(1, zoneLevel)) / 20.0D;
    }

    private static double targetOverlayStrength(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }

        // Пока заражение выше 100 у.е., визуал держится на максимальной силе.
        // Когда значение упало ниже 100, он начинает плавно исчезать.
        if (value > VISUAL_FULL_THRESHOLD) {
            return 1.0D;
        }

        double t = clamp(value / VISUAL_FULL_THRESHOLD, 0.0D, 1.0D);
        return smoothStep(t);
    }

    private static double smoothStep(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static void spawnZoneMarkersIfHoldingZoneItem(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!isHoldingContaminationZoneItem(minecraft)) {
            return;
        }

        if (minecraft.level.getGameTime() % MARKER_SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        BlockPos basePos = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();

        for (int x = basePos.getX() - MARKER_SCAN_RADIUS; x <= basePos.getX() + MARKER_SCAN_RADIUS; x++) {
            for (int z = basePos.getZ() - MARKER_SCAN_RADIUS; z <= basePos.getZ() + MARKER_SCAN_RADIUS; z++) {
                for (int y = COLUMN_MIN_Y; y <= COLUMN_MAX_Y; y++) {
                    scanPos.set(x, y, z);
                    ContaminationType type = CzoContaminations.getType(minecraft.level.getBlockState(scanPos).getBlock());

                    if (type == null) {
                        continue;
                    }

                    spawnZoneMarkerParticle(minecraft, scanPos, type);
                }
            }
        }
    }

    private static boolean isHoldingContaminationZoneItem(Minecraft minecraft) {
        return isContaminationZoneItem(minecraft.player.getMainHandItem())
                || isContaminationZoneItem(minecraft.player.getOffhandItem());
    }

    private static boolean isContaminationZoneItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && CzoContaminations.getType(blockItem.getBlock()) != null;
    }

    private static void spawnZoneMarkerParticle(Minecraft minecraft, BlockPos pos, ContaminationType type) {
        double x = pos.getX() + 0.5D + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.62D;
        double y = pos.getY() + 0.5D + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.62D;
        double z = pos.getZ() + 0.5D + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.62D;

        float r;
        float g;
        float b;

        switch (type) {
            case RADIATION -> {
                // Радиация — жёлтый.
                r = 1.0F;
                g = 0.92F;
                b = 0.08F;
            }
            case BIOLOGICAL -> {
                // Био — лаймовый.
                r = 0.28F;
                g = 1.0F;
                b = 0.05F;
            }
            case PSY -> {
                // Пси — синий.
                r = 0.10F;
                g = 0.36F;
                b = 1.0F;
            }
            case THERMAL -> {
                // Терма — красный.
                r = 1.0F;
                g = 0.05F;
                b = 0.0F;
            }
            default -> {
                r = 1.0F;
                g = 1.0F;
                b = 1.0F;
            }
        }

        // Белая искра фейерверка + цветная дымка, чтобы тех-блоки читались по типу заражения.
        minecraft.level.addParticle(
                ParticleTypes.FIREWORK,
                x,
                y,
                z,
                (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.03D,
                minecraft.level.getRandom().nextDouble() * 0.035D,
                (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.03D
        );

        minecraft.level.addParticle(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, r, g, b),
                x,
                y,
                z,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static void drawContaminationOverlay(
            GuiGraphicsExtractor graphics,
            ContaminationType type,
            double intensity,
            int width,
            int height,
            long gameTime
    ) {
        int filterColor;
        int edgeColor;

        switch (type) {
            case THERMAL -> {
                filterColor = argb((int) (58 * intensity), 255, 102, 16);
                edgeColor = argb((int) (152 * intensity), 135, 18, 0);
            }
            case BIOLOGICAL -> {
                filterColor = argb((int) (54 * intensity), 110, 255, 40);
                edgeColor = argb((int) (142 * intensity), 28, 82, 20);
            }
            case PSY -> {
                filterColor = argb((int) (62 * intensity), 205, 190, 72);
                edgeColor = argb((int) (150 * intensity), 68, 32, 150);
            }
            case RADIATION -> {
                // Stage 8C: radiation is now a STALKER-like desaturation/noise effect,
                // not a full black fade. The world remains visible, but loses color and
                // gets increasingly dirty/noisy as local radiation grows from 1..100 u.e.
                filterColor = argb((int) (32 + 58 * intensity), 138, 138, 138);
                edgeColor = argb((int) (82 + 92 * intensity), 0, 0, 0);
            }
            default -> {
                filterColor = argb((int) (44 * intensity), 255, 255, 255);
                edgeColor = argb((int) (120 * intensity), 0, 0, 0);
            }
        }

        graphics.fill(0, 0, width, height, filterColor);
        drawVignette(graphics, width, height, edgeColor, intensity);

        if (type == ContaminationType.RADIATION) {
            drawRadiationNoise(graphics, width, height, intensity, gameTime);
        }
    }

    private static void drawVignette(GuiGraphicsExtractor graphics, int width, int height, int color, double intensity) {
        int maxInset = Math.max(12, (int) (Math.min(width, height) * (0.12D + 0.08D * intensity)));
        int layers = 7;

        for (int i = 0; i < layers; i++) {
            double t = 1.0D - i / (double) layers;
            int alpha = (int) (alphaOf(color) * t * t);
            int c = withAlpha(color, alpha);
            int thickness = Math.max(2, maxInset / layers);
            int inset = i * thickness;

            graphics.fill(inset, inset, width - inset, inset + thickness, c);
            graphics.fill(inset, height - inset - thickness, width - inset, height - inset, c);
            graphics.fill(inset, inset, inset + thickness, height - inset, c);
            graphics.fill(width - inset - thickness, inset, width - inset, height - inset, c);
        }
    }

    private static void drawRadiationNoise(GuiGraphicsExtractor graphics, int width, int height, double intensity, long gameTime) {
        // Dense lightweight film grain. No full-screen texture is needed; this keeps it cheap
        // while visually moving toward the user's reference: grayscale, noisy, harsh, but readable.
        int points = (int) (90 + 620 * intensity);
        long seed = gameTime * 1103515245L + 12345L;

        for (int i = 0; i < points; i++) {
            seed = seed * 1664525L + 1013904223L;
            int x = (int) Math.floorMod(seed >> 16, Math.max(1, width));
            seed = seed * 1664525L + 1013904223L;
            int y = (int) Math.floorMod(seed >> 16, Math.max(1, height));

            seed = seed * 1664525L + 1013904223L;
            int gray = 42 + (int) Math.floorMod(seed, 184);
            int alpha = (int) (18 + 46 * intensity);
            int size = intensity > 0.72D && (i % 7 == 0) ? 2 : 1;

            graphics.fill(x, y, Math.min(width, x + size), Math.min(height, y + size), argb(alpha, gray, gray, gray));
        }

        // Very light scan/film bands. This gives the screen a failing-detector feel without
        // blacking out the whole image.
        int bands = (int) (4 + 12 * intensity);
        for (int i = 0; i < bands; i++) {
            seed = seed * 1664525L + 1013904223L;
            int y = (int) Math.floorMod(seed >> 16, Math.max(1, height));
            int alpha = (int) (5 + 18 * intensity);
            graphics.fill(0, y, width, Math.min(height, y + 1), argb(alpha, 210, 210, 210));
        }
    }

    private static double flatDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void clearVisuals() {
        ESTIMATED_VALUES.clear();
        OVERLAY_STRENGTHS.clear();
        localMilkUseTicks = 0;
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (clampInt(alpha, 0, 255) << 24)
                | (clampInt(red, 0, 255) << 16)
                | (clampInt(green, 0, 255) << 8)
                | clampInt(blue, 0, 255);
    }

    private static int withAlpha(int color, int alpha) {
        return (clampInt(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaOf(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
