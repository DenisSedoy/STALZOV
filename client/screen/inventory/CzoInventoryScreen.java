package com.czo.client.screen.inventory;

import com.czo.CZO;
import com.czo.inventory.CzoInventoryBootstrap;
import com.czo.faction.CzoFractions;
import com.czo.inventory.equipment.CzoEquipmentSlot;
import com.czo.inventory.equipment.CzoEquipmentSlotStorage;
import com.czo.inventory.grid.GridEntry;
import com.czo.inventory.grid.GridInventory;
import com.czo.inventory.grid.GridInventorySlots;
import com.czo.inventory.grid.ItemSize;
import com.czo.inventory.grid.CzoStackCounts;
import com.czo.item.gun.GunItem;
import com.czo.item.gun.definition.GunStats;
import com.czo.network.ServerboundDropBackpackStackPacket;
import com.czo.network.ServerboundEquipFromInventoryPacket;
import com.czo.network.ServerboundQuickEquipFromInventoryPacket;
import com.czo.network.ServerboundSetInventoryGridPositionPacket;
import com.czo.network.ServerboundSortInventoryGridPacket;
import com.czo.network.ServerboundUnequipToBackpackPacket;
import com.czo.network.ServerboundUnequipToGridPositionPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.czo.client.screen.customization.CzoAppearanceScreen;

/**
 * Первый рабочий экран нового CZO HUD-инвентаря.
 *
 * <p>Это клиентский визуальный слой поверх текущего vanilla-инвентаря игрока:
 * слева рисуется EFT/STALKER-блок экипировки, справа — grid-рюкзак с размерами предметов.
 * Stage 5 добавляет точное снятие экипировки в выбранную клетку, ПКМ-быстрое экипирование и локальный drag из слотов.</p>
 */
public class CzoInventoryScreen extends Screen {
    private static final int BACKGROUND = 0xEE050706;
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
    private static final int BAD = 0xFFE07070;
    private static final int TAB_ACTIVE = 0xFF2D3B2D;
    private static final int TAB_IDLE = 0xAA151A15;
    private static final int TOP_BAR_H = 36;

    private static final int GRID_COLUMNS = GridInventorySlots.GRID_COLUMNS;
    private static final int GRID_ROWS = GridInventorySlots.GRID_ROWS;
    private static final int CELL = 27;
    private static final int EQUIPMENT_SLOT = 34;
    private static final int SMALL_SLOT = 26;

    private static final Identifier FACTION_ISKATELI_TEXTURE = Identifier.fromNamespaceAndPath(CZO.MODID, "textures/gui/factions/pathfinders.png");
    private static final Identifier FACTION_KONTRABANDISTY_TEXTURE = Identifier.fromNamespaceAndPath(CZO.MODID, "textures/gui/factions/contrabandists.png");

    private Tab currentTab = Tab.INVENTORY;
    private int scrollRows;
    private int statsScrollPixels;
    private int artifactScrollPixels;
    private boolean appearanceNotice;
    private int appearanceButtonX = -1;
    private int appearanceButtonY = -1;
    private int appearanceButtonW;
    private int appearanceButtonH;

    private GridEntry hoveredGridEntry;
    private CzoEquipmentSlot hoveredEquipmentSlot;
    private int hoveredVanillaSlot = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private DraggedGridItem draggedItem;
    private DraggedEquipmentItem draggedEquipmentItem;
    private String noticeText = "";
    private int noticeTicks;

    public CzoInventoryScreen() {
        super(
                Minecraft.getInstance(),
                Minecraft.getInstance().font,
                Component.literal("CZO Inventory")
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND);
        graphics.fillGradient(0, 0, this.width, this.height, 0xAA0A0E0A, 0xDD020302);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredGridEntry = null;
        hoveredEquipmentSlot = null;
        hoveredVanillaSlot = -1;
        hoveredStack = ItemStack.EMPTY;
        if (noticeTicks > 0) {
            noticeTicks--;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            drawCenteredText(graphics, "Нет игрока", this.height / 2, BAD);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        drawWindow(graphics, mouseX, mouseY, player, partialTick);
        drawDraggedPreview(graphics, mouseX, mouseY);
        drawNotice(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!hoveredStack.isEmpty() && draggedItem == null && draggedEquipmentItem == null) {
            graphics.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private void drawWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, LocalPlayer player, float partialTick) {
        int top = 18;
        int bottom = this.height - 18;
        int left = Math.max(10, (this.width - windowWidth()) / 2);
        int w = Math.min(windowWidth(), this.width - 20);
        int h = bottom - top;

        graphics.fill(left, top, left + w, bottom, 0xAA090B09);
        drawOutline(graphics, left, top, w, h, 0xFF222822);

        drawTopBar(graphics, left, top, w, mouseX, mouseY, player);

        if (currentTab == Tab.INVENTORY) {
            drawInventoryTab(graphics, left, top + TOP_BAR_H, w, h - TOP_BAR_H, mouseX, mouseY, player);
        } else {
            drawStatsTab(graphics, left, top + TOP_BAR_H, w, h - TOP_BAR_H, mouseX, mouseY, player, partialTick);
        }

        // Кнопка CZO в правом нижнем углу удалена: модовый инвентарь теперь основной.
    }

    private int windowWidth() {
        return Math.min(900, Math.max(620, this.width - 30));
    }

    private void drawTopBar(GuiGraphicsExtractor graphics, int left, int top, int width, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(left, top, left + width, top + TOP_BAR_H, 0xCC0D100D);
        drawOutline(graphics, left, top, width, TOP_BAR_H, 0xFF242A24);

        int tabY = top + 6;
        int tabW = Math.min(160, Math.max(120, width / 5));
        drawTab(graphics, left + 9, tabY, tabW, 22, "ИНВЕНТАРЬ", currentTab == Tab.INVENTORY, mouseX, mouseY);
        drawTab(graphics, left + 14 + tabW, tabY, tabW, 22, "СТАТИСТИКА", currentTab == Tab.STATS, mouseX, mouseY);

        int profileX = left + 24 + tabW * 2;
        int profileW = width - (profileX - left) - 9;
        if (profileW > 150) {
            graphics.fill(profileX, top + 3, profileX + profileW, top + TOP_BAR_H - 3, 0x88101510);
            drawOutline(graphics, profileX, top + 3, profileW, TOP_BAR_H - 6, OUTLINE);

            drawFactionTexture(graphics, player, profileX + 9, top + 4, 28);
            drawSmallText(graphics, player.getName().getString(), profileX + 45, top + 7, TEXT);
            drawTinyText(graphics, CzoFractions.hudSubtitle(player), profileX + 45, top + 20, TEXT_MUTED);

            int currencyX = profileX + profileW - 68;
            drawCurrencyBox(graphics, currencyX, top + 6, "₽", true);
            drawCurrencyBox(graphics, currencyX + 18, top + 6, "", false);
            drawCurrencyBox(graphics, currencyX + 36, top + 6, "", false);
            drawSmallText(graphics, "0", currencyX + 2, top + 21, WARNING);
        }
    }

    private void drawCurrencyBox(GuiGraphicsExtractor graphics, int x, int y, String label, boolean active) {
        int bg = active ? 0xAA2D3B2D : 0x66151A15;
        int outline = active ? WARNING : OUTLINE;
        graphics.fill(x, y, x + 15, y + 12, bg);
        drawOutline(graphics, x, y, 15, 12, outline);
        if (label != null && !label.isBlank()) {
            drawCenteredScaledText(graphics, label, x + 7, y + 2, active ? WARNING : TEXT_MUTED, 0.62F);
        }
    }

    private void drawFactionTexture(GuiGraphicsExtractor graphics, LocalPlayer player, int x, int y, int size) {
        Identifier texture = CzoFractions.isContrabandist(player) ? FACTION_KONTRABANDISTY_TEXTURE : FACTION_ISKATELI_TEXTURE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, 128, 128, 128, 128);
    }

    private void drawTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        int backgroundColor;
        if (active) {
            backgroundColor = TAB_ACTIVE;
        } else if (hovered) {
            backgroundColor = 0xAA202820;
        } else {
            backgroundColor = TAB_IDLE;
        }

        int outlineColor = active ? WARNING : OUTLINE;
        int textColor = active ? TEXT : TEXT_MUTED;
        graphics.fill(x, y, x + w, y + h, backgroundColor);
        drawOutline(graphics, x, y, w, h, outlineColor);
        drawCenteredScaledText(graphics, label, x + w / 2, y + 5, textColor, 0.62F);
    }

