package com.czo.item.gun.module;

import java.util.Arrays;
import java.util.Optional;

public enum GunModuleSlot {
    MAGAZINE("magazine", "Магазин"),
    MUZZLE("muzzle", "Дульник"),
    OPTIC("optic", "Прицел"),
    STOCK("stock", "Приклад"),
    GRIP("grip", "Рукоять"),
    BARREL("barrel", "Ствол"),
    HANDGUARD("handguard", "Цевьё"),
    CHARM("charm", "Украшение");

    private final String id;
    private final String displayName;

    GunModuleSlot(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<GunModuleSlot> fromId(String id) {
        return Arrays.stream(values())
                .filter(slot -> slot.id.equals(id))
                .findFirst();
    }
}