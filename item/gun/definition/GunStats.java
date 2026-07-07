package com.czo.item.gun.definition;

public record GunStats(
        float damage,
        double range,
        int rpm,

        int baseReloadTicks,

        float hipSpreadDegrees,
        float aimSpreadDegrees,

        float recoilVertical,
        float recoilHorizontal,

        double muzzleVelocity,
        double gravity,
        double drag
) {
    public int fireDelayTicks() {
        return Math.max(1, Math.round(1200.0F / rpm));
    }
}