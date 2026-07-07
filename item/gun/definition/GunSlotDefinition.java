package com.czo.item.gun.definition;

public record GunSlotDefinition(
        String path,
        String type,
        String label,

        int x,
        int y,

        int anchorX,
        int anchorY
) {
}