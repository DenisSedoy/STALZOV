package com.czo.client.hud;

import com.czo.CZO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Small client-side HUD visibility toggles.
 *
 * <p>Stage 7G: when chat is open, small lime-highlighted buttons appear near HUD blocks.
 * Clicking them toggles the corresponding CZO HUD element. This is intentionally client-only
 * and non-persistent for now; later it can move into a config file.</p>
 */
public final class CzoHudVisibility {
    private static final Identifier HIDE_ICON = Identifier.fromNamespaceAndPath(CZO.MODID, "textures/gui/hide_hud_button.png");
    private static final int ICON = 12;
    private static final int LIME = 0xFF36FF00;
    private static final int LIME_SOFT = 0x9936FF00;
    private static final int BG = 0xCC0A1010;
    private static final int MUTED = 0xFF9AA6A6;

    private static boolean minimapHidden;
    private static boolean weaponHudHidden;

    private CzoHudVisibility() {
    }

    public static boolean isMinimapHidden() {
        return minimapHidden;
    }

    public static boolean isWeaponHudHidden() {
        return weaponHudHidden;
    }

    public static boolean shouldShowToggleButtons(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.options.hideGui) {
            return false;
        }
        Player player = minecraft.player;
        return !player.isCreative() && !player.isSpectator();
    }

    public static void renderChatToggles(GuiGraphicsExtractor graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (!shouldShowToggleButtons(minecraft)) {
            return;
        }

        drawToggle(graphics, minimapToggleX(), minimapToggleY(), mouseX, mouseY, minimapHidden);
        drawToggle(graphics, weaponToggleX(graphics.guiWidth(), graphics.guiHeight()), weaponToggleY(graphics.guiHeight()), mouseX, mouseY, weaponHudHidden);
    }

    public static boolean handleChatClick(Minecraft minecraft, int mouseX, int mouseY) {
        if (!shouldShowToggleButtons(minecraft)) {
            return false;
        }

        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();

        if (contains(minimapToggleX(), minimapToggleY(), ICON, ICON, mouseX, mouseY)) {
            minimapHidden = !minimapHidden;
            return true;
        }

        if (contains(weaponToggleX(guiW, guiH), weaponToggleY(guiH), ICON, ICON, mouseX, mouseY)) {
            weaponHudHidden = !weaponHudHidden;
            return true;
        }

        return false;
    }

    private static void drawToggle(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, boolean hidden) {
        boolean hovered = contains(x, y, ICON, ICON, mouseX, mouseY);
        graphics.fill(x - 1, y - 1, x + ICON + 1, y + ICON + 1, hovered ? LIME_SOFT : BG);
        drawOutline(graphics, x - 1, y - 1, ICON + 2, ICON + 2, hovered ? LIME : 0xFF2E3838);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HIDE_ICON, x, y, 0.0F, 0.0F, ICON, ICON, 32, 32, 32, 32);

        if (hidden) {
            graphics.fill(x + 2, y + ICON - 3, x + ICON - 2, y + ICON - 2, LIME);
        }
    }

    private static int minimapToggleX() {
        return CzoMinimapHudRenderer.MAP_X + CzoMinimapHudRenderer.MAP_SIZE + 4;
    }

    private static int minimapToggleY() {
        return CzoMinimapHudRenderer.MAP_Y + 1;
    }

    private static int weaponToggleX(int guiWidth, int guiHeight) {
        return CzoSurvivalHudRenderer.panelX(guiWidth) - ICON - 4;
    }

    private static int weaponToggleY(int guiHeight) {
        return CzoSurvivalHudRenderer.panelY(guiHeight) + CzoSurvivalHudRenderer.PANEL_H - ICON - 2;
    }

    private static boolean contains(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
