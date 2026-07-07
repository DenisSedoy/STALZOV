package com.czo.inventory.grid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Автосортировка: группирует предметы по категориям и пакует их сверху вниз, слева направо. */
public final class GridInventorySorter {
    private GridInventorySorter() {
    }

    public static boolean sortInPlace(GridInventory inventory, ItemSizeRegistry sizeRegistry) {
        if (inventory == null || sizeRegistry == null) {
            return false;
        }

        List<GridEntry> entries = new ArrayList<>(inventory.entries());
        entries.sort(Comparator
                .comparingInt((GridEntry entry) -> InventoryItemCategory.detect(entry.stack()).sortOrder())
                .thenComparingInt((GridEntry entry) -> -sizeRegistry.sizeOf(entry.stack(), entry.rotated()).area())
                .thenComparing(entry -> entry.stack().getHoverName().getString()));

        GridInventory sorted = new GridInventory(inventory.width(), inventory.height());
        for (GridEntry entry : entries) {
            ItemSize size = sizeRegistry.sizeOf(entry.stack(), entry.rotated());
            PlacementResult result = sorted.placeFirstFree(entry.stack(), size, entry.rotated());
            if (!result.ok()) {
                return false;
            }
        }

        inventory.clear();
        for (GridEntry entry : sorted.entries()) {
            PlacementResult result = inventory.place(entry.stack(), entry.rect().position(), entry.size(), entry.rotated());
            if (!result.ok()) {
                return false;
            }
        }
        return true;
    }
}
