package com.czo.inventory.stats;

/** Типы защиты персонажа, которые потом будут собираться из брони, маски, артефактов и заражений. */
public enum ProtectionType {
    BULLET("stat.czo.protection.bullet"),
    RUPTURE("stat.czo.protection.rupture"),
    EXPLOSION("stat.czo.protection.explosion"),
    ELECTRIC("stat.czo.protection.electric"),
    FIRE("stat.czo.protection.fire"),
    CHEMICAL("stat.czo.protection.chemical"),
    RADIATION("stat.czo.protection.radiation"),
    THERMAL("stat.czo.protection.thermal"),
    BIO("stat.czo.protection.bio"),
    PSY("stat.czo.protection.psy"),
    COLD("stat.czo.protection.cold"),
    BLEEDING("stat.czo.protection.bleeding");

    private final String translationKey;

    ProtectionType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
