package com.czo.client.hud;

import com.czo.client.CzoContaminationClientEvents;
import com.czo.client.movement.CzoClientStaminaEvents;
import com.czo.contamination.ContaminationType;
import com.czo.item.gun.GunItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Stage 7H: compact CoP-like survival HUD.
 *
 * <p>Ammo text, ammo count and the loaded ammo item icon live in the same lower ammo strip.
 * The old right weapon preview tile is removed, because CZO shows ammo first.</p>
 */
public final class CzoSurvivalHudRenderer {
    public static final int PANEL_W = 104;
    public static final int PANEL_H = 43;
    private static final int PANEL_MARGIN = 7;

    private static final int PANEL_BG = 0xB80B1010;
    private static final int PANEL_BG_DARK = 0xCC050808;
    private static final int PANEL_BG_SOFT = 0x991C2525;
    private static final int PANEL_OUTLINE = 0xFF2E3838;
    private static final int PANEL_OUTLINE_BRIGHT = 0xFF526060;

    private static final int TEXT = 0xFFE9EEEE;
    private static final int TEXT_MUTED = 0xFF9AA6A6;
    private static final int AMMO_TEXT = 0xFFFFC75A;

    private static final int HP_EMPTY = 0xFF2A0507;
    private static final int HP_FILL = 0xFFE20A0A;
    private static final int HP_FILL_LOW = 0xFFB91A1A;
    private static final int STAMINA_EMPTY = 0xFF061325;
    private static final int STAMINA_FILL = 0xFF4A8CFF;
    private static final int STAMINA_FILL_LOW = 0xFF315C99;

    private static final int ICON_OUTLINE = 0xFF6D7474;
    private static final int ICON_SYMBOL = 0xFF050505;
    private static final int SEVERITY_LIGHT = 0xFF7CFF1F;
    private static final int SEVERITY_MEDIUM = 0xFFFFE42A;
    private static final int SEVERITY_HEAVY = 0xFFFF8A1C;
    private static final int SEVERITY_LETHAL = 0xFFFF2020;

    private CzoSurvivalHudRenderer() {
    }

    public static int panelX(int guiWidth) {
        return guiWidth - PANEL_W - PANEL_MARGIN;
    }

    public static int panelY(int guiHeight) {
        return guiHeight - PANEL_H - PANEL_MARGIN;
    }

