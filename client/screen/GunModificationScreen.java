package com.czo.client.screen;

import com.czo.client.gunrender.CzoGunPreviewGui;
import com.czo.item.gun.GunItem;
import com.czo.item.gun.definition.CzoGuns;
import com.czo.item.gun.definition.GunDefinition;
import com.czo.item.gun.definition.GunSlotDefinition;
import com.czo.item.gun.definition.GunStats;
import com.czo.item.gun.module.CzoGunModules;
import com.czo.item.gun.module.GunModule;
import com.czo.item.gun.module.GunModuleSlot;
import com.czo.network.ServerboundSetGunModulePacket;
import com.czo.registry.CzoDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.lwjgl.glfw.GLFW;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class GunModificationScreen extends Screen {
    private static final int COLOR_BACKGROUND = 0xDD050505;
    private static final int COLOR_PANEL = 0x99111111;
    private static final int COLOR_PANEL_DARK = 0xAA070907;
    private static final int COLOR_SLOT = 0x77333333;
    private static final int COLOR_SLOT_HOVER = 0x99444422;
    private static final int COLOR_SLOT_OUTLINE = 0xFFAAAAAA;
    private static final int COLOR_SLOT_ACTIVE = 0xFF47C947;
    private static final int COLOR_LINE = 0xAA55FF55;
    private static final int COLOR_LINE_LOCKED = 0x66666666;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_DIM = 0xFF777777;
    private static final int COLOR_WARNING = 0xFFFFCC55;
    private static final int COLOR_GOOD = 0xFF88FF88;
    private static final int COLOR_BAD = 0xFFFF7777;
    private static final int COLOR_ACCENT = 0xFFFF8A1A;

    // Координаты слотов пока остаются 2D-локальными координатами оружия.
    // Ниже масштаб только для расположения нод/силуэта, не для текста.
    private static final float LAYOUT_SCALE = 0.52F;
    private static final float SOCKET_PREVIEW_PIXELS = 360.0F;

    // Ванильный шрифт Minecraft нельзя заменить на Times New Roman одной строкой кода.
    // Поэтому уменьшаем именно рендер текста через pose().scale(...).
    private static final float SMALL_TEXT_SCALE = 0.56F;
    private static final float TINY_TEXT_SCALE = 0.52F;
    private static final float SLOT_TEXT_SCALE = 0.60F;

    private static final int SLOT_SIZE = 18;
    private static final int MODULE_CARD_WIDTH = 56;
    private static final int MODULE_CARD_HEIGHT = 30;
    private static final int MODULE_CARD_GAP = 4;

    private GunSlotDefinition hoveredSlot;
    private GunSlotDefinition selectedSlot;
    private GunModule hoveredModule;

    // В UI храним все три угла. Пока item() реально отображает GUI-модель предмета,
    // а не полноценный 3D PiP renderer. X/Y уже готовы под следующий renderer.
    private float previewYawDegrees;
    private float previewPitchDegrees;
    private float previewRollDegrees;
    private boolean draggingPreview;

    public GunModificationScreen() {
        super(
                Minecraft.getInstance(),
                Minecraft.getInstance().font,
                Component.literal("Модификация оружия")
        );
    }

    private int scaled(int value) {
        return Math.round(value * LAYOUT_SCALE);
    }

    private int statsPanelWidth() {
        // Фон у стат-блока убран, ширина теперь нужна только для внутренней логики.
        return 76;
    }

    private int statsPanelHeight() {
        return 156;
    }

    private int statsPanelX() {
        return 12;
    }

    private int statsPanelY() {
        return previewPanelY() + Math.max(0, (previewPanelHeight() - statsPanelHeight()) / 2);
    }

    private int previewPanelX() {
        return (this.width - previewPanelWidth()) / 2;
    }

    private int previewPanelY() {
        return 38;
    }

    private int previewPanelWidth() {
        int available = this.width - 260;
        return Math.min(560, Math.max(360, available));
    }

    private int previewPanelHeight() {
        return Math.max(205, this.height - previewPanelY() - 50);
    }

    private int layoutCenterX() {
        return previewPanelX() + previewPanelWidth() / 2;
    }

    private int layoutCenterY() {
        return previewPanelY() + previewPanelHeight() / 2;
    }

    private int moduleStripWidth() {
        return Math.min(390, Math.max(230, previewPanelWidth() - 70));
    }

    private int moduleStripX() {
        return previewPanelX() + (previewPanelWidth() - moduleStripWidth()) / 2;
    }

    private int moduleStripY() {
        return previewPanelY() + previewPanelHeight() - 38;
    }

    private void drawText(GuiGraphicsExtractor graphics, Component component, int x, int y, int color) {
        graphics.text(this.font, component, x, y, color, true);
    }

    private void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawText(graphics, Component.literal(text), x, y, color);
    }

    private void drawSmallText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, SMALL_TEXT_SCALE);
    }

    private void drawTinyText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, TINY_TEXT_SCALE);
    }

    private void drawSlotText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, SLOT_TEXT_SCALE);
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(
                this.font,
                Component.literal(text),
                Math.round(x / scale),
                Math.round(y / scale),
                color,
                true
        );
        graphics.pose().popMatrix();
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String text, int y, int color) {
        drawText(graphics, text, this.width / 2 - this.font.width(text) / 2, y, color);
    }

    private void drawCenteredScaledText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color, float scale) {
        drawScaledText(graphics, text, centerX - scaledTextWidth(text, scale) / 2, y, color, scale);
    }

    private int scaledTextWidth(String text, float scale) {
        return Math.round(this.font.width(text) * scale);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredSlot = null;
        this.hoveredModule = null;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            drawCenteredText(graphics, "Нет игрока", this.height / 2, 0xFFFF5555);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem gunItem)) {
            drawCenteredText(graphics, "В руках нет оружия", this.height / 2, 0xFFFF5555);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        GunDefinition gun = getGunDefinition(stack);
        int centerX = layoutCenterX();
        int centerY = layoutCenterY();

        drawHeader(graphics, gun);
        drawStatsPanel(graphics, gunItem, stack, statsPanelX(), statsPanelY());
        drawWeaponPreview(graphics, gun, stack, centerX, centerY, mouseX, mouseY);

        List<GunSlotDefinition> slots = gun.rootSlots();

        for (GunSlotDefinition slot : slots) {
            drawConnection(graphics, slot, centerX, centerY, stack);
        }

        for (GunSlotDefinition slot : slots) {
            drawSlot(graphics, slot, centerX, centerY, mouseX, mouseY, stack);
        }

        drawModuleStrip(graphics, stack, mouseX, mouseY);
        drawComparePanel(graphics, stack);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, GunDefinition gun) {
        String title = "МОДИФИКАЦИЯ: " + gun.displayName();
        drawCenteredText(graphics, title, 8, COLOR_TEXT);
        drawCenteredScaledText(graphics, "M / Esc — закрыть", this.width / 2, 24, COLOR_MUTED, SMALL_TEXT_SCALE);
    }

    private void drawStatsPanel(GuiGraphicsExtractor graphics, GunItem gunItem, ItemStack stack, int x, int y) {
        // Стат-блок без фона и рамки: только текст.
        GunStats stats = gunItem.getEffectiveStats(stack);
        int capacity = getMagazineCapacityForHud(stack);

        int line = y + 7;
        int step = 8;
        int tx = x;

        drawSmallText(graphics, gunItem.getHudGunName(stack), tx, line, COLOR_GOOD);
        line += step + 3;
        drawSmallText(graphics, "Прочность: 100%", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Вес: тест", tx, line, COLOR_MUTED);

        line += step + 4;
        drawSmallText(graphics, "Магазин: " + gunItem.getHudMagazineText(stack), tx, line, COLOR_TEXT);
        line += step;
        drawSmallText(graphics, "Ёмкость: " + capacity + " патр.", tx, line, COLOR_TEXT);

        line += step + 4;
        drawSmallText(graphics, "Урон: " + formatFloat(stats.damage()) + " ед.", tx, line, COLOR_TEXT);
        line += step;
        drawSmallText(graphics, "Дальность: " + Math.round(stats.range()) + " м", tx, line, COLOR_TEXT);
        line += step;
        drawSmallText(graphics, "Темп стрельбы: " + stats.rpm() + " rpm", tx, line, COLOR_TEXT);
        line += step;
        drawSmallText(graphics, "Перезарядка: " + formatSeconds(GunItem.ticksToSeconds(stats.baseReloadTicks())) + " с", tx, line, COLOR_TEXT);
        line += step;
        drawSmallText(graphics, "Разброс от бедра: " + formatFloat(stats.hipSpreadDegrees()) + "°", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Разброс ADS: " + formatFloat(stats.aimSpreadDegrees()) + "°", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Отдача вверх: " + formatFloat(stats.recoilVertical()) + " ед.", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Отдача вбок: " + formatFloat(stats.recoilHorizontal()) + " ед.", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Скорость пули: " + Math.round(stats.muzzleVelocity()) + " м/с", tx, line, COLOR_MUTED);
        line += step;
        drawSmallText(graphics, "Гравитация: " + formatDouble(stats.gravity()) + " коэф.", tx, line, COLOR_DIM);
        line += step;
        drawSmallText(graphics, "Сопротивление: " + formatDouble(stats.drag()) + " коэф.", tx, line, COLOR_DIM);
    }


    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawWeaponPreview(
            GuiGraphicsExtractor graphics,
            GunDefinition gun,
            ItemStack stack,
            int centerX,
            int centerY,
            int mouseX,
            int mouseY
    ) {
        int panelW = previewPanelWidth();
        int panelH = previewPanelHeight();
        int panelX = previewPanelX();
        int panelY = previewPanelY();

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x10101010);
        drawOutline(graphics, panelX, panelY, panelW, panelH, isInsidePreviewArea(mouseX, mouseY) ? 0x44555555 : 0x22333333);

        // Stage 1K: не PNG. Рисуем настоящий OBJ/Layout через special item renderer в preview-контексте.
        drawRealItemModel(graphics, stack, centerX, centerY);

        drawCenteredScaledText(graphics, gun.displayName() + " / OBJ PREVIEW", centerX, previewPanelY() + 9, 0x55555555, SMALL_TEXT_SCALE);

        String rotationText = "X " + Math.round(previewPitchDegrees)
                + "°   Y " + Math.round(previewYawDegrees)
                + "°   Z " + Math.round(previewRollDegrees) + "°";
        drawTinyText(graphics, rotationText, panelX + 8, panelY + panelH - 11, COLOR_DIM);
    }

    private void drawRealItemModel(GuiGraphicsExtractor graphics, ItemStack stack, int centerX, int centerY) {
        int x = previewModelX();
        int y = previewModelY();
        int w = previewModelWidth();
        int h = previewModelHeight();

        graphics.enableScissor(x, y, x + w, y + h);
        CzoGunPreviewGui.submit(
                graphics,
                stack,
                x,
                y,
                w,
                h,
                previewYawDegrees,
                previewPitchDegrees,
                previewRollDegrees
        );
        graphics.disableScissor();
    }

    private int previewModelX() {
        return previewPanelX() + 2;
    }

    private int previewModelY() {
        return previewPanelY() + 2;
    }

    private int previewModelWidth() {
        return previewPanelWidth() - 4;
    }

    private int previewModelHeight() {
        return previewPanelHeight() - 4;
    }

    private ItemStack getModuleIconStack(GunModule module) {
        if (module == null || !module.hasPhysicalItem()) {
            return ItemStack.EMPTY;
        }

        Identifier itemId = Identifier.parse(module.physicalItemId());

        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        return new ItemStack(item);
    }

    private void drawItemIcon(GuiGraphicsExtractor graphics, ItemStack iconStack, int x, int y, float scale) {
        if (iconStack.isEmpty()) {
            return;
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(iconStack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void fillRotatedRect(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int localX,
            int localY,
            int width,
            int height,
            int color
    ) {
        int step = 3;

        for (int x = localX; x < localX + width; x += step) {
            for (int y = localY; y < localY + height; y += step) {
                ScreenPoint point = rotateLocal(centerX, centerY, x, y);
                graphics.fill(point.x(), point.y(), point.x() + 2, point.y() + 2, color);
            }
        }
    }

    private void drawCenteredLocalText(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int centerY,
            int localX,
            int localY,
            int color
    ) {
        ScreenPoint point = rotateLocal(centerX, centerY, localX, localY);
        drawCenteredScaledText(graphics, text, point.x(), point.y(), color, SMALL_TEXT_SCALE);
    }

    private ScreenPoint rotateLocal(int centerX, int centerY, int localX, int localY) {
        // Псевдо-3D проекция для нод: yaw сжимает X, pitch сжимает Y, roll крутит в плоскости экрана.
        // Полноценная 3D-привязка нод к OBJ-модели пойдёт через отдельный GunPreviewRenderer.
        double roll = Math.toRadians(previewRollDegrees);
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        double yawPerspective = 0.42D + 0.58D * Math.abs(Math.cos(Math.toRadians(previewYawDegrees)));
        double pitchPerspective = 0.58D + 0.42D * Math.abs(Math.cos(Math.toRadians(previewPitchDegrees)));

        double sx = scaled(localX) * yawPerspective;
        double sy = scaled(localY) * pitchPerspective;

        int x = centerX + Math.round((float) (sx * cos - sy * sin));
        int y = centerY + Math.round((float) (sx * sin + sy * cos));

        return new ScreenPoint(x, y);
    }

    private ScreenPoint clampPointToPreview(ScreenPoint point, int elementSize) {
        int padding = 8;
        int minX = previewPanelX() + padding;
        int maxX = previewPanelX() + previewPanelWidth() - padding - elementSize;
        int minY = previewPanelY() + padding;
        int maxY = previewPanelY() + previewPanelHeight() - padding - elementSize;

        return new ScreenPoint(
                clamp(point.x(), minX, maxX),
                clamp(point.y(), minY, maxY)
        );
    }

    private ScreenPoint socketPoint(ItemStack stack, GunSlotDefinition slot, int centerX, int centerY) {
        String visualSlot = visualSlotPath(slot);
        Vector2f projected = CzoGunPreviewGui.projectSlot(
                stack,
                visualSlot,
                previewModelX(),
                previewModelY(),
                previewModelWidth(),
                previewModelHeight(),
                previewYawDegrees,
                previewPitchDegrees,
                previewRollDegrees
        );

        int x = Math.round(projected.x());
        int y = Math.round(projected.y());

        // If the layout has no matching socket yet, the projector returns the model center.
        // In that case we keep the old hand-tuned anchor so unfinished slots remain usable.
        if (x == centerX && y == centerY && !"magazine".equals(visualSlot) && !"cheeks".equals(visualSlot)) {
            return rotateLocal(centerX, centerY, slot.anchorX(), slot.anchorY());
        }

        return new ScreenPoint(x, y);
    }

    private String visualSlotPath(GunSlotDefinition slot) {
        if (slot == null || slot.path() == null) {
            return "";
        }

        return switch (slot.path()) {
            case "grip" -> "cheeks";
            case "muzzle" -> "muzzle";
            case "magazine" -> "magazine";
            case "optic" -> "optic";
            case "charm" -> "charm";
            default -> slot.path();
        };
    }

    private int safeTextX(String text, int preferredX, float scale) {
        return clamp(preferredX, previewPanelX() + 4, Math.max(previewPanelX() + 4, previewPanelX() + previewPanelWidth() - scaledTextWidth(text, scale) - 4));
    }

    private int safeTextY(int preferredY) {
        return clamp(preferredY, previewPanelY() + 4, previewPanelY() + previewPanelHeight() - 12);
    }

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            GunSlotDefinition slot,
            int centerX,
            int centerY,
            int mouseX,
            int mouseY,
            ItemStack stack
    ) {
        ScreenPoint point = clampPointToPreview(rotateLocal(centerX, centerY, slot.x(), slot.y()), SLOT_SIZE);
        int x = point.x();
        int y = point.y();

        boolean hovered = contains(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY);

        if (hovered) {
            hoveredSlot = slot;
        }

        String installedId = getInstalledModuleId(stack, slot.path());
        boolean active = installedId != null && !installedId.equals(CzoGunModules.NONE);
        boolean selected = selectedSlot != null && selectedSlot.path().equals(slot.path());

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? COLOR_SLOT_HOVER : COLOR_SLOT);

        int outlineColor;

        if (hovered || selected) {
            outlineColor = 0xFFFFFF55;
        } else if (active) {
            outlineColor = COLOR_SLOT_ACTIVE;
        } else {
            outlineColor = COLOR_SLOT_OUTLINE;
        }

        drawOutline(graphics, x, y, SLOT_SIZE, SLOT_SIZE, outlineColor);

        if (active) {
            GunModule installedModule = CzoGunModules.getModule(installedId);
            ItemStack iconStack = getModuleIconStack(installedModule);

            if (!iconStack.isEmpty()) {
                drawItemIcon(graphics, iconStack, x + 1, y + 1, 1.0F);
            } else {
                graphics.fill(x, y, x + SLOT_SIZE, y + 5, 0xAA247A24);
                drawSlotText(graphics, "≡", x + 4, y - 1, 0xFFAAFFAA);
            }
        }

        String label = slot.label();
        String moduleLabel = getInstalledModuleLabel(stack, slot.path());
        int labelX = safeTextX(label, x, SLOT_TEXT_SCALE);
        int labelY = safeTextY(y + SLOT_SIZE + 2);
        int moduleX = safeTextX(moduleLabel, x, SLOT_TEXT_SCALE);
        int moduleY = safeTextY(y + SLOT_SIZE + 10);

        drawSlotText(graphics, label, labelX, labelY, COLOR_TEXT);
        drawSlotText(graphics, moduleLabel, moduleX, moduleY, COLOR_MUTED);
    }

    private void drawConnection(
            GuiGraphicsExtractor graphics,
            GunSlotDefinition slot,
            int centerX,
            int centerY,
            ItemStack stack
    ) {
        ScreenPoint from = clampPointToPreview(rotateLocal(centerX, centerY, slot.x(), slot.y()), SLOT_SIZE);
        ScreenPoint to = clampPointToPreview(socketPoint(stack, slot, centerX, centerY), 8);

        int fromX = from.x() + SLOT_SIZE / 2;
        int fromY = from.y() + SLOT_SIZE / 2;
        int toX = to.x();
        int toY = to.y();

        String installedId = getInstalledModuleId(stack, slot.path());
        boolean active = installedId != null && !installedId.equals(CzoGunModules.NONE);

        drawLine(graphics, fromX, fromY, toX, toY, active ? COLOR_LINE : COLOR_LINE_LOCKED);

        graphics.fill(toX - 3, toY - 3, toX + 4, toY + 4, 0xAA225522);
        drawOutline(graphics, toX - 4, toY - 4, 8, 8, active ? COLOR_SLOT_ACTIVE : 0xFF999999);
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            int x = x1 + dx * i / steps;
            int y = y1 + dy * i / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void drawModuleStrip(GuiGraphicsExtractor graphics, ItemStack stack, int mouseX, int mouseY) {
        GunSlotDefinition slot = selectedSlot;

        if (slot == null) {
            return;
        }

        GunModuleSlot moduleSlot = GunModuleSlot.fromId(slot.type()).orElse(null);

        if (moduleSlot == null) {
            return;
        }

        List<GunModule> modules = getVisibleModulesForSlot(moduleSlot, stack);

        if (modules.isEmpty()) {
            return;
        }

        int x = moduleStripX();
        int y = moduleStripY();
        int w = moduleStripWidth();
        int h = 35;

        graphics.fill(x - 5, y - 12, x + w + 5, y + h, COLOR_PANEL_DARK);
        drawOutline(graphics, x - 5, y - 12, w + 10, h + 12, 0x66444444);
        drawCenteredScaledText(graphics, "Слот: " + slot.label(), x + w / 2, y - 9, COLOR_ACCENT, SMALL_TEXT_SCALE);

        int cardX = x;
        String currentModuleId = getInstalledModuleId(stack, slot.path());

        for (GunModule module : modules) {
            if (cardX + MODULE_CARD_WIDTH > x + w) {
                break;
            }

            boolean hovered = contains(cardX, y, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, mouseX, mouseY);
            boolean installed = module.id().equals(currentModuleId);

            if (hovered) {
                hoveredModule = module;
            }

            graphics.fill(cardX, y, cardX + MODULE_CARD_WIDTH, y + MODULE_CARD_HEIGHT, hovered ? 0xAA333322 : 0xAA1A1A1A);
            int cardOutlineColor;

            if (installed) {
                cardOutlineColor = COLOR_SLOT_ACTIVE;
            } else if (hovered) {
                cardOutlineColor = COLOR_WARNING;
            } else {
                cardOutlineColor = 0xFF555555;
            }

            drawOutline(graphics, cardX, y, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, cardOutlineColor);

            ItemStack iconStack = getModuleIconStack(module);
            if (!iconStack.isEmpty()) {
                drawItemIcon(graphics, iconStack, cardX + MODULE_CARD_WIDTH - 18, y + 3, 0.78F);
            }

            drawTinyText(graphics, compactText(module.displayName(), iconStack.isEmpty() ? 12 : 8), cardX + 4, y + 4, installed ? COLOR_GOOD : COLOR_TEXT);

            if (installed) {
                drawTinyText(graphics, "ЛКМ: снять", cardX + 4, y + 16, COLOR_WARNING);
            } else if (module.isMagazine()) {
                drawTinyText(graphics, module.magazineCapacity() + " патр.", cardX + 4, y + 16, COLOR_DIM);
            } else {
                drawTinyText(graphics, compactText(module.slot().displayName(), 10), cardX + 4, y + 16, COLOR_MUTED);
            }

            cardX += MODULE_CARD_WIDTH + MODULE_CARD_GAP;
        }
    }

    private void drawComparePanel(GuiGraphicsExtractor graphics, ItemStack stack) {
        if (hoveredModule == null) {
            return;
        }

        int w = 116;
        int h = 62;
        int x = this.width - w - 6;
        int y = previewPanelY() + 10;

        graphics.fill(x, y, x + w, y + h, COLOR_PANEL_DARK);
        drawOutline(graphics, x, y, w, h, 0x66777777);

        drawSmallText(graphics, "Сравнение", x + 5, y + 5, COLOR_ACCENT);
        drawTinyText(graphics, compactText(hoveredModule.displayName(), 18), x + 5, y + 17, COLOR_TEXT);

        if (hoveredModule.isMagazine()) {
            GunModule nextModule = getPreviewTargetModule(stack, hoveredModule);
            int currentCapacity = getMagazineCapacityForHud(stack);
            int nextCapacity = nextModule.magazineCapacity();
            double currentReload = GunItem.ticksToSeconds(getMagazineReloadForHud(stack));
            double nextReload = GunItem.ticksToSeconds(nextModule.reloadTicks());

            drawStatCompare(graphics, x + 5, y + 34, "Ёмкость", currentCapacity, nextCapacity, true);
            drawStatCompareSeconds(graphics, x + 5, y + 45, "Перезар.", currentReload, nextReload, false);
        } else {
            drawTinyText(graphics, "Статы модуля", x + 5, y + 34, COLOR_MUTED);
            drawTinyText(graphics, "позже", x + 5, y + 45, COLOR_DIM);
        }
    }

    private void drawStatCompare(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String label,
            int currentValue,
            int nextValue,
            boolean biggerIsBetter
    ) {
        int color;

        if (nextValue == currentValue) {
            color = COLOR_MUTED;
        } else if ((nextValue > currentValue) == biggerIsBetter) {
            color = COLOR_GOOD;
        } else {
            color = COLOR_BAD;
        }

        drawTinyText(graphics, label + ": " + currentValue + " → " + nextValue, x, y, color);
    }

    private void drawStatCompareSeconds(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String label,
            double currentValue,
            double nextValue,
            boolean biggerIsBetter
    ) {
        int color;

        if (Math.abs(nextValue - currentValue) < 0.001D) {
            color = COLOR_MUTED;
        } else if ((nextValue > currentValue) == biggerIsBetter) {
            color = COLOR_GOOD;
        } else {
            color = COLOR_BAD;
        }

        drawTinyText(graphics, label + ": " + formatSeconds(currentValue) + " с → " + formatSeconds(nextValue) + " с", x, y, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return super.mouseClicked(event, doubleClick);
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem)) {
            return super.mouseClicked(event, doubleClick);
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (tryClickModuleStrip(stack, mouseX, mouseY)) {
            return true;
        }

        GunDefinition gun = getGunDefinition(stack);
        int centerX = layoutCenterX();
        int centerY = layoutCenterY();

        for (GunSlotDefinition slot : gun.rootSlots()) {
            ScreenPoint point = clampPointToPreview(rotateLocal(centerX, centerY, slot.x(), slot.y()), SLOT_SIZE);
            int x = point.x();
            int y = point.y();

            if (contains(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
                if (selectedSlot != null && selectedSlot.path().equals(slot.path())) {
                    selectedSlot = null;
                } else {
                    selectedSlot = slot;
                }

                return true;
            }
        }

        if (isInsidePreviewArea(mouseX, mouseY)) {
            draggingPreview = true;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingPreview && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            previewYawDegrees += (float) dragX * 0.95F;
            previewPitchDegrees = clampFloat(previewPitchDegrees + (float) dragY * 0.75F, -85.0F, 85.0F);
            previewRollDegrees += (float) (dragX * 0.12D + dragY * 0.05D);
            normalizePreviewRotations();
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPreview) {
            draggingPreview = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    private boolean tryClickModuleStrip(ItemStack stack, int mouseX, int mouseY) {
        GunSlotDefinition slot = getActiveUiSlot();

        if (slot == null) {
            return false;
        }

        GunModuleSlot moduleSlot = GunModuleSlot.fromId(slot.type()).orElse(null);

        if (moduleSlot == null) {
            return false;
        }

        List<GunModule> modules = getVisibleModulesForSlot(moduleSlot, stack);

        if (modules.isEmpty()) {
            return false;
        }

        int x = moduleStripX();
        int y = moduleStripY();
        int w = moduleStripWidth();
        int cardX = x;

        for (GunModule module : modules) {
            if (cardX + MODULE_CARD_WIDTH > x + w) {
                break;
            }

            if (contains(cardX, y, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, mouseX, mouseY)) {
                String currentModuleId = getInstalledModuleId(stack, slot.path());
                String nextModuleId = module.id().equals(currentModuleId) ? CzoGunModules.NONE : module.id();
                ClientPacketDistributor.sendToServer(new ServerboundSetGunModulePacket(slot.path(), nextModuleId));
                return true;
            }

            cardX += MODULE_CARD_WIDTH + MODULE_CARD_GAP;
        }

        return false;
    }

    private List<GunModule> getVisibleModulesForSlot(GunModuleSlot moduleSlot, ItemStack weaponStack) {
        List<GunModule> result = new ArrayList<>();

        String currentModuleId = selectedSlot == null
                ? CzoGunModules.NONE
                : getInstalledModuleId(weaponStack, selectedSlot.path());

        for (GunModule module : CzoGunModules.getInstallableModulesForSlot(moduleSlot)) {
            if (module.id().equals(currentModuleId) || hasPhysicalModuleInInventory(module)) {
                result.add(module);
            }
        }

        return result;
    }

    private boolean hasPhysicalModuleInInventory(GunModule module) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || !module.hasPhysicalItem()) {
            return false;
        }

        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (itemId != null && itemId.toString().equals(module.physicalItemId())) {
                return true;
            }
        }

        return false;
    }

    private GunModule getPreviewTargetModule(ItemStack stack, GunModule hovered) {
        if (selectedSlot != null && hovered.id().equals(getInstalledModuleId(stack, selectedSlot.path()))) {
            return CzoGunModules.getMagazine(CzoGunModules.getDefaultModuleIdForSlot(GunModuleSlot.MAGAZINE));
        }

        return hovered;
    }

    private GunSlotDefinition getActiveUiSlot() {
        return selectedSlot;
    }

    private boolean isInsidePreviewArea(int mouseX, int mouseY) {
        return contains(previewPanelX(), previewPanelY(), previewPanelWidth(), previewPanelHeight(), mouseX, mouseY);
    }

    private void normalizePreviewRotations() {
        previewYawDegrees = normalize360(previewYawDegrees);
        previewRollDegrees = normalize360(previewRollDegrees);
    }

    private float normalize360(float value) {
        while (value >= 360.0F) {
            value -= 360.0F;
        }

        while (value < 0.0F) {
            value += 360.0F;
        }

        return value;
    }

    private GunDefinition getGunDefinition(ItemStack stack) {
        String gunId = stack.get(CzoDataComponents.GUN_ID.get());

        if (gunId == null || gunId.isBlank()) {
            return CzoGuns.get(CzoGuns.PM);
        }

        return CzoGuns.get(gunId);
    }

    private int getMagazineCapacityForHud(ItemStack stack) {
        return getMagazineForHud(stack).magazineCapacity();
    }

    private int getMagazineReloadForHud(ItemStack stack) {
        return getMagazineForHud(stack).reloadTicks();
    }

    private GunModule getMagazineForHud(ItemStack stack) {
        String magazineId = stack.get(CzoDataComponents.MAGAZINE_ID.get());

        if (magazineId == null || magazineId.isBlank()) {
            return CzoGunModules.getMagazine(CzoGunModules.PM_MAG_8);
        }

        return CzoGunModules.getMagazine(magazineId);
    }

    private String getInstalledModuleId(ItemStack stack, String slotPath) {
        String moduleId = switch (slotPath) {
            case "magazine" -> stack.get(CzoDataComponents.MAGAZINE_ID.get());
            case "muzzle" -> stack.get(CzoDataComponents.MUZZLE_ID.get());
            case "optic" -> stack.get(CzoDataComponents.OPTIC_ID.get());
            case "stock" -> stack.get(CzoDataComponents.STOCK_ID.get());
            case "grip" -> stack.get(CzoDataComponents.GRIP_ID.get());
            case "barrel" -> stack.get(CzoDataComponents.BARREL_ID.get());
            case "handguard" -> stack.get(CzoDataComponents.HANDGUARD_ID.get());
            case "charm" -> stack.get(CzoDataComponents.CHARM_ID.get());
            case "laser" -> stack.get(CzoDataComponents.LASER_ID.get());
            default -> CzoGunModules.NONE;
        };

        if (moduleId == null || moduleId.isBlank() || CzoGunModules.NONE.equals(moduleId)) {
            GunModuleSlot slot = GunModuleSlot.fromId(slotPath).orElse(null);
            return slot == null ? CzoGunModules.NONE : CzoGunModules.getDefaultModuleIdForSlot(slot);
        }

        return moduleId;
    }

    private String getInstalledModuleLabel(ItemStack stack, String slotPath) {
        String moduleId = getInstalledModuleId(stack, slotPath);

        if (moduleId == null || moduleId.isBlank() || CzoGunModules.NONE.equals(moduleId)) {
            return "Пусто";
        }

        GunModule module = CzoGunModules.getModule(moduleId);

        if (module == null) {
            return "???";
        }

        return module.displayName();
    }

    private String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F) {
            return Integer.toString(Math.round(value));
        }

        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String formatDouble(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001D) {
            return Long.toString(Math.round(value));
        }

        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private String formatSeconds(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String compactText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private boolean contains(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_M) {
            this.onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_Q) {
            previewRollDegrees -= 8.0F;
            normalizePreviewRotations();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_E) {
            previewRollDegrees += 8.0F;
            normalizePreviewRotations();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_W) {
            previewPitchDegrees = clampFloat(previewPitchDegrees - 6.0F, -85.0F, 85.0F);
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_S) {
            previewPitchDegrees = clampFloat(previewPitchDegrees + 6.0F, -85.0F, 85.0F);
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_A) {
            previewYawDegrees -= 8.0F;
            normalizePreviewRotations();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_D) {
            previewYawDegrees += 8.0F;
            normalizePreviewRotations();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ScreenPoint(int x, int y) {
    }
}