    private void drawInventoryTab(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY, LocalPlayer player) {
        int gap = 12;
        int equipmentX = left + 9;
        int minBackpackW = GRID_COLUMNS * CELL + 42;
        int equipmentW = Math.min(315, Math.max(250, width - minBackpackW - gap - 18));
        int backpackW = width - equipmentW - gap - 18;
        if (backpackW < minBackpackW) {
            int deficit = minBackpackW - backpackW;
            equipmentW = Math.max(235, equipmentW - deficit);
            backpackW = width - equipmentW - gap - 18;
        }
        int backpackX = equipmentX + equipmentW + gap;
        int panelH = height - 10;

        drawEquipmentPanel(graphics, equipmentX, top + 5, equipmentW, panelH, mouseX, mouseY, player);
        drawBackpackPanel(graphics, backpackX, top + 5, backpackW, panelH, mouseX, mouseY, player);
    }

    private void drawEquipmentPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        drawOutline(graphics, x, y, w, h, OUTLINE);

        Map<CzoEquipmentSlot, ItemStack> stacks = equipmentSnapshot(player);

        int appearanceH = 22;
        int appearanceY = y + h - appearanceH - 10;

        // Stage 7N: weapon columns are wider but lower. This keeps long weapons readable
        // without letting the side slots dominate the whole equipment panel.
        int baseSideW = Math.max(52, Math.min(62, w / 6));
        int sideW = Math.max(42, Math.round(baseSideW * 0.90F));
        int sideY = y + 9;
        int baseSideH = Math.max(128, Math.min(168, appearanceY - y - 120));
        int sideH = Math.max(86, Math.round(baseSideH * 0.72F));
        int leftSideX = x + 12;
        int rightSideX = x + w - 12 - sideW;

        int centerLeft = leftSideX + sideW + 22;
        int centerRight = rightSideX - 22;
        int centerW = Math.max(130, centerRight - centerLeft);

        // Armour/protection slots are 5% bigger, but their visual centres stay in place.
        int baseGearW = Math.max(28, Math.min(32, (centerW - 14) / 3));
        int baseGearH = Math.max(27, Math.min(31, baseGearW));
        int gearW = Math.max(baseGearW + 1, Math.round(baseGearW * 1.05F));
        int gearH = Math.max(baseGearH + 1, Math.round(baseGearH * 1.05F));
        int baseGearGap = Math.max(5, Math.min(7, (centerW - baseGearW * 3) / 4));
        int baseGearGroupW = baseGearW * 3 + baseGearGap * 2;
        int baseCol1 = x + w / 2 - baseGearGroupW / 2;
        int baseCol2 = baseCol1 + baseGearW + baseGearGap;
        int baseCol3 = baseCol2 + baseGearW + baseGearGap;
        int col1 = baseCol1 + baseGearW / 2 - gearW / 2;
        int col2 = baseCol2 + baseGearW / 2 - gearW / 2;
        int col3 = baseCol3 + baseGearW / 2 - gearW / 2;

        int baseTopRowY = y + 18;
        int topRowY = baseTopRowY + baseGearH / 2 - gearH / 2;
        int baseMidRowY = baseTopRowY + baseGearH + 17;
        int midRowY = baseMidRowY + baseGearH / 2 - gearH / 2;

        // Pouches, holster and scabbard are 5% smaller, also keeping their centres.
        int baseBottomSlot = Math.max(34, Math.min(38, sideW - 6));
        int basePouchW = Math.max(32, Math.min(36, gearW + 4));
        int basePouchH = Math.max(31, Math.min(36, baseBottomSlot));
        int bottomSlot = Math.max(28, Math.round(baseBottomSlot * 0.95F));
        int pouchW = Math.max(28, Math.round(basePouchW * 0.95F));
        int pouchH = Math.max(28, Math.round(basePouchH * 0.95F));
        int baseBottomY = Math.max(midRowY + gearH + 32, appearanceY - basePouchH - 11);
        baseBottomY = Math.min(baseBottomY, appearanceY - basePouchH - 8);
        int bottomY = baseBottomY + basePouchH / 2 - pouchH / 2;

        int baseBottomLeftX = leftSideX;
        int baseBottomRightX = rightSideX + sideW - baseBottomSlot;
        int bottomLeftX = baseBottomLeftX + baseBottomSlot / 2 - bottomSlot / 2;
        int bottomRightX = baseBottomRightX + baseBottomSlot / 2 - bottomSlot / 2;

        int pouchGap = 8;
        int baseThreePouchTotalW = basePouchW * 3 + pouchGap * 2;
        int basePouchesX = x + w / 2 - baseThreePouchTotalW / 2;
        int pouch1X = basePouchesX + basePouchW / 2 - pouchW / 2;
        int pouch2X = basePouchesX + basePouchW + pouchGap + basePouchW / 2 - pouchW / 2;
        int pouch3X = basePouchesX + (basePouchW + pouchGap) * 2 + basePouchW / 2 - pouchW / 2;

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.PRIMARY_WEAPON_1,
                leftSideX, sideY, sideW, sideH, "В РУКАХ",
                stacks.get(CzoEquipmentSlot.PRIMARY_WEAPON_1), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.PRIMARY_WEAPON_2,
                rightSideX, sideY, sideW, sideH, "НА РЕМНЕ",
                stacks.get(CzoEquipmentSlot.PRIMARY_WEAPON_2), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.GAS_MASK,
                col1, topRowY, gearW, gearH, "ЛИЦО",
                stacks.get(CzoEquipmentSlot.GAS_MASK), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.HELMET,
                col2, topRowY, gearW, gearH, "ГОЛОВА",
                stacks.get(CzoEquipmentSlot.HELMET), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.EXTRA_PROTECTION,
                col3, topRowY, gearW, gearH, "КОНЕЧНОСТИ",
                stacks.get(CzoEquipmentSlot.EXTRA_PROTECTION), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.BACKPACK,
                col1, midRowY, gearW, gearH, "СПИНА",
                stacks.get(CzoEquipmentSlot.BACKPACK), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.BODY_ARMOR,
                col2, midRowY, gearW, gearH, "ТЕЛО",
                stacks.get(CzoEquipmentSlot.BODY_ARMOR), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.ARTIFACT_CONTAINER,
                col3, midRowY, gearW, gearH, "ПОЯС",
                stacks.get(CzoEquipmentSlot.ARTIFACT_CONTAINER), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.MELEE_WEAPON,
                bottomLeftX, bottomY, bottomSlot, pouchH, "НОЖНЫ",
                stacks.get(CzoEquipmentSlot.MELEE_WEAPON), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.BOLT,
                pouch1X, bottomY, pouchW, pouchH, "ПОДСУМОК",
                stacks.get(CzoEquipmentSlot.BOLT), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.THROWABLE,
                pouch2X, bottomY, pouchW, pouchH, "ПОДСУМОК",
                stacks.get(CzoEquipmentSlot.THROWABLE), mouseX, mouseY);
        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.EXTRA_POUCH,
                pouch3X, bottomY, pouchW, pouchH, "ПОДСУМОК",
                stacks.get(CzoEquipmentSlot.EXTRA_POUCH), mouseX, mouseY);

        drawEquipmentSlotRect(graphics, CzoEquipmentSlot.SECONDARY_WEAPON,
                bottomRightX, bottomY, bottomSlot, pouchH, "КОБУРА",
                stacks.get(CzoEquipmentSlot.SECONDARY_WEAPON), mouseX, mouseY);

        this.appearanceButtonX = x + 8;
        this.appearanceButtonY = appearanceY;
        this.appearanceButtonW = w - 16;
        this.appearanceButtonH = appearanceH;
        boolean appearanceHovered = contains(this.appearanceButtonX, this.appearanceButtonY, this.appearanceButtonW, this.appearanceButtonH, mouseX, mouseY);
        graphics.fill(this.appearanceButtonX, this.appearanceButtonY, this.appearanceButtonX + this.appearanceButtonW, this.appearanceButtonY + this.appearanceButtonH, appearanceHovered ? 0xAA263126 : PANEL_DARK);
        drawOutline(graphics, this.appearanceButtonX, this.appearanceButtonY, this.appearanceButtonW, this.appearanceButtonH, appearanceHovered ? WARNING : OUTLINE);
        drawCenteredScaledText(graphics, "ВНЕШНИЙ ВИД", x + w / 2, appearanceY + 7, appearanceHovered ? WARNING : TEXT_MUTED, 0.65F);
    }


    private Map<CzoEquipmentSlot, ItemStack> equipmentSnapshot(LocalPlayer player) {
        EnumMap<CzoEquipmentSlot, ItemStack> result = new EnumMap<>(CzoEquipmentSlot.class);
        for (CzoEquipmentSlot slot : CzoEquipmentSlot.values()) {
            result.put(slot, CzoEquipmentSlotStorage.get(player, slot));
        }
        return result;
    }

    private void drawSilhouette(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int color = 0x553B433B;
        int line = 0x88434C43;
        graphics.fill(x + w / 2 - 10, y, x + w / 2 + 10, y + 18, color);
        graphics.fill(x + w / 2 - 15, y + 20, x + w / 2 + 15, y + 78, color);
        graphics.fill(x + w / 2 - 28, y + 25, x + w / 2 - 17, y + 92, color);
        graphics.fill(x + w / 2 + 17, y + 25, x + w / 2 + 28, y + 92, color);
        graphics.fill(x + w / 2 - 14, y + 78, x + w / 2 - 2, y + h, color);
        graphics.fill(x + w / 2 + 2, y + 78, x + w / 2 + 14, y + h, color);
        drawOutline(graphics, x + w / 2 - 16, y + 19, 32, 61, line);
    }

    private void drawEquipmentSlot(GuiGraphicsExtractor graphics, CzoEquipmentSlot slot, int x, int y, int size, String key, String label, ItemStack stack, int mouseX, int mouseY) {
        drawEquipmentSlotRect(graphics, slot, x, y, size, size, label, stack, mouseX, mouseY);
    }

    private void drawEquipmentSlotRect(GuiGraphicsExtractor graphics, CzoEquipmentSlot slot, int x, int y, int w, int h, String label, ItemStack stack, int mouseX, int mouseY) {
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        boolean acceptsDragged = draggedItem != null && slot.canAccept(draggedItem.stack());
        boolean sourceDragged = draggedEquipmentItem != null && draggedEquipmentItem.slot() == slot;
        int backgroundColor = 0x88333333;
        int outlineColor = OUTLINE;
        if (hovered) {
            backgroundColor = 0xAA343B2A;
            outlineColor = OUTLINE_HOVER;
        } else if (acceptsDragged) {
            backgroundColor = 0x66394A2D;
            outlineColor = GOOD;
        } else if (sourceDragged) {
            backgroundColor = 0x66504926;
            outlineColor = WARNING;
        }

        drawBeveledSlot(graphics, x, y, w, h, backgroundColor, outlineColor);
        if (hovered) {
            hoveredEquipmentSlot = slot;
        }

        if (stack != null && !stack.isEmpty()) {
            int itemX = x + Math.max(1, (w - 16) / 2);
            int itemY = y + Math.max(1, (h - 16) / 2);
            graphics.item(stack, itemX, itemY);
            graphics.itemDecorations(this.font, stack, itemX, itemY);
            if (hovered) {
                hoveredStack = stack;
            }
        }

        drawCenteredScaledText(graphics, label, x + w / 2, y + h - 11, stack != null && !stack.isEmpty() ? TEXT_MUTED : TEXT_DIM, 0.40F);
    }

    private void drawReservedEquipmentSlot(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label) {
        drawBeveledSlot(graphics, x, y, w, h, 0x55333333, 0x66495149);
        drawCenteredScaledText(graphics, label, x + w / 2, y + h - 11, TEXT_DIM, 0.40F);
        drawCenteredScaledText(graphics, "+", x + w / 2, y + 12, TEXT_DIM, 0.55F);
    }

    private void drawBeveledSlot(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int backgroundColor, int outlineColor) {
        graphics.fill(x + 2, y, x + w - 2, y + h, backgroundColor);
        graphics.fill(x, y + 3, x + w, y + h, backgroundColor);
        drawOutline(graphics, x, y + 3, w, h - 3, outlineColor);
        graphics.fill(x + 2, y, x + w - 2, y + 1, outlineColor);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x664A514A);
        graphics.fill(x + 2, y + 2, x + w - 2, y + 3, 0x33272E27);
    }


    private void drawBackpackPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        drawOutline(graphics, x, y, w, h, OUTLINE);

        drawSmallText(graphics, "РЮКЗАК", x + 9, y + 7, TEXT);
        drawTinyText(graphics, "drag grid / scroll / размеры предметов", x + 9, y + 18, TEXT_MUTED);

        BackpackSnapshot snapshot = buildBackpackSnapshot(player);
        int usedKg = estimateWeightKg(player);
        drawTinyText(graphics, usedKg + "/60 кг", x + w - 55, y + 9, usedKg > 50 ? WARNING : TEXT_MUTED);

        int gridX = x + 10;
        int gridY = y + 35;
        int scrollbarX = x + w - 10;
        int gridW = Math.min(GRID_COLUMNS * CELL, Math.max(CELL, scrollbarX - gridX - 7));
        gridW = (gridW / CELL) * CELL;
        int visibleRows = Math.max(3, Math.min(GRID_ROWS, (h - 62) / CELL));
        int gridH = visibleRows * CELL;
        int maxScroll = Math.max(0, GRID_ROWS - visibleRows);
        scrollRows = clamp(scrollRows, 0, maxScroll);

        drawGridBackground(graphics, gridX, gridY, gridW, gridH, visibleRows);

        graphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);
        int contentShift = scrollRows * CELL;
        for (GridEntry entry : snapshot.inventory.entries()) {
            drawGridEntry(graphics, entry, gridX, gridY - contentShift, mouseX, mouseY, snapshot.slotByEntryId.get(entry.id()));
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(graphics, scrollbarX, gridY, 6, gridH, scrollRows, maxScroll, visibleRows);
        }

        int footerY = y + h - 18;
        drawTinyText(graphics, "Ячеек: " + snapshot.inventory.usedCells() + "/" + snapshot.inventory.totalCells(), x + 10, footerY, TEXT_MUTED);

        Rect sortButton = autoSortButtonRect(x, y, w, h);
        boolean sortHovered = contains(sortButton.x(), sortButton.y(), sortButton.w(), sortButton.h(), mouseX, mouseY);
        graphics.fill(sortButton.x(), sortButton.y(), sortButton.x() + sortButton.w(), sortButton.y() + sortButton.h(), sortHovered ? 0xAA343B2A : PANEL_DARK);
        drawOutline(graphics, sortButton.x(), sortButton.y(), sortButton.w(), sortButton.h(), sortHovered ? OUTLINE_HOVER : OUTLINE);
        drawCenteredScaledText(graphics, "АВТО", sortButton.x() + sortButton.w() / 2, sortButton.y() + 4, sortHovered ? WARNING : TEXT_MUTED, 0.56F);

        if (draggedItem != null) {
            drawTinyText(graphics, "ЛКМ по клетке — положить, по слоту экипировки — надеть, V/R — повернуть", x + 10, footerY - 11, WARNING);
        } else if (draggedEquipmentItem != null) {
            drawTinyText(graphics, "ЛКМ по клетке — снять сюда, V/R — повернуть, Esc — отмена", x + 10, footerY - 11, WARNING);
        } else {
            drawTinyText(graphics, "ЛКМ — взять, ПКМ — быстро надеть/снять, G — выбросить весь стак", x + 10, footerY - 11, TEXT_DIM);
        }
    }

    private BackpackSnapshot buildBackpackSnapshot(LocalPlayer player) {
        Map<UUID, Integer> slotByEntryId = new HashMap<>();
        GridInventory grid = GridInventorySlots.buildPlayerBackpack(
                player.getInventory(),
                CzoInventoryBootstrap.ITEM_SIZES,
                slotByEntryId
        );
        return new BackpackSnapshot(grid, slotByEntryId);
    }

    private void drawGridBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int visibleRows) {
        graphics.fill(x, y, x + w, y + h, PANEL_DARK);
        drawOutline(graphics, x, y, w, h, OUTLINE);

        int visibleCols = Math.max(1, Math.min(GRID_COLUMNS, w / CELL));
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < visibleCols; col++) {
                int cx = x + col * CELL;
                int cy = y + row * CELL;
                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x44111111);
                drawOutline(graphics, cx, cy, CELL, CELL, 0x332F352F);
            }
        }
    }

    private void drawGridEntry(GuiGraphicsExtractor graphics, GridEntry entry, int gridX, int gridY, int mouseX, int mouseY, Integer vanillaSlot) {
        int x = gridX + entry.rect().x() * CELL;
        int y = gridY + entry.rect().y() * CELL;
        int w = entry.rect().width() * CELL;
        int h = entry.rect().height() * CELL;
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        ItemStack stack = entry.stack();

        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xBB3A432E : 0xAA232823);
        drawOutline(graphics, x, y, w, h, hovered ? OUTLINE_HOVER : 0xFF566056);

        int iconX = x + Math.max(1, (w - 16) / 2);
        int iconY = y + Math.max(1, (h - 16) / 2);
        graphics.item(stack, iconX, iconY);
        graphics.itemDecorations(this.font, stack, iconX, iconY, CzoStackCounts.countOverlay(stack));

        String sizeText = entry.rect().width() + "×" + entry.rect().height();
        drawTinyText(graphics, sizeText, x + 3, y + 3, TEXT_MUTED);
        if (vanillaSlot != null) {
            drawTinyText(graphics, "#" + vanillaSlot, x + 3, y + h - 8, TEXT_DIM);
        }

        if (hovered) {
            hoveredGridEntry = entry;
            hoveredVanillaSlot = vanillaSlot == null ? -1 : vanillaSlot;
            hoveredStack = stack;
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int scroll, int maxScroll, int visibleRows) {
        graphics.fill(x, y, x + w, y + h, 0x66000000);
        int thumbH = Math.max(16, h * visibleRows / GRID_ROWS);
        int thumbY = y + (h - thumbH) * scroll / Math.max(1, maxScroll);
        graphics.fill(x + 1, thumbY, x + w - 1, thumbY + thumbH, 0xAA9B9B9B);
    }


    private void drawDraggedPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack previewStack = ItemStack.EMPTY;
        boolean rotated = false;
        boolean fromEquipment = false;

        if (draggedItem != null && !draggedItem.stack().isEmpty()) {
            previewStack = draggedItem.stack();
            rotated = draggedItem.rotated();
        } else if (draggedEquipmentItem != null && !draggedEquipmentItem.stack().isEmpty()) {
            previewStack = draggedEquipmentItem.stack();
            rotated = draggedEquipmentItem.rotated();
            fromEquipment = true;
        }

        if (previewStack.isEmpty()) {
            return;
        }

        ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(previewStack, rotated);
        int w = Math.max(CELL, size.width() * CELL);
        int h = Math.max(CELL, size.height() * CELL);
        int x = mouseX - w / 2;
        int y = mouseY - h / 2;
        graphics.fill(x, y, x + w, y + h, fromEquipment ? 0xBB3A2F23 : 0xBB2C3328);
        drawOutline(graphics, x, y, w, h, WARNING);
        graphics.item(previewStack, x + Math.max(1, (w - 16) / 2), y + Math.max(1, (h - 16) / 2));
        graphics.itemDecorations(this.font, previewStack, x + Math.max(1, (w - 16) / 2), y + Math.max(1, (h - 16) / 2), CzoStackCounts.countOverlay(previewStack));
        drawTinyText(graphics, size.width() + "×" + size.height(), x + 3, y + 3, TEXT_MUTED);
        if (fromEquipment) {
            drawTinyText(graphics, "снять", x + 3, y + h - 8, WARNING);
        }
    }

    private void drawNotice(GuiGraphicsExtractor graphics) {
        if (noticeTicks <= 0 || noticeText.isBlank()) {
            return;
        }
        int boxW = Math.max(120, Math.round(this.font.width(noticeText) * 0.65F) + 18);
        int x = this.width / 2 - boxW / 2;
        int y = 8;
        graphics.fill(x, y, x + boxW, y + 18, 0xDD111511);
        drawOutline(graphics, x, y, boxW, 18, OUTLINE_HOVER);
        drawCenteredScaledText(graphics, noticeText, this.width / 2, y + 5, WARNING, 0.65F);
    }

    private void notice(String text) {
        noticeText = text == null ? "" : text;
        noticeTicks = 45;
    }

    private BackpackMetrics backpackMetrics() {
        int left = Math.max(10, (this.width - windowWidth()) / 2);
        int top = 18 + TOP_BAR_H + 5;
        int windowW = Math.min(windowWidth(), this.width - 20);
        int gap = 12;
        int minBackpackW = GRID_COLUMNS * CELL + 42;
        int equipmentW = Math.min(315, Math.max(250, windowW - minBackpackW - gap - 18));
        int backpackW = windowW - equipmentW - gap - 18;
        if (backpackW < minBackpackW) {
            int deficit = minBackpackW - backpackW;
            equipmentW = Math.max(235, equipmentW - deficit);
            backpackW = windowW - equipmentW - gap - 18;
        }
        int panelH = (this.height - 18 - 18) - TOP_BAR_H - 10;
        int backpackX = left + 9 + equipmentW + gap;
        int backpackY = top;

        int gridX = backpackX + 10;
        int gridY = backpackY + 35;
        int scrollbarX = backpackX + backpackW - 10;
        int gridW = Math.min(GRID_COLUMNS * CELL, Math.max(CELL, scrollbarX - gridX - 7));
        gridW = (gridW / CELL) * CELL;
        int visibleRows = Math.max(3, Math.min(GRID_ROWS, (panelH - 62) / CELL));
        int gridH = visibleRows * CELL;
        int maxScroll = Math.max(0, GRID_ROWS - visibleRows);
        scrollRows = clamp(scrollRows, 0, maxScroll);
        return new BackpackMetrics(backpackX, backpackY, backpackW, panelH, gridX, gridY, gridW, gridH, visibleRows, maxScroll);
    }

    private Rect autoSortButtonRect(int panelX, int panelY, int panelW, int panelH) {
        return new Rect(panelX + panelW - 49, panelY + panelH - 22, 38, 15);
    }

    private int estimateWeightKg(LocalPlayer player) {
        int cells = 0;
        Inventory inventory = player.getInventory();
        for (int slot = GridInventorySlots.FIRST_PLAYER_SLOT; slot <= GridInventorySlots.LAST_PLAYER_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(stack);
                cells += size.width() * size.height();
            }
        }
        return Math.max(0, Math.round(cells * 0.9F));
    }

    private void drawStatsTab(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY, LocalPlayer player, float partialTick) {
        int gap = 10;
        int panelH = height - 10;
        int contentX = left + 9;
        int contentW = Math.max(260, width - 18);

        int statsW = Math.min(175, Math.max(140, contentW * 30 / 100));
        int effectsW = Math.min(165, Math.max(125, contentW * 28 / 100));
        int modelW = contentW - statsW - effectsW - gap * 2;
        if (modelW < 130) {
            int need = 130 - modelW;
            int shrinkStats = Math.min(need / 2 + need % 2, Math.max(0, statsW - 130));
            statsW -= shrinkStats;
            need -= shrinkStats;
            int shrinkEffects = Math.min(need, Math.max(0, effectsW - 115));
            effectsW -= shrinkEffects;
            modelW = contentW - statsW - effectsW - gap * 2;
        }
        modelW = Math.max(120, modelW);

        int x1 = contentX;
        int x2 = x1 + statsW + gap;
        int x3 = Math.min(left + width - 9 - effectsW, x2 + modelW + gap);
        if (x3 < x2 + 115 + gap) {
            modelW = Math.max(115, x3 - x2 - gap);
        }

        drawStatsList(graphics, x1, top + 5, statsW, panelH, player);
        drawPlayerModelPanel(graphics, x2, top + 5, modelW, panelH, mouseX, mouseY, player);
        drawArtifactStatsPanel(graphics, x3, top + 5, effectsW, panelH, player);
    }

    private void drawStatsList(GuiGraphicsExtractor graphics, int x, int y, int w, int h, LocalPlayer player) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        drawOutline(graphics, x, y, w, h, OUTLINE);

        drawSmallText(graphics, "ХАРАКТЕРИСТИКИ", x + 9, y + 7, TEXT);
        int viewportTop = y + 22;
        int viewportBottom = y + h - 8;
        int contentHeight = 150;
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        statsScrollPixels = clamp(statsScrollPixels, 0, maxScroll);

        graphics.enableScissor(x + 2, viewportTop, x + w - 8, viewportBottom);
        int line = viewportTop + 4 - statsScrollPixels;
        int step = 10;
        drawStatLine(graphics, "Живучесть", format(player.getHealth()) + "/" + format(player.getMaxHealth()), x + 9, line, GOOD); line += step;
        drawStatLine(graphics, "Сытость", player.getFoodData().getFoodLevel() + "/20", x + 9, line, TEXT); line += step;
        drawStatLine(graphics, "Броня", String.valueOf(player.getArmorValue()), x + 9, line, TEXT); line += step;
        drawStatLine(graphics, "Опыт", String.valueOf(player.experienceLevel), x + 9, line, TEXT_MUTED); line += step + 6;

        drawTinyText(graphics, "ДВИЖЕНИЕ", x + 9, line, WARNING); line += step;
        drawStatLine(graphics, "Скорость", formatAttributePercent(player, Attributes.MOVEMENT_SPEED), x + 9, line, TEXT); line += step;
        drawStatLine(graphics, "Переносимый вес", estimateWeightKg(player) + "/60 кг", x + 9, line, TEXT); line += step;
        drawStatLine(graphics, "Регенерация", "база", x + 9, line, TEXT_DIM); line += step + 6;

        drawTinyText(graphics, "ЗАЩИТА", x + 9, line, WARNING); line += step;
        drawStatLine(graphics, "Пулестойкость", String.valueOf(player.getArmorValue() * 10), x + 9, line, GOOD); line += step;
        drawStatLine(graphics, "Радиация", "0", x + 9, line, TEXT_MUTED); line += step;
        drawStatLine(graphics, "Химзащита", "0", x + 9, line, TEXT_MUTED); line += step;
        drawStatLine(graphics, "Биозащита", "0", x + 9, line, TEXT_MUTED); line += step;
        drawStatLine(graphics, "Пси-защита", "0", x + 9, line, TEXT_MUTED); line += step;
        drawStatLine(graphics, "Термозащита", "0", x + 9, line, TEXT_MUTED); line += step;
        drawStatLine(graphics, "Кровотечение", "0%", x + 9, line, TEXT_MUTED);
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = x + w - 6;
            graphics.fill(barX, viewportTop, barX + 3, viewportBottom, 0x55000000);
            int trackH = viewportBottom - viewportTop;
            int thumbH = Math.max(14, trackH * trackH / Math.max(trackH, contentHeight));
            int thumbY = viewportTop + (trackH - thumbH) * statsScrollPixels / Math.max(1, maxScroll);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAA9B9B9B);
        }
    }

    private void drawPlayerModelPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY, LocalPlayer player) {
        graphics.fill(x, y, x + w, y + h, PANEL_SOFT);
        drawOutline(graphics, x, y, w, h, OUTLINE);
        drawCenteredScaledText(graphics, "МОДЕЛЬ ПЕРСОНАЖА", x + w / 2, y + 9, TEXT_MUTED, 0.62F);

        // Stage 8A: размер модели после теста уменьшен в 2 раза относительно Stage 7P.
        // Нижний блок оружия остаётся убранным, панель используется только под превью персонажа.
        int modelTop = y + 22;
        int modelBottom = y + h - 8;
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                x + 8,
                modelTop,
                x + w - 8,
                modelBottom,
                Math.max(43, Math.min(66, h / 4)),
                0.0625F,
                mouseX,
                mouseY,
                player
        );
    }

    private void drawArtifactStatsPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, LocalPlayer player) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        drawOutline(graphics, x, y, w, h, OUTLINE);
        drawSmallText(graphics, "АРТЕФАКТЫ / ЭФФЕКТЫ", x + 9, y + 7, TEXT);

        int viewportTop = y + 24;
        int viewportBottom = y + h - 8;
        int contentHeight = 235;
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        artifactScrollPixels = clamp(artifactScrollPixels, 0, maxScroll);

        graphics.enableScissor(x + 2, viewportTop, x + w - 8, viewportBottom);
        int line = viewportTop + 4 - artifactScrollPixels;
        drawTinyText(graphics, "Контейнер артефактов ещё не подключён", x + 9, line, TEXT_MUTED); line += 13;
        drawTinyText(graphics, "Сюда пойдут:", x + 9, line, WARNING); line += 11;
        drawTinyText(graphics, "+ регенерация", x + 14, line, GOOD); line += 10;
        drawTinyText(graphics, "+ защита от зон", x + 14, line, GOOD); line += 10;
        drawTinyText(graphics, "- радиофон", x + 14, line, BAD); line += 10;
        drawTinyText(graphics, "- пси/хим штрафы", x + 14, line, BAD); line += 17;

        drawTinyText(graphics, "АКТИВНЫЕ ЭФФЕКТЫ", x + 9, line, WARNING); line += 12;
        if (player.getActiveEffects().isEmpty()) {
            drawTinyText(graphics, "Нет", x + 9, line, TEXT_DIM);
            line += 10;
        } else {
            for (var effect : player.getActiveEffects()) {
                drawTinyText(graphics, compact(effect.getEffect().value().getDisplayName().getString(), 20), x + 9, line, TEXT);
                line += 10;
            }
        }

        line += 10;
        drawTinyText(graphics, "Позже тут будут:", x + 9, line, WARNING); line += 11;
        drawTinyText(graphics, "• параметры контейнера", x + 12, line, TEXT_MUTED); line += 10;
        drawTinyText(graphics, "• активные артефакты", x + 12, line, TEXT_MUTED); line += 10;
        drawTinyText(graphics, "• суммарные бонусы", x + 12, line, TEXT_MUTED); line += 10;
        drawTinyText(graphics, "• суммарные штрафы", x + 12, line, TEXT_MUTED);
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = x + w - 6;
            graphics.fill(barX, viewportTop, barX + 3, viewportBottom, 0x55000000);
            int trackH = viewportBottom - viewportTop;
            int thumbH = Math.max(14, trackH * trackH / Math.max(trackH, contentHeight));
            int thumbY = viewportTop + (trackH - thumbH) * artifactScrollPixels / Math.max(1, maxScroll);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAA9B9B9B);
        }
    }

    private void drawStatLine(GuiGraphicsExtractor graphics, String label, String value, int x, int y, int valueColor) {
        drawTinyText(graphics, label, x, y, TEXT_MUTED);
        drawTinyText(graphics, value, x + 105, y, valueColor);
    }

    private String formatAttributePercent(LocalPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return "100%";
        }
        return Math.round(instance.getValue() * 1000.0D) + "%";
    }

    private void drawSwitchButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.width - 39;
        int y = this.height - 39;
        boolean hovered = contains(x, y, 32, 32, mouseX, mouseY);
        graphics.fill(x, y, x + 32, y + 32, hovered ? 0xCC333822 : 0xAA111511);
        drawOutline(graphics, x, y, 32, 32, hovered ? OUTLINE_HOVER : OUTLINE);
        // Пока рисуем иконку как текстурный спрайт не через atlas, а простым фоном + знак.
        // Файл texture/gui/inventory_switch_button.png приложен в архив под следующий кастомный widget.
        drawCenteredScaledText(graphics, "CZO", x + 16, y + 7, hovered ? WARNING : TEXT, 0.65F);
        drawCenteredScaledText(graphics, "↔", x + 16, y + 18, TEXT_MUTED, 0.70F);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return super.mouseClicked(event, doubleClick);
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        boolean rightClick = event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;

        int top = 18;
        int left = Math.max(10, (this.width - windowWidth()) / 2);
        int winW = Math.min(windowWidth(), this.width - 20);
        int tabW = Math.min(160, Math.max(120, winW / 5));
        if (contains(left + 9, top + 6, tabW, 22, mouseX, mouseY)) {
            currentTab = Tab.INVENTORY;
            return true;
        }
        if (contains(left + 14 + tabW, top + 6, tabW, 22, mouseX, mouseY)) {
            currentTab = Tab.STATS;
            return true;
        }

        if (currentTab == Tab.INVENTORY) {
            if (handleEquipmentClick(rightClick)) {
                return true;
            }
            if (handleBackpackClick(mouseX, mouseY, rightClick)) {
                return true;
            }

            if (appearanceButtonW > 0 && contains(appearanceButtonX, appearanceButtonY, appearanceButtonW, appearanceButtonH, mouseX, mouseY)) {
                this.minecraft.setScreen(new CzoAppearanceScreen(this));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleEquipmentClick(boolean rightClick) {
        if (hoveredEquipmentSlot == null) {
            return false;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        if (draggedEquipmentItem != null) {
            if (draggedEquipmentItem.slot() == hoveredEquipmentSlot) {
                draggedEquipmentItem = null;
                notice("Перенос отменён");
                return true;
            }
            notice("Перенос между слотами будет следующим этапом");
            return true;
        }

        if (draggedItem != null) {
            if (!hoveredEquipmentSlot.canAccept(draggedItem.stack())) {
                notice("Предмет не подходит: " + CzoEquipmentSlotStorage.displayName(hoveredEquipmentSlot));
                return true;
            }
            ClientPacketDistributor.sendToServer(new ServerboundEquipFromInventoryPacket(
                    hoveredEquipmentSlot.ordinal(),
                    draggedItem.vanillaSlot()
            ));
            notice("Экипировка: " + CzoEquipmentSlotStorage.displayName(hoveredEquipmentSlot));
            draggedItem = null;
            return true;
        }

        ItemStack stack = CzoEquipmentSlotStorage.get(player, hoveredEquipmentSlot);
        if (!stack.isEmpty()) {
            if (rightClick) {
                ClientPacketDistributor.sendToServer(new ServerboundUnequipToBackpackPacket(hoveredEquipmentSlot.ordinal()));
                notice("Снятие в первое свободное место");
                return true;
            }

            draggedEquipmentItem = new DraggedEquipmentItem(hoveredEquipmentSlot, stack, false);
            notice("Экипировка взята: " + CzoEquipmentSlotStorage.displayName(hoveredEquipmentSlot));
            return true;
        }

        return true;
    }

    private boolean handleBackpackClick(int mouseX, int mouseY, boolean rightClick) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        BackpackMetrics metrics = backpackMetrics();
        if (metrics == null) {
            return false;
        }

        Rect sortButton = autoSortButtonRect(metrics.panelX(), metrics.panelY(), metrics.panelW(), metrics.panelH());
        if (contains(sortButton.x(), sortButton.y(), sortButton.w(), sortButton.h(), mouseX, mouseY)) {
            ClientPacketDistributor.sendToServer(ServerboundSortInventoryGridPacket.INSTANCE);
            draggedItem = null;
            draggedEquipmentItem = null;
            notice("Автосортировка");
            return true;
        }

        if (!contains(metrics.gridX(), metrics.gridY(), metrics.gridW(), metrics.gridH(), mouseX, mouseY)) {
            return false;
        }

        int cellX = (mouseX - metrics.gridX()) / CELL;
        int cellY = (mouseY - metrics.gridY()) / CELL + scrollRows;

        if (cellX < 0 || cellY < 0 || cellX >= GRID_COLUMNS || cellY >= GRID_ROWS) {
            return true;
        }

        BackpackSnapshot snapshot = buildBackpackSnapshot(player);
        if (draggedEquipmentItem != null) {
            ClientPacketDistributor.sendToServer(new ServerboundUnequipToGridPositionPacket(
                    draggedEquipmentItem.slot().ordinal(),
                    cellX,
                    cellY,
                    draggedEquipmentItem.rotated() ? 1 : 0
            ));
            draggedEquipmentItem = null;
            notice("Снятие в выбранную клетку");
            return true;
        }

        if (draggedItem != null) {
            ClientPacketDistributor.sendToServer(new ServerboundSetInventoryGridPositionPacket(
                    draggedItem.vanillaSlot(),
                    cellX,
                    cellY,
                    draggedItem.rotated() ? 1 : 0
            ));
            draggedItem = null;
            notice("Перемещение предмета");
            return true;
        }

        GridEntry clicked = snapshot.inventory().getEntryAt(cellX, cellY).orElse(null);
        if (clicked == null) {
            return true;
        }

        Integer vanillaSlot = snapshot.slotByEntryId().get(clicked.id());
        if (vanillaSlot == null || vanillaSlot < 0) {
            return true;
        }

        if (rightClick) {
            ClientPacketDistributor.sendToServer(new ServerboundQuickEquipFromInventoryPacket(vanillaSlot));
            notice("Быстрое экипирование");
            return true;
        }

        draggedItem = new DraggedGridItem(vanillaSlot, clicked.stack(), clicked.rotated());
        notice("Предмет взят");
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (currentTab == Tab.STATS) {
            int delta = scrollY < 0 ? 10 : -10;
            if (x > this.width * 0.67D) {
                artifactScrollPixels = clamp(artifactScrollPixels + delta, 0, 220);
            } else {
                statsScrollPixels = clamp(statsScrollPixels + delta, 0, 180);
            }
            return true;
        }
        if (currentTab != Tab.INVENTORY) {
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }

        BackpackMetrics metrics = backpackMetrics();
        if (metrics == null || !contains(metrics.gridX(), metrics.gridY(), metrics.gridW(), metrics.gridH(), (int) x, (int) y)) {
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }

        scrollRows = clamp(scrollRows + (scrollY < 0 ? 1 : -1), 0, metrics.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_G && currentTab == Tab.INVENTORY) {
            int slotToDrop = -1;
            if (draggedItem != null) {
                slotToDrop = draggedItem.vanillaSlot();
            } else if (hoveredGridEntry != null && hoveredVanillaSlot >= 0) {
                slotToDrop = hoveredVanillaSlot;
            }

            if (slotToDrop >= 0) {
                ClientPacketDistributor.sendToServer(new ServerboundDropBackpackStackPacket(slotToDrop));
                draggedItem = null;
                notice("Стак выброшен");
                return true;
            }
        }

        if (event.key() == GLFW.GLFW_KEY_V || event.key() == GLFW.GLFW_KEY_R) {
            if (draggedItem != null) {
                draggedItem = draggedItem.withToggledRotation();
                notice("Предмет повёрнут в руке");
                return true;
            }
            if (draggedEquipmentItem != null) {
                draggedEquipmentItem = draggedEquipmentItem.withToggledRotation();
                notice("Экипировка повёрнута в руке");
                return true;
            }
            if (hoveredGridEntry != null && hoveredVanillaSlot >= 0) {
                ClientPacketDistributor.sendToServer(new ServerboundSetInventoryGridPositionPacket(
                        hoveredVanillaSlot,
                        hoveredGridEntry.rect().x(),
                        hoveredGridEntry.rect().y(),
                        hoveredGridEntry.rotated() ? 0 : 1
                ));
                notice("Поворот предмета");
                return true;
            }
        }

        if (event.isEscape()) {
            if (draggedItem != null || draggedEquipmentItem != null) {
                draggedItem = null;
                draggedEquipmentItem = null;
                notice("Перенос отменён");
                return true;
            }
            this.onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_TAB) {
            currentTab = currentTab == Tab.INVENTORY ? Tab.STATS : Tab.INVENTORY;
            return true;
        }
        return super.keyPressed(event);
    }

    private void openCreativeInventoryOrClose() {
        LocalPlayer player = minecraft.player;
        if (player != null && player.hasInfiniteMaterials()) {
            minecraft.setScreen(new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get()));
            return;
        }
        this.onClose();
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.text(this.font, Component.literal(text), x, y, color, true);
    }

    private void drawSmallText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, 0.72F);
    }

    private void drawTinyText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        drawScaledText(graphics, text, x, y, color, 0.56F);
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, Component.literal(text), Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popMatrix();
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String text, int y, int color) {
        drawText(graphics, text, this.width / 2 - this.font.width(text) / 2, y, color);
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

    private String format(float value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String compact(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private enum Tab {
        INVENTORY,
        STATS
    }

    private record DraggedGridItem(int vanillaSlot, ItemStack stack, boolean rotated) {
        private DraggedGridItem withToggledRotation() {
            return new DraggedGridItem(vanillaSlot, stack, !rotated);
        }
    }

    private record DraggedEquipmentItem(CzoEquipmentSlot slot, ItemStack stack, boolean rotated) {
        private DraggedEquipmentItem withToggledRotation() {
            return new DraggedEquipmentItem(slot, stack, !rotated);
        }
    }

    private record Rect(int x, int y, int w, int h) {
    }

    private record BackpackMetrics(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int gridX,
            int gridY,
            int gridW,
            int gridH,
            int visibleRows,
            int maxScroll
    ) {
    }

    private record BackpackSnapshot(GridInventory inventory, Map<UUID, Integer> slotByEntryId) {
    }
}
