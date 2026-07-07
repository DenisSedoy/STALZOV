package com.czo.client.screen.customization;

import com.czo.client.appearance.CzoAppearanceSelection;
import com.czo.client.appearance.CzoAppearanceSelection.Unlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Compact CZO appearance/customization screen. */
public class CzoAppearanceScreen extends Screen {
    private static final int BG = 0xEE050706;
    private static final int PANEL = 0xAA111511;
    private static final int PANEL_DARK = 0xBB070907;
    private static final int PANEL_SOFT = 0x661A211A;
    private static final int OUTLINE = 0xFF394139;
    private static final int OUTLINE_HOVER = 0xFFE4C86A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_MUTED = 0xFF9B9B9B;
    private static final int TEXT_DIM = 0xFF666666;
    private static final int GOOD = 0xFF70E070;
    private static final int WARNING = 0xFFE4C86A;

    private static final int PREVIEW_W = 48;
    private static final int PREVIEW_H = 96;
    private static final int RIGHT_ROW_H = 22;
    private static final int RIGHT_ROW_GAP = 5;

    private final Screen parent;

    private int left;
    private int top;
    private int uiW;
    private int uiH;

    private int doneX;
    private int doneY;
    private int doneW;
    private int doneH;

    private int rightX;
    private int rightY;
    private int rightW;
    private int rightH;
    private int rightViewportTop;
    private int rightViewportBottom;
    private int rightScrollPixels;

    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;
    private float previewYaw;
    private boolean draggingPreview;

    private Category selectedCategory = Category.APPEARANCE;

    public CzoAppearanceScreen(Screen parent) {
        super(
                Minecraft.getInstance(),
                Minecraft.getInstance().font,
                Component.literal("CZO Appearance")
        );
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(new CzoAppearanceScreen(parent));
        }
    }

    @Override
    protected void init() {
        this.uiW = Math.min(520, Math.max(460, this.width - 80));
        this.uiH = Math.min(310, Math.max(230, this.height - 80));
        this.uiW = Math.min(this.uiW, this.width - 20);
        this.uiH = Math.min(this.uiH, this.height - 20);

        this.left = Math.max(10, (this.width - this.uiW) / 2);
        this.top = Math.max(10, (this.height - this.uiH) / 2);

        this.doneW = 86;
        this.doneH = 17;
        this.doneX = this.left + this.uiW - this.doneW - 10;
        this.doneY = this.top + this.uiH - this.doneH - 10;

        this.rightW = 168;
        this.rightX = this.left + this.uiW - this.rightW - 10;
        this.rightY = this.top + 38;
        this.rightH = this.uiH - 60;
        this.rightViewportTop = this.rightY + 25;
        this.rightViewportBottom = this.rightY + this.rightH - 8;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG);
        graphics.fillGradient(0, 0, this.width, this.height, 0xAA0A0E0A, 0xDD020302);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        CzoAppearanceSelection.sanitize(player);
        drawWindow(graphics, mouseX, mouseY, player);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        if (contains(this.doneX, this.doneY, this.doneW, this.doneH, mouseX, mouseY)) {
            this.onClose();
            return true;
        }

        if (contains(this.previewX, this.previewY, this.previewW, this.previewH, mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }

        if (handleCategoryClick(mouseX, mouseY)) {
            return true;
        }

        if (handleOptionClick(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && (this.draggingPreview || contains(this.previewX, this.previewY, this.previewW, this.previewH, mouseX, mouseY))) {
            this.draggingPreview = true;
            this.previewYaw = wrapYaw(this.previewYaw + (float) offsetX * 0.85F);
            return true;
        }

        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (contains(this.rightX, this.rightY, this.rightW, this.rightH, (int) x, (int) y)) {
            int maxScroll = maxRightScroll();
            this.rightScrollPixels = clamp(this.rightScrollPixels + (scrollY < 0 ? 14 : -14), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private void drawWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(this.left, this.top, this.left + this.uiW, this.top + this.uiH, 0xAA090B09);
        drawOutline(graphics, this.left, this.top, this.uiW, this.uiH, 0xFF222822);

        drawHeader(graphics);
        drawLeftPanel(graphics, mouseX, mouseY);
        drawPreviewPanel(graphics, mouseX, mouseY, player);
        drawRightPanel(graphics, mouseX, mouseY, player);
        drawDoneButton(graphics, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        int barH = 28;
        graphics.fill(this.left, this.top, this.left + this.uiW, this.top + barH, 0xCC0D100D);
        drawOutline(graphics, this.left, this.top, this.uiW, barH, 0xFF242A24);
        drawCenteredScaledText(graphics, "ВНЕШНИЙ ВИД", this.left + this.uiW / 2, this.top + 9, WARNING, 0.62F);
    }

    private void drawLeftPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.left + 10;
        int y = this.top + 38;
        int w = 122;
        int h = this.uiH - 60;

        graphics.fill(x, y, x + w, y + h, PANEL);
        drawOutline(graphics, x, y, w, h, OUTLINE);
        drawTinyText(graphics, "КАТЕГОРИИ", x + 8, y + 8, TEXT);

        int rowY = y + 25;
        for (Category category : Category.values()) {
            drawCategory(graphics, x + 8, rowY, w - 16, 20, category, mouseX, mouseY);
            rowY += 25;
        }
    }

    private void drawCategory(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Category category, int mouseX, int mouseY) {
        boolean active = this.selectedCategory == category;
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        int bg = active ? 0xAA263126 : (hovered ? 0xAA202820 : PANEL_DARK);
        int outline = active ? WARNING : OUTLINE;
        int text = active ? TEXT : TEXT_MUTED;
        graphics.fill(x, y, x + w, y + h, bg);
        drawOutline(graphics, x, y, w, h, outline);
        drawTinyText(graphics, category.title, x + 8, y + 7, text);
    }

    private void drawPreviewPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, LocalPlayer player) {
        int x = this.left + 142;
        int y = this.top + 38;
        int w = this.uiW - 142 - this.rightW - 20;
        int h = this.uiH - 60;
        w = Math.max(132, w);

        this.previewX = x;
        this.previewY = y;
        this.previewW = w;
        this.previewH = h;

        graphics.fill(x, y, x + w, y + h, PANEL_SOFT);
        drawOutline(graphics, x, y, w, h, OUTLINE);
        drawCenteredScaledText(graphics, "ПРЕВЬЮ", x + w / 2, y + 8, TEXT_MUTED, 0.56F);
        drawCenteredScaledText(graphics, "зажми ЛКМ и тяни — поворот", x + w / 2, y + h - 14, TEXT_DIM, 0.42F);

        if (player == null) {
            drawCenteredScaledText(graphics, "нет игрока", x + w / 2, y + h / 2, TEXT_DIM, 0.52F);
            return;
        }

        // In 26.1.2 the safe vanilla helper we already use is still the
        // follows-mouse preview extractor. We feed it a synthetic mouse X from
        // previewYaw so the wheel can spin the model around the Y axis without
        // breaking the GUI API.
        int centerX = x + w / 2;
        int centerY = y + h / 2;
        int fakeMouseX = centerX + Math.round((float) Math.sin(Math.toRadians(this.previewYaw)) * 160.0F);
        int fakeMouseY = centerY;

        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                x + 8,
                y + 24,
                x + w - 8,
                y + h - 20,
                Math.max(38, Math.min(58, h / 3)),
                0.0625F,
                fakeMouseX,
                fakeMouseY,
                player
        );
    }

    private void drawRightPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(this.rightX, this.rightY, this.rightX + this.rightW, this.rightY + this.rightH, PANEL);
        drawOutline(graphics, this.rightX, this.rightY, this.rightW, this.rightH, OUTLINE);
        drawTinyText(graphics, this.selectedCategory.title, this.rightX + 8, this.rightY + 8, TEXT);

        List<AppearanceOption> options = unlockedOptions(player, this.selectedCategory);
        int contentH = options.size() * (RIGHT_ROW_H + RIGHT_ROW_GAP);
        int viewportH = this.rightViewportBottom - this.rightViewportTop;
        this.rightScrollPixels = clamp(this.rightScrollPixels, 0, Math.max(0, contentH - viewportH));

        graphics.enableScissor(this.rightX + 2, this.rightViewportTop, this.rightX + this.rightW - 8, this.rightViewportBottom);
        int rowY = this.rightViewportTop - this.rightScrollPixels;
        for (AppearanceOption option : options) {
            drawOptionRow(graphics, this.rightX + 8, rowY, this.rightW - 24, option, mouseX, mouseY);
            rowY += RIGHT_ROW_H + RIGHT_ROW_GAP;
        }
        graphics.disableScissor();

        if (options.isEmpty()) {
            drawTinyText(graphics, "Пока нет открытых вариантов", this.rightX + 8, this.rightViewportTop + 6, TEXT_DIM);
        }

        drawRightScrollbar(graphics, contentH, viewportH);
    }

    private void drawOptionRow(GuiGraphicsExtractor graphics, int x, int y, int w, AppearanceOption option, int mouseX, int mouseY) {
        boolean selected = isSelected(option);
        boolean hovered = contains(x, y, w, RIGHT_ROW_H, mouseX, mouseY);
        int bg = selected ? 0xAA263126 : (hovered ? 0xAA202820 : 0x66333333);
        int outline = selected ? WARNING : (hovered ? OUTLINE_HOVER : OUTLINE);
        graphics.fill(x, y, x + w, y + RIGHT_ROW_H, bg);
        drawOutline(graphics, x, y, w, RIGHT_ROW_H, outline);
        drawTinyText(graphics, option.title, x + 7, y + 6, selected ? WARNING : TEXT);
        drawTinyText(graphics, option.unlockText, x + w - 30, y + 6, option.unlock == Unlock.COMMON ? GOOD : TEXT_MUTED);
    }

    private void drawRightScrollbar(GuiGraphicsExtractor graphics, int contentH, int viewportH) {
        int barX = this.rightX + this.rightW - 6;
        graphics.fill(barX, this.rightViewportTop, barX + 3, this.rightViewportBottom, 0x55000000);
        if (contentH <= viewportH) {
            return;
        }
        int trackH = this.rightViewportBottom - this.rightViewportTop;
        int thumbH = Math.max(14, trackH * viewportH / Math.max(viewportH, contentH));
        int thumbY = this.rightViewportTop + (trackH - thumbH) * this.rightScrollPixels / Math.max(1, contentH - viewportH);
        graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAA9B9B9B);
    }

    private void drawDoneButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean hovered = contains(this.doneX, this.doneY, this.doneW, this.doneH, mouseX, mouseY);
        graphics.fill(this.doneX, this.doneY, this.doneX + this.doneW, this.doneY + this.doneH, hovered ? 0xAA263126 : PANEL_DARK);
        drawOutline(graphics, this.doneX, this.doneY, this.doneW, this.doneH, hovered ? OUTLINE_HOVER : WARNING);
        drawCenteredScaledText(graphics, "ГОТОВО", this.doneX + this.doneW / 2, this.doneY + 5, WARNING, 0.55F);
    }

    private boolean handleCategoryClick(int mouseX, int mouseY) {
        int x = this.left + 18;
        int y = this.top + 63;
        int w = 106;
        int h = 20;
        for (Category category : Category.values()) {
            if (contains(x, y, w, h, mouseX, mouseY)) {
                this.selectedCategory = category;
                this.rightScrollPixels = 0;
                return true;
            }
            y += 25;
        }
        return false;
    }

    private boolean handleOptionClick(int mouseX, int mouseY) {
        if (!contains(this.rightX, this.rightViewportTop, this.rightW, this.rightViewportBottom - this.rightViewportTop, mouseX, mouseY)) {
            return false;
        }

        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        List<AppearanceOption> options = unlockedOptions(player, this.selectedCategory);
        int rowY = this.rightViewportTop - this.rightScrollPixels;
        for (AppearanceOption option : options) {
            if (contains(this.rightX + 8, rowY, this.rightW - 24, RIGHT_ROW_H, mouseX, mouseY)) {
                select(option);
                return true;
            }
            rowY += RIGHT_ROW_H + RIGHT_ROW_GAP;
        }
        return false;
    }

    private List<AppearanceOption> unlockedOptions(LocalPlayer player, Category category) {
        List<AppearanceOption> result = new ArrayList<>();
        for (AppearanceOption option : allOptions(category)) {
            if (CzoAppearanceSelection.isUnlocked(player, option.unlock)) {
                result.add(option);
            }
        }
        return result;
    }

    private List<AppearanceOption> allOptions(Category category) {
        return switch (category) {
            case APPEARANCE -> List.of(
                    new AppearanceOption(Category.APPEARANCE, CzoAppearanceSelection.SKIN_BASE, "НЕТ СКИНА (база)", Unlock.COMMON, "всем"),
                    new AppearanceOption(Category.APPEARANCE, CzoAppearanceSelection.SKIN_ISKATEL, "Искатель", Unlock.PATHFINDER, "иск"),
                    new AppearanceOption(Category.APPEARANCE, CzoAppearanceSelection.SKIN_CONTRABANDIST, "Контрабандист", Unlock.CONTRABANDIST, "конт")
            );
            case TOP -> List.of(
                    new AppearanceOption(Category.TOP, CzoAppearanceSelection.TOP_BASE_SWEATER, "Бабушкин свитер", Unlock.COMMON, "всем"),
                    new AppearanceOption(Category.TOP, CzoAppearanceSelection.TOP_ISKATEL, "Куртка Искателя", Unlock.PATHFINDER, "иск"),
                    new AppearanceOption(Category.TOP, CzoAppearanceSelection.TOP_CONTRABANDIST, "Плащ Контрабандиста", Unlock.CONTRABANDIST, "конт")
            );
            case BOTTOM -> List.of(
                    new AppearanceOption(Category.BOTTOM, CzoAppearanceSelection.PANTS_BASE_JEANS, "Потёртые джинсы", Unlock.COMMON, "всем"),
                    new AppearanceOption(Category.BOTTOM, CzoAppearanceSelection.PANTS_ISKATEL, "Штаны Искателя", Unlock.PATHFINDER, "иск"),
                    new AppearanceOption(Category.BOTTOM, CzoAppearanceSelection.PANTS_CONTRABANDIST, "Штаны Контрабандиста", Unlock.CONTRABANDIST, "конт")
            );
            case BOOTS -> List.of(
                    new AppearanceOption(Category.BOOTS, CzoAppearanceSelection.BOOTS_BASE, "Базовые ботинки", Unlock.COMMON, "всем"),
                    new AppearanceOption(Category.BOOTS, CzoAppearanceSelection.BOOTS_ISKATEL, "Ботинки Искателя", Unlock.PATHFINDER, "иск"),
                    new AppearanceOption(Category.BOOTS, CzoAppearanceSelection.BOOTS_CONTRABANDIST, "Кеды Контрабандиста", Unlock.CONTRABANDIST, "конт")
            );
        };
    }

    private boolean isSelected(AppearanceOption option) {
        return switch (option.category) {
            case APPEARANCE -> option.id.equals(CzoAppearanceSelection.skin());
            case TOP -> option.id.equals(CzoAppearanceSelection.top());
            case BOTTOM -> option.id.equals(CzoAppearanceSelection.pants());
            case BOOTS -> option.id.equals(CzoAppearanceSelection.boots());
        };
    }

    private void select(AppearanceOption option) {
        switch (option.category) {
            case APPEARANCE -> CzoAppearanceSelection.setSkin(option.id);
            case TOP -> CzoAppearanceSelection.setTop(option.id);
            case BOTTOM -> CzoAppearanceSelection.setPants(option.id);
            case BOOTS -> CzoAppearanceSelection.setBoots(option.id);
        }
    }

    private int maxRightScroll() {
        int viewportH = Math.max(1, this.rightViewportBottom - this.rightViewportTop);
        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        int contentH = unlockedOptions(player, this.selectedCategory).size() * (RIGHT_ROW_H + RIGHT_ROW_GAP);
        return Math.max(0, contentH - viewportH);
    }

    private float wrapYaw(float value) {
        value %= 360.0F;
        if (value < 0.0F) {
            value += 360.0F;
        }
        return value;
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawTinyText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, 0.48F);
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, Component.literal(text), Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popMatrix();
    }

    private void drawCenteredScaledText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color, float scale) {
        int scaledWidth = Math.round(this.font.width(text) * scale);
        drawScaledText(graphics, text, centerX - scaledWidth / 2, y, color, scale);
    }

    private boolean contains(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Category {
        APPEARANCE("ВНЕШНОСТЬ"),
        TOP("ВЕРХ"),
        BOTTOM("НИЗ"),
        BOOTS("ОБУВЬ");

        private final String title;

        Category(String title) {
            this.title = title;
        }
    }

    private record AppearanceOption(Category category, String id, String title, Unlock unlock, String unlockText) {
    }
}
