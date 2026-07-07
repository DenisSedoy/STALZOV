package com.czo.item.gun.ammo;

import com.czo.item.gun.definition.GunDefinition;
import com.czo.item.gun.module.GunStatModifier;

import java.util.List;
import java.util.Map;

public final class CzoAmmo {
    private CzoAmmo() {
    }

    // Базовый 9x18 FMJ. Он же калибр для всех 9x18 вариантов.
    public static final String AMMO_9X18_FMJ = "czo:ammo_9x18_fmj";
    public static final String AMMO_9X18_AP = "czo:ammo_9x18_ap";

    private static final Map<String, CzoAmmoDefinition> AMMO = Map.of(
            AMMO_9X18_FMJ, new CzoAmmoDefinition(
                    AMMO_9X18_FMJ,
                    "9x18 FMJ",
                    AMMO_9X18_FMJ,
                    GunStatModifier.NONE
            ),

            AMMO_9X18_AP, new CzoAmmoDefinition(
                    AMMO_9X18_AP,
                    "9x18 БП",
                    AMMO_9X18_FMJ,
                    new GunStatModifier(
                            2.0F,   // урон
                            8.0D,   // дальность
                            0,      // rpm
                            0,      // reload ticks
                            0.08F,  // разброс от бедра чуть выше
                            0.04F,  // ADS чуть выше
                            0.12F,  // отдача вверх выше
                            0.04F,  // отдача вбок выше
                            35.0D,  // скорость пули выше
                            0.0D,
                            -0.001D // чуть лучше удержание скорости
                    )
            )
    );

    public static CzoAmmoDefinition get(String id) {
        return AMMO.get(id);
    }

    public static CzoAmmoDefinition getOrDefault(String id, GunDefinition gun) {
        CzoAmmoDefinition ammo = get(id);

        if (ammo != null && isCompatible(gun, ammo.id())) {
            return ammo;
        }

        CzoAmmoDefinition fallback = get(gun.ammoItemId());
        return fallback != null ? fallback : get(AMMO_9X18_FMJ);
    }

    public static boolean isCompatible(GunDefinition gun, String ammoId) {
        CzoAmmoDefinition ammo = get(ammoId);
        return ammo != null && ammo.caliberId().equals(gun.ammoItemId());
    }

    public static List<CzoAmmoDefinition> getCompatibleAmmo(GunDefinition gun) {
        return AMMO.values()
                .stream()
                .filter(ammo -> ammo.caliberId().equals(gun.ammoItemId()))
                .toList();
    }
}
