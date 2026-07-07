package com.czo.inventory.grid;

import com.czo.inventory.equipment.CzoEquipmentSlotStorage;
import com.czo.registry.CzoDataComponents;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Мост между vanilla-инвентарём игрока и CZO grid-раскладкой.
 *
 * <p>На Stage 4 предметы рюкзака всё ещё лежат в vanilla inventory slots, но первые 9 слотов
 * зарезервированы под временную CZO-экипировку. Координаты HUD хранятся на ItemStack
 * через data components. Это даёт рабочий drag/drop, поворот, автосортировку и базовое
 * экипирование без отдельного attachment-слоя.</p>
 */
public final class GridInventorySlots {
    public static final int GRID_COLUMNS = 10;
    public static final int GRID_ROWS = 24;
    public static final int FIRST_PLAYER_SLOT = CzoEquipmentSlotStorage.BACKPACK_FIRST_SLOT;
    public static final int LAST_PLAYER_SLOT = 35;

    private GridInventorySlots() {
    }

    public static boolean isPlayerBackpackSlot(int slot) {
        return slot >= FIRST_PLAYER_SLOT && slot <= LAST_PLAYER_SLOT;
    }

    public static int gridX(ItemStack stack) {
        Integer value = stack == null || stack.isEmpty() ? null : stack.get(CzoDataComponents.INVENTORY_GRID_X.get());
        return value == null ? -1 : value;
    }

    public static int gridY(ItemStack stack) {
        Integer value = stack == null || stack.isEmpty() ? null : stack.get(CzoDataComponents.INVENTORY_GRID_Y.get());
        return value == null ? -1 : value;
    }

    public static boolean rotated(ItemStack stack) {
        Integer value = stack == null || stack.isEmpty() ? null : stack.get(CzoDataComponents.INVENTORY_GRID_ROTATED.get());
        return value != null && value != 0;
    }

    public static boolean hasStoredPosition(ItemStack stack) {
        return gridX(stack) >= 0 && gridY(stack) >= 0;
    }

    public static void clearStoredPosition(ItemStack stack) {
        setStoredPosition(stack, -1, -1, false);
    }

    public static void setStoredPosition(ItemStack stack, int x, int y, boolean rotated) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(CzoDataComponents.INVENTORY_GRID_X.get(), x);
        stack.set(CzoDataComponents.INVENTORY_GRID_Y.get(), y);
        stack.set(CzoDataComponents.INVENTORY_GRID_ROTATED.get(), rotated ? 1 : 0);
    }

    /**
     * Собирает визуальную сетку из vanilla-слотов. Сначала пытается сохранить координаты,
     * записанные на ItemStack, а предметы без координат или с конфликтом аккуратно допаковывает
     * в первое свободное место.
     */
    public static GridInventory buildPlayerBackpack(
            Inventory vanillaInventory,
            ItemSizeRegistry sizeRegistry,
            Map<UUID, Integer> slotByEntryId
    ) {
        return buildPlayerBackpack(vanillaInventory, sizeRegistry, slotByEntryId, -1);
    }

    public static GridInventory buildPlayerBackpack(
            Inventory vanillaInventory,
            ItemSizeRegistry sizeRegistry,
            Map<UUID, Integer> slotByEntryId,
            int ignoredVanillaSlot
    ) {
        GridInventory grid = new GridInventory(GRID_COLUMNS, GRID_ROWS);
        List<Integer> deferredSlots = new ArrayList<>();

        for (int slot = FIRST_PLAYER_SLOT; slot <= LAST_PLAYER_SLOT; slot++) {
            if (slot == ignoredVanillaSlot) {
                continue;
            }

            ItemStack stack = vanillaInventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (!tryPlaceStored(grid, stack, slot, sizeRegistry, slotByEntryId)) {
                deferredSlots.add(slot);
            }
        }

        for (int slot : deferredSlots) {
            ItemStack stack = vanillaInventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            boolean rotated = rotated(stack);
            ItemSize size = sizeRegistry.sizeOf(stack, rotated);
            PlacementResult placed = grid.placeFirstFree(stack, size, rotated);
            if (!placed.ok() && !ItemSize.ONE_BY_ONE.equals(size)) {
                placed = grid.placeFirstFree(stack, ItemSize.ONE_BY_ONE, false);
            }
            if (placed.ok() && placed.entryId() != null && slotByEntryId != null) {
                slotByEntryId.put(placed.entryId(), slot);
            }
        }

        return grid;
    }

    private static boolean tryPlaceStored(
            GridInventory grid,
            ItemStack stack,
            int vanillaSlot,
            ItemSizeRegistry sizeRegistry,
            Map<UUID, Integer> slotByEntryId
    ) {
        if (!hasStoredPosition(stack)) {
            return false;
        }

        boolean rotated = rotated(stack);
        ItemSize size = sizeRegistry.sizeOf(stack, rotated);
        PlacementResult placed = grid.place(stack, new GridPosition(gridX(stack), gridY(stack)), size, rotated);
        if (!placed.ok()) {
            return false;
        }

        if (placed.entryId() != null && slotByEntryId != null) {
            slotByEntryId.put(placed.entryId(), vanillaSlot);
        }
        return true;
    }
}
