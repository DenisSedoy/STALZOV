package com.czo.item.gun.module;

import java.util.List;
import java.util.Map;

public final class CzoGunModules {
    private CzoGunModules() {
    }

    public static final String NONE = "czo:none";

    // Внутренний дефолтный магазин. НЕ предмет, НЕ лут, НЕ показываем в меню выбора.
    public static final String PM_MAG_8 = "czo:pm_mag_8";

    // Физический предмет-модуль. Есть item czo:pm_mag_12.
    public static final String PM_MAG_12 = "czo:pm_mag_12";

    public static final String TEST_SUPPRESSOR = "czo:test_suppressor";
    public static final String TEST_RED_DOT = "czo:test_red_dot";

    private static final GunModule NONE_MODULE = new GunModule(
            NONE,
            GunModuleSlot.CHARM,
            "Пусто",
            0,
            0,
            GunStatModifier.NONE,
            null,
            true
    );

    private static final Map<String, GunModule> MODULES = Map.of(
            PM_MAG_8, new GunModule(
                    PM_MAG_8,
                    GunModuleSlot.MAGAZINE,
                    "ПМ 8",
                    8,
                    40,
                    GunStatModifier.NONE,
                    null,
                    true
            ),

            PM_MAG_12, new GunModule(
                    PM_MAG_12,
                    GunModuleSlot.MAGAZINE,
                    "ПМ 12",
                    12,
                    46,
                    GunStatModifier.NONE,
                    "czo:pm_mag_12",
                    false
            ),

            TEST_SUPPRESSOR, new GunModule(
                    TEST_SUPPRESSOR,
                    GunModuleSlot.MUZZLE,
                    "Тестовый глушитель",
                    0,
                    0,
                    new GunStatModifier(
                            0.0F,
                            -5.0D,
                            0,
                            4,
                            0.05F,
                            0.02F,
                            -0.10F,
                            -0.04F,
                            -20.0D,
                            0.0D,
                            0.001D
                    ),
                    null,
                    false
            ),

            TEST_RED_DOT, new GunModule(
                    TEST_RED_DOT,
                    GunModuleSlot.OPTIC,
                    "Тестовый коллиматор",
                    0,
                    0,
                    new GunStatModifier(
                            0.0F,
                            0.0D,
                            0,
                            0,
                            0.0F,
                            -0.10F,
                            0.0F,
                            0.0F,
                            0.0D,
                            0.0D,
                            0.0D
                    ),
                    null,
                    false
            )
    );

    public static GunModule getModule(String id) {
        if (id == null || id.isBlank() || NONE.equals(id)) {
            return NONE_MODULE;
        }

        return MODULES.get(id);
    }

    public static GunModule getMagazine(String id) {
        GunModule module = MODULES.get(id);

        if (module == null || module.slot() != GunModuleSlot.MAGAZINE) {
            return MODULES.get(PM_MAG_8);
        }

        return module;
    }

    public static List<GunModule> getModulesForSlot(GunModuleSlot slot) {
        return MODULES.values()
                .stream()
                .filter(module -> module.slot() == slot)
                .toList();
    }

    public static List<GunModule> getInstallableModulesForSlot(GunModuleSlot slot) {
        return MODULES.values()
                .stream()
                .filter(module -> module.slot() == slot)
                .filter(GunModule::canBeShownInModificationMenu)
                .toList();
    }

    public static String getDefaultModuleIdForSlot(GunModuleSlot slot) {
        return switch (slot) {
            case MAGAZINE -> PM_MAG_8;
            default -> NONE;
        };
    }
}
