package com.czo.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Stage 7N: explored-cache minimap.
 *
 * <p>The minimap no longer rebuilds the full visible image as a slideshow. Terrain is sampled into
 * a session cache once when the player sees it. Each rendered frame only projects already-known
 * cached blocks to the minimap. Unknown visible cells are queued and sampled in small budgets, while
 * already-known visible cells are rechecked slowly so world changes are picked up without constant
 * full rebuilds. Dynamic markers, like other players, are still drawn every frame.</p>
 */
public final class CzoMinimapHudRenderer {
    public static final int MAP_X = 3;
    public static final int MAP_Y = 3;

    public static final int INNER_PAD = 3;
    public static final int MAP_CELLS = 64;
    public static final int CELL_SIZE = 1;
    public static final int INNER_SIZE = MAP_CELLS * CELL_SIZE;
    public static final int MAP_SIZE = INNER_SIZE + INNER_PAD * 2;
    private static final int WORLD_BLOCKS_PER_CELL = 1;
    private static final float MINIMAP_ROTATION_DEGREES = 180.0F;

    private static final int SAMPLE_BUDGET_PER_FRAME = 192;
    private static final int RESCAN_BUDGET_PER_FRAME = 18;
    private static final int MAX_EXPLORED_BLOCKS = 180_000;

    private static final int FRAME_DARK = 0xDD0B1010;
    private static final int FRAME_MID = 0xFF2E3838;
    private static final int FRAME_LIGHT = 0xFF596565;
    private static final int MAP_BG = 0xCC202727;
    private static final int MAP_GRID = 0x102F3A3A;
    private static final int MAP_UNKNOWN = 0xFF101616;
    private static final int TEXT = 0xFFE9EEEE;
    private static final int TEXT_MUTED = 0xFF9AA6A6;
    private static final int CLOCK_TEXT = 0xFFFFC75A;
    private static final int NORTH_RED = 0xFFFF4040;
    private static final int PLAYER_MARKER = 0xFFFFFFFF;
    private static final int PLAYER_MARKER_DARK = 0xCC000000;
    private static final int OTHER_PLAYER_MARKER = 0xFFFFD55A;
    private static final int OTHER_PLAYER_OUTLINE = 0xCC000000;

    private static final int[] DISPLAY_CACHE = new int[MAP_CELLS * MAP_CELLS];
    private static final long[] DISPLAY_KEYS = new long[MAP_CELLS * MAP_CELLS];

    private static final Map<Long, Integer> EXPLORED_COLORS = new HashMap<>();
    private static final Deque<Long> SAMPLE_QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED_KEYS = new HashSet<>();
    private static final Deque<Long> LRU_KEYS = new ArrayDeque<>();

    private static ClientLevel cachedLevel;
    private static int rescanCursor;

    static {
        fillCache(DISPLAY_CACHE, MAP_UNKNOWN);
    }

    private CzoMinimapHudRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.options.hideGui || CzoHudVisibility.isMinimapHidden()) {
            return;
        }

        Player player = minecraft.player;
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        graphics.nextStratum();
        drawMapBlock(graphics, minecraft, player, MAP_X, MAP_Y, MAP_SIZE);
    }

    private static void drawMapBlock(GuiGraphicsExtractor graphics, Minecraft minecraft, Player player, int x, int y, int size) {
        Font font = minecraft.font;
        ClientLevel level = minecraft.level;

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, FRAME_DARK);
        drawOutline(graphics, x - 2, y - 2, size + 4, size + 4, FRAME_MID);
        drawOutline(graphics, x - 1, y - 1, size + 2, size + 2, FRAME_LIGHT);

        int innerX = x + INNER_PAD;
        int innerY = y + INNER_PAD;
        graphics.fill(x, y, x + size, y + size, MAP_BG);

        if (level != null && cachedLevel != level) {
            clearExploredCache(level);
        }

        buildVisibleCacheAndQueueMissing(level, player);
        sampleQueuedTerrain(level);
        rescanVisibleTerrain(level);
        drawTerrainRuns(graphics, innerX, innerY);
        drawGrid(graphics, innerX, innerY, INNER_SIZE, 16);
        drawOtherPlayers(graphics, minecraft, player, innerX, innerY);
        drawPlayerMarker(graphics, x + size / 2, y + size / 2);

        drawCompass(graphics, font, player, x - 1, y - 1);
        drawClock(graphics, minecraft.level, x, y + size + 1);
    }

    private static void clearExploredCache(ClientLevel level) {
        cachedLevel = level;
        EXPLORED_COLORS.clear();
        SAMPLE_QUEUE.clear();
        QUEUED_KEYS.clear();
        LRU_KEYS.clear();
        rescanCursor = 0;
        fillCache(DISPLAY_CACHE, MAP_UNKNOWN);
    }

    /**
     * Every frame, project current camera/player movement against the already-known explored cache.
     * This is cheap and removes the visible row-by-row refresh. Missing cells are queued for small
     * incremental sampling instead of forcing a full terrain rebuild.
     */
    private static void buildVisibleCacheAndQueueMissing(ClientLevel level, Player player) {
        if (level == null || player == null) {
            fillCache(DISPLAY_CACHE, MAP_UNKNOWN);
            return;
        }

        int half = MAP_CELLS / 2;
        double radians = minimapRadians(player);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double playerX = player.getX();
        double playerZ = player.getZ();

        for (int cellY = 0; cellY < MAP_CELLS; cellY++) {
            int rowIndex = cellY * MAP_CELLS;
            double localZ = (cellY - half + 0.5D) * WORLD_BLOCKS_PER_CELL;

            for (int cellX = 0; cellX < MAP_CELLS; cellX++) {
                double localX = (cellX - half + 0.5D) * WORLD_BLOCKS_PER_CELL;
                int worldX = (int) Math.floor(playerX + localX * cos - localZ * sin);
                int worldZ = (int) Math.floor(playerZ + localX * sin + localZ * cos);
                long key = terrainKey(worldX, worldZ);
                int index = rowIndex + cellX;
                DISPLAY_KEYS[index] = key;

                Integer color = EXPLORED_COLORS.get(key);
                if (color == null) {
                    DISPLAY_CACHE[index] = MAP_UNKNOWN;
                    enqueueSample(key);
                } else {
                    DISPLAY_CACHE[index] = color;
                }
            }
        }
    }

    private static void enqueueSample(long key) {
        if (EXPLORED_COLORS.containsKey(key) || QUEUED_KEYS.contains(key)) {
            return;
        }
        SAMPLE_QUEUE.addLast(key);
        QUEUED_KEYS.add(key);
    }

    private static void sampleQueuedTerrain(ClientLevel level) {
        if (level == null) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int sampled = 0;
        while (sampled < SAMPLE_BUDGET_PER_FRAME && !SAMPLE_QUEUE.isEmpty()) {
            long key = SAMPLE_QUEUE.removeFirst();
            QUEUED_KEYS.remove(key);
            if (!EXPLORED_COLORS.containsKey(key)) {
                int worldX = keyX(key);
                int worldZ = keyZ(key);
                EXPLORED_COLORS.put(key, sampleTerrainColor(level, pos, worldX, worldZ));
                LRU_KEYS.addLast(key);
                trimExploredCacheIfNeeded();
                sampled++;
            }
        }
    }

    /**
     * Recheck a few visible cells per frame. If terrain changed after the area was explored, only
     * those touched cells are replaced in the cache instead of rebuilding the whole map.
     */
    private static void rescanVisibleTerrain(ClientLevel level) {
        if (level == null || EXPLORED_COLORS.isEmpty()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int total = DISPLAY_KEYS.length;
        for (int i = 0; i < RESCAN_BUDGET_PER_FRAME; i++) {
            int index = Math.floorMod(rescanCursor++, total);
            long key = DISPLAY_KEYS[index];
            if (!EXPLORED_COLORS.containsKey(key)) {
                continue;
            }
            int worldX = keyX(key);
            int worldZ = keyZ(key);
            int newColor = sampleTerrainColor(level, pos, worldX, worldZ);
            Integer oldColor = EXPLORED_COLORS.get(key);
            if (oldColor == null || oldColor.intValue() != newColor) {
                EXPLORED_COLORS.put(key, newColor);
                DISPLAY_CACHE[index] = newColor;
            }
        }
    }

    private static void trimExploredCacheIfNeeded() {
        while (EXPLORED_COLORS.size() > MAX_EXPLORED_BLOCKS && !LRU_KEYS.isEmpty()) {
            long oldest = LRU_KEYS.removeFirst();
            EXPLORED_COLORS.remove(oldest);
            QUEUED_KEYS.remove(oldest);
        }
    }

    private static int sampleTerrainColor(ClientLevel level, BlockPos.MutableBlockPos pos, int worldX, int worldZ) {
        if (!level.hasChunk(worldX >> 4, worldZ >> 4)) {
            return MAP_UNKNOWN;
        }

        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
        if (height <= level.getMinY()) {
            return MAP_UNKNOWN;
        }

        pos.set(worldX, height - 1, worldZ);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return MAP_UNKNOWN;
        }

        int color;
        if (!state.getFluidState().isEmpty() || state.liquid()) {
            color = 0xFF1D4D7A;
        } else {
            MapColor mapColor = state.getMapColor(level, pos);
            color = mapColor == MapColor.NONE ? 0xFF2B3333 : mapColor.calculateARGBColor(MapColor.Brightness.NORMAL);
        }

        int shade = heightShade(height);
        color = applyShade(color, shade);
        return quantizeForHud(forceOpaque(tintForCzoStyle(color)));
    }

    private static int heightShade(int height) {
        int mod = Math.floorMod(height, 16);
        if (mod <= 3) {
            return -10;
        }
        if (mod >= 12) {
            return 8;
        }
        return 0;
    }

    private static int tintForCzoStyle(int argb) {
        int a = (argb >>> 24) & 255;
        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;

        r = clamp((int) (r * 0.70F + 10), 0, 255);
        g = clamp((int) (g * 0.78F + 12), 0, 255);
        b = clamp((int) (b * 0.74F + 12), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int quantizeForHud(int argb) {
        int a = (argb >>> 24) & 255;
        int r = ((argb >>> 16) & 255) & 0xF0;
        int g = ((argb >>> 8) & 255) & 0xF0;
        int b = (argb & 255) & 0xF0;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int applyShade(int argb, int shade) {
        int a = (argb >>> 24) & 255;
        int r = clamp(((argb >>> 16) & 255) + shade, 0, 255);
        int g = clamp(((argb >>> 8) & 255) + shade, 0, 255);
        int b = clamp((argb & 255) + shade, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int forceOpaque(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    private static void drawTerrainRuns(GuiGraphicsExtractor graphics, int x, int y) {
        for (int cellY = 0; cellY < MAP_CELLS; cellY++) {
            int rowIndex = cellY * MAP_CELLS;
            int py = y + cellY * CELL_SIZE;
            int runStart = 0;
            int runColor = DISPLAY_CACHE[rowIndex];

            for (int cellX = 1; cellX <= MAP_CELLS; cellX++) {
                int color = cellX >= MAP_CELLS ? Integer.MIN_VALUE : DISPLAY_CACHE[rowIndex + cellX];
                if (color != runColor) {
                    int x1 = x + runStart * CELL_SIZE;
                    int x2 = x + cellX * CELL_SIZE;
                    graphics.fill(x1, py, x2, py + CELL_SIZE, runColor);
                    runStart = cellX;
                    runColor = color;
                }
            }
        }
    }

    private static void drawOtherPlayers(GuiGraphicsExtractor graphics, Minecraft minecraft, Player self, int innerX, int innerY) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        double radians = minimapRadians(self);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        int half = MAP_CELLS / 2;

        for (AbstractClientPlayer other : level.players()) {
            if (other == self || other.isSpectator()) {
                continue;
            }

            double dx = other.getX() - self.getX();
            double dz = other.getZ() - self.getZ();
            double localX = dx * cos + dz * sin;
            double localZ = -dx * sin + dz * cos;
            int cellX = half + (int) Math.round(localX / WORLD_BLOCKS_PER_CELL);
            int cellY = half + (int) Math.round(localZ / WORLD_BLOCKS_PER_CELL);
            if (cellX < 1 || cellY < 1 || cellX >= MAP_CELLS - 1 || cellY >= MAP_CELLS - 1) {
                continue;
            }

            int px = innerX + cellX * CELL_SIZE;
            int py = innerY + cellY * CELL_SIZE;
            graphics.fill(px - 2, py - 2, px + 3, py + 3, OTHER_PLAYER_OUTLINE);
            graphics.fill(px - 1, py - 1, px + 2, py + 2, OTHER_PLAYER_MARKER);
        }
    }

    private static void drawGrid(GuiGraphicsExtractor graphics, int x, int y, int size, int step) {
        for (int offset = step; offset < size; offset += step) {
            graphics.fill(x + offset, y, x + offset + 1, y + size, MAP_GRID);
            graphics.fill(x, y + offset, x + size, y + offset + 1, MAP_GRID);
        }
    }

    private static void drawPlayerMarker(GuiGraphicsExtractor graphics, int cx, int cy) {
        graphics.fill(cx - 1, cy - 4, cx + 1, cy - 2, PLAYER_MARKER_DARK);
        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, PLAYER_MARKER_DARK);
        graphics.fill(cx - 1, cy + 2, cx + 1, cy + 4, PLAYER_MARKER_DARK);
        graphics.fill(cx - 1, cy - 3, cx + 1, cy - 1, PLAYER_MARKER);
        graphics.fill(cx - 1, cy + 1, cx + 1, cy + 3, PLAYER_MARKER);
        graphics.fill(cx - 3, cy - 1, cx - 1, cy + 1, PLAYER_MARKER);
        graphics.fill(cx + 1, cy - 1, cx + 3, cy + 1, PLAYER_MARKER);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, PLAYER_MARKER);
    }

    private static void drawCompass(GuiGraphicsExtractor graphics, Font font, Player player, int x, int y) {
        int size = 14;
        int cx = x + size / 2;
        int cy = y + size / 2;

        graphics.fill(x + 3, y, x + size - 3, y + size, 0xE00D1212);
        graphics.fill(x, y + 3, x + size, y + size - 3, 0xE00D1212);
        drawOutline(graphics, x + 2, y + 1, size - 4, size - 2, FRAME_MID);
        drawOutline(graphics, x + 1, y + 2, size - 2, size - 4, FRAME_MID);
        graphics.text(font, "N", cx - font.width("N") / 2, y - 3, TEXT, true);

        double angle = Math.toRadians(180.0F - player.getYRot() + MINIMAP_ROTATION_DEGREES);
        int tipX = cx + (int) Math.round(Math.sin(angle) * 5.0D);
        int tipY = cy - (int) Math.round(Math.cos(angle) * 5.0D);
        int tailX = cx - (int) Math.round(Math.sin(angle) * 3.0D);
        int tailY = cy + (int) Math.round(Math.cos(angle) * 3.0D);

        drawPixelLine(graphics, tailX, tailY, tipX, tipY, NORTH_RED);
        graphics.fill(tipX, tipY, tipX + 1, tipY + 1, NORTH_RED);
        graphics.fill(cx, cy, cx + 1, cy + 1, TEXT_MUTED);
    }

    private static void drawClock(GuiGraphicsExtractor graphics, ClientLevel level, int x, int y) {
        String time = level == null ? "--:--" : minecraftTimeToHuman(level.getDefaultClockTime());
        int w = 28;
        int h = 8;

        graphics.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x88000000);
        graphics.fill(x, y, x + w, y + h, 0xCC202828);
        drawOutline(graphics, x, y, w, h, FRAME_MID);
        drawTinyClockText(graphics, time, x + 4, y + 1, CLOCK_TEXT);
    }

    private static void drawTinyClockText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ':') {
                graphics.fill(cursor, y + 1, cursor + 1, y + 2, color);
                graphics.fill(cursor, y + 4, cursor + 1, y + 5, color);
                cursor += 3;
            } else {
                drawTinyDigit(graphics, c, cursor, y, color);
                cursor += 4;
            }
        }
    }

    private static void drawTinyDigit(GuiGraphicsExtractor graphics, char digit, int x, int y, int color) {
        String[] rows = switch (digit) {
            case '0' -> new String[]{"111", "101", "101", "101", "111"};
            case '1' -> new String[]{"010", "110", "010", "010", "111"};
            case '2' -> new String[]{"111", "001", "111", "100", "111"};
            case '3' -> new String[]{"111", "001", "111", "001", "111"};
            case '4' -> new String[]{"101", "101", "111", "001", "001"};
            case '5' -> new String[]{"111", "100", "111", "001", "111"};
            case '6' -> new String[]{"111", "100", "111", "101", "111"};
            case '7' -> new String[]{"111", "001", "010", "010", "010"};
            case '8' -> new String[]{"111", "101", "111", "101", "111"};
            case '9' -> new String[]{"111", "101", "111", "001", "111"};
            default -> new String[]{"000", "000", "000", "000", "000"};
        };

        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '1') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    private static String minecraftTimeToHuman(long dayTime) {
        long ticks = Math.floorMod(dayTime + 6000L, 24000L);
        int totalMinutes = (int) ((ticks * 1440L) / 24000L);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return twoDigits(hours) + ":" + twoDigits(minutes);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private static void fillCache(int[] cache, int color) {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = color;
        }
    }

    private static double minimapRadians(Player player) {
        if (player == null) {
            return 0.0D;
        }
        return Math.toRadians(normalizeYaw(player.getYRot() + MINIMAP_ROTATION_DEGREES));
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private static long terrainKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int keyX(long key) {
        return (int) (key >> 32);
    }

    private static int keyZ(long key) {
        return (int) key;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawPixelLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