    public static void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.options.hideGui || CzoHudVisibility.isWeaponHudHidden()) {
            return;
        }

        Player player = minecraft.player;
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        graphics.nextStratum();
        drawLowHealthOverlay(graphics, player);
        drawMainPanel(graphics, minecraft, player, panelX(graphics.guiWidth()), panelY(graphics.guiHeight()), PANEL_W, PANEL_H);
    }


    private static void drawLowHealthOverlay(GuiGraphicsExtractor graphics, Player player) {
        float ratio = safeRatio(player.getHealth(), Math.max(1.0F, player.getMaxHealth()));
        if (ratio > 0.30F) {
            return;
        }

        int maxAlpha = 0x26; // ~15% opacity
        int alpha = Math.round(maxAlpha * (1.0F - ratio / 0.30F));
        alpha = Math.max(0, Math.min(maxAlpha, alpha));
        if (alpha <= 0) {
            return;
        }

        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0x00FF0000);
    }

    private static void drawMainPanel(GuiGraphicsExtractor graphics, Minecraft minecraft, Player player, int x, int y, int w, int h) {
        Font font = minecraft.font;

        graphics.fill(x, y, x + w, y + h, PANEL_BG);
        drawOutline(graphics, x, y, w, h, PANEL_OUTLINE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x552A3636);

        drawContaminationIcons(graphics, minecraft, x + 16, y - 13);

        drawSmallIconPlate(graphics, x + 4, y + 5, "♥");
        drawSmallIconPlate(graphics, x + 4, y + 17, "ϟ");

        float hpRatio = safeRatio(player.getHealth(), Math.max(1.0F, player.getMaxHealth()));
        float staminaRatio = temporaryStaminaRatio(player);

        int barX = x + 16;
        int barW = w - 22;
        drawBar(graphics, barX, y + 6, barW, 7, hpRatio, HP_EMPTY, hpRatio <= 0.25F ? HP_FILL_LOW : HP_FILL);
        drawBar(graphics, barX, y + 19, barW, 6, staminaRatio, STAMINA_EMPTY, staminaRatio <= 0.25F ? STAMINA_FILL_LOW : STAMINA_FILL);

        ItemStack weapon = currentWeapon(player);
        boolean isGun = !weapon.isEmpty() && weapon.getItem() instanceof GunItem;
        GunItem gun = isGun ? (GunItem) weapon.getItem() : null;
        String ammoName = "ПУСТО";
        String ammoCount = "—";
        ItemStack ammoIcon = ItemStack.EMPTY;

        if (isGun) {
            ammoName = gun.getLoadedAmmoDisplayName(weapon);
            ammoCount = gun.getHudAmmoText(weapon);
            ammoIcon = ammoIconStack(gun, weapon);
        }

        drawAmmoStrip(graphics, font, ammoName, ammoCount, ammoIcon, isGun, x + 16, y + 29, w - 20, 11);
    }

    private static void drawAmmoStrip(GuiGraphicsExtractor graphics, Font font, String ammoName, String ammoCount, ItemStack ammoIcon, boolean active, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BG_DARK);
        drawOutline(graphics, x, y, w, h, active ? PANEL_OUTLINE_BRIGHT : PANEL_OUTLINE);

        int iconX = x + w - 15;
        int iconY = y - 2;
        graphics.fill(iconX - 1, y + 1, iconX + 15, y + h - 1, 0x66293232);
        drawOutline(graphics, iconX - 1, y + 1, 16, h - 2, active ? PANEL_OUTLINE_BRIGHT : PANEL_OUTLINE);

        if (!ammoIcon.isEmpty()) {
            graphics.item(ammoIcon, iconX, iconY);
        } else {
            drawCenteredText(graphics, font, "—", iconX + 7, y + 1, TEXT_MUTED, false);
        }

        int textW = Math.max(16, w - 22);
        graphics.text(font, abbreviateToWidth(font, ammoName, textW), x + 2, y - 1, active ? TEXT : TEXT_MUTED, false);
        graphics.text(font, abbreviateToWidth(font, ammoCount, textW), x + 2, y + 5, AMMO_TEXT, false);
    }

    private static ItemStack ammoIconStack(GunItem gun, ItemStack weapon) {
        String ammoItemId = gun.getHudLoadedAmmoItemId(weapon);
        if (ammoItemId == null || ammoItemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        try {
            Identifier id = Identifier.parse(ammoItemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void drawContaminationIcons(GuiGraphicsExtractor graphics, Minecraft minecraft, int startX, int y) {
        int x = startX;
        for (ContaminationType type : ContaminationType.values()) {
            int severity = CzoContaminationClientEvents.getHudSeverityLevel(type);
            if (severity <= 0) {
                continue;
            }

            int color = severityColor(severity);
            drawContaminationIcon(graphics, minecraft.font, x, y, type.symbol(), color);
            x += 13;
        }
    }

    private static void drawContaminationIcon(GuiGraphicsExtractor graphics, Font font, int x, int y, String symbol, int color) {
        graphics.fill(x + 3, y, x + 9, y + 12, ICON_OUTLINE);
        graphics.fill(x, y + 3, x + 12, y + 9, ICON_OUTLINE);
        graphics.fill(x + 3, y + 2, x + 9, y + 10, color);
        graphics.fill(x + 2, y + 3, x + 10, y + 9, color);

        String tiny = symbol == null || symbol.isBlank() ? "!" : symbol;
        int textW = font.width(tiny);
        int tx = x + 6 - textW / 2;
        int ty = y + 2;
        graphics.text(font, tiny, tx, ty, ICON_SYMBOL, false);
    }

    private static int severityColor(int severity) {
        return switch (severity) {
            case 1 -> SEVERITY_LIGHT;
            case 2 -> SEVERITY_MEDIUM;
            case 3 -> SEVERITY_HEAVY;
            default -> SEVERITY_LETHAL;
        };
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, float ratio, int emptyColor, int fillColor) {
        ratio = clamp(ratio, 0.0F, 1.0F);
        graphics.fill(x, y, x + w, y + h, PANEL_BG_DARK);
        drawOutline(graphics, x, y, w, h, PANEL_OUTLINE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, emptyColor);

        int fillW = Math.round((w - 2) * ratio);
        if (fillW > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, fillColor);
            graphics.fill(x + 1, y + 1, x + 1 + fillW, y + 2, 0x44FFFFFF);
        }
    }

    private static void drawSmallIconPlate(GuiGraphicsExtractor graphics, int x, int y, String text) {
        int size = 9;
        graphics.fill(x, y, x + size, y + size, PANEL_BG_SOFT);
        drawOutline(graphics, x, y, size, size, PANEL_OUTLINE);
        Minecraft minecraft = Minecraft.getInstance();
        int textW = minecraft.font.width(text);
        graphics.text(minecraft.font, text, x + size / 2 - textW / 2, y, 0xFFFFFFFF, false);
    }

    private static ItemStack currentWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            return mainHand;
        }
        return ItemStack.EMPTY;
    }

    private static String abbreviateToWidth(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "—";
        }

        String trimmed = text.trim();
        if (font.width(trimmed) <= maxWidth) {
            return trimmed;
        }

        String ellipsis = "...";
        int end = trimmed.length();
        while (end > 1 && font.width(trimmed.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return trimmed.substring(0, Math.max(1, end)) + ellipsis;
    }

    private static float temporaryStaminaRatio(Player player) {
        return clamp(CzoClientStaminaEvents.staminaRatio(player), 0.0F, 1.0F);
    }

    private static float safeRatio(float value, float max) {
        if (max <= 0.0F) {
            return 0.0F;
        }
        return clamp(value / max, 0.0F, 1.0F);
    }

    private static void drawCenteredText(GuiGraphicsExtractor graphics, Font font, String text, int cx, int y, int color, boolean shadow) {
        int textW = font.width(text);
        graphics.text(font, text, cx - textW / 2, y, color, shadow);
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
