package com.czo.item.gun.module;

import com.czo.item.gun.definition.GunStats;

public record GunModule(
        String id,
        GunModuleSlot slot,
        String displayName,

        // Для магазинов.
        int magazineCapacity,
        int reloadTicks,

        // Для всех модулей: влияние на характеристики оружия.
        GunStatModifier statModifier,

        // Физический предмет модуля в инвентаре. null = внутренний/служебный модуль.
        String physicalItemId,

        // true = дефолтная база оружия. Такие модули не показываем как лут/карточку выбора.
        boolean defaultModule
) {
    public GunModule(
            String id,
            GunModuleSlot slot,
            String displayName,
            int magazineCapacity,
            int reloadTicks
    ) {
        this(id, slot, displayName, magazineCapacity, reloadTicks, GunStatModifier.NONE, null, false);
    }

    public GunModule(
            String id,
            GunModuleSlot slot,
            String displayName,
            int magazineCapacity,
            int reloadTicks,
            GunStatModifier statModifier
    ) {
        this(id, slot, displayName, magazineCapacity, reloadTicks, statModifier, null, false);
    }

    public boolean isMagazine() {
        return slot == GunModuleSlot.MAGAZINE;
    }

    public boolean hasPhysicalItem() {
        return physicalItemId != null && !physicalItemId.isBlank();
    }

    public boolean canBeShownInModificationMenu() {
        return hasPhysicalItem() && !defaultModule;
    }

    public GunStats applyTo(GunStats stats) {
        return statModifier.apply(stats);
    }
}
