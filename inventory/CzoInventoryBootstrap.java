package com.czo.inventory;

import com.czo.inventory.grid.ItemSizeRegistry;

/**
 * Точка входа будущей системы инвентаря.
 *
 * <p>На первом этапе здесь только общий реестр размеров предметов. Когда будем подключать GUI,
 * синхронизацию и storage/attachments, расширим этот класс.</p>
 */
public final class CzoInventoryBootstrap {
    public static final ItemSizeRegistry ITEM_SIZES = ItemSizeRegistry.createDefault();

    private static boolean initialized;

    private CzoInventoryBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
}
