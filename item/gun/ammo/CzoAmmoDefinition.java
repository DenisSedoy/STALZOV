package com.czo.item.gun.ammo;

import com.czo.item.gun.definition.GunStats;
import com.czo.item.gun.module.GunStatModifier;

public record CzoAmmoDefinition(
        String id,
        String displayName,
        String caliberId,
        GunStatModifier statModifier
) {
    public GunStats applyTo(GunStats stats) {
        return statModifier.apply(stats);
    }
}
