package com.czo.item.gun.module;

import com.czo.item.gun.definition.GunStats;

/**
 * Модификатор характеристик оружия от установленного модуля.
 *
 * Сейчас это аддитивная система: значение модуля прибавляется к базовой/текущей стате.
 * Для отрицательных эффектов используем отрицательные значения.
 *
 * Примеры:
 * - aimSpreadDegreesAdd = -0.10F уменьшит разброс в ADS на 0.10 градуса.
 * - reloadTicksAdd = 6 увеличит перезарядку на 6 тиков, то есть на 0.3 секунды.
 * - recoilVerticalAdd = -0.15F уменьшит вертикальную отдачу.
 */
public record GunStatModifier(
        float damageAdd,
        double rangeAdd,
        int rpmAdd,

        int reloadTicksAdd,

        float hipSpreadDegreesAdd,
        float aimSpreadDegreesAdd,

        float recoilVerticalAdd,
        float recoilHorizontalAdd,

        double muzzleVelocityAdd,
        double gravityAdd,
        double dragAdd
) {
    public static final GunStatModifier NONE = new GunStatModifier(
            0.0F,
            0.0D,
            0,
            0,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0D,
            0.0D,
            0.0D
    );

    public GunStats apply(GunStats stats) {
        return new GunStats(
                Math.max(0.0F, stats.damage() + damageAdd),
                Math.max(1.0D, stats.range() + rangeAdd),
                Math.max(1, stats.rpm() + rpmAdd),
                Math.max(1, stats.baseReloadTicks() + reloadTicksAdd),
                Math.max(0.0F, stats.hipSpreadDegrees() + hipSpreadDegreesAdd),
                Math.max(0.0F, stats.aimSpreadDegrees() + aimSpreadDegreesAdd),
                Math.max(0.0F, stats.recoilVertical() + recoilVerticalAdd),
                Math.max(0.0F, stats.recoilHorizontal() + recoilHorizontalAdd),
                Math.max(0.0D, stats.muzzleVelocity() + muzzleVelocityAdd),
                Math.max(0.0D, stats.gravity() + gravityAdd),
                Math.max(0.0D, stats.drag() + dragAdd)
        );
    }

    public boolean isEmpty() {
        return this.equals(NONE);
    }
}
