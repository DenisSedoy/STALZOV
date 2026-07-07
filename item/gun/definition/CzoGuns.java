package com.czo.item.gun.definition;

import com.czo.item.gun.ammo.CzoAmmo;
import com.czo.item.gun.module.CzoGunModules;

import java.util.List;
import java.util.Map;

public final class CzoGuns {
    private CzoGuns() {
    }

    public static final String PM = "czo:pm";
    public static final String AK74 = "czo:ak74";

    private static final GunDefinition FALLBACK = new GunDefinition(
            PM,
            "ПМ",
            CzoAmmo.AMMO_9X18_FMJ,
            CzoGunModules.PM_MAG_8,
            "czo:textures/gui/guns/pm_preview.png",
            new GunStats(
                    6.0F,
                    80.0D,
                    300,
                    40,
                    1.25F,
                    0.25F,
                    1.2F,
                    0.35F,
                    315.0D,
                    0.035D,
                    0.995D
            ),
            List.of(
                    new GunSlotDefinition("magazine", "magazine", "Магазин", 18, 118, 6, 38),
                    new GunSlotDefinition("muzzle", "muzzle", "Дульник", -260, -92, -164, -8),
                    new GunSlotDefinition("grip", "grip", "Рукоять", 122, 92, 28, 36),
                    new GunSlotDefinition("charm", "charm", "Украшение", 230, -20, 6, -18)
            )
    );

    private static final Map<String, GunDefinition> GUNS = Map.of(
            PM, FALLBACK,

            AK74, new GunDefinition(
                    AK74,
                    "АК-74",
                    "czo:ammo_545x39",
                    "czo:ak74_mag_30",
                    "czo:textures/gui/guns/ak74_preview.png",
                    new GunStats(
                            9.0F,
                            180.0D,
                            600,
                            55,
                            1.8F,
                            0.35F,
                            2.5F,
                            0.8F,
                            900.0D,
                            0.025D,
                            0.998D
                    ),
                    List.of(
                            new GunSlotDefinition("magazine", "magazine", "Магазин", 18, 118, 6, 38),
                            new GunSlotDefinition("muzzle", "muzzle", "Дульник", -260, -92, -164, -8),
                            new GunSlotDefinition("barrel", "barrel", "Ствол", -236, 24, -118, -2),
                            new GunSlotDefinition("handguard", "handguard", "Цевьё", -180, -88, -80, -12),
                            new GunSlotDefinition("optic", "optic", "Прицел", 190, -112, 38, -26),
                            new GunSlotDefinition("stock", "stock", "Приклад", 230, 12, 118, -4),
                            new GunSlotDefinition("grip", "grip", "Рукоять", 122, 92, 28, 36),
                            new GunSlotDefinition("charm", "charm", "Украшение", 230, -20, 6, -18)
                    )
            )
    );

    public static GunDefinition get(String id) {
        return GUNS.getOrDefault(id, FALLBACK);
    }
}