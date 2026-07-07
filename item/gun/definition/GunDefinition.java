package com.czo.item.gun.definition;

import java.util.List;

public record GunDefinition(
        String id,
        String displayName,

        String ammoItemId,
        String defaultMagazineId,

        String previewTexture,

        GunStats stats,
        List<GunSlotDefinition> rootSlots
) {
}