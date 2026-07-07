package com.czo.inventory.stats;

/** Основные характеристики персонажа для вкладки статистики. */
public enum StatType {
    SURVIVABILITY("stat.czo.survivability"),
    STAMINA("stat.czo.stamina"),
    WALK_SPEED("stat.czo.walk_speed"),
    RUN_SPEED("stat.czo.run_speed"),
    CARRY_WEIGHT("stat.czo.carry_weight"),
    HEALTH_REGEN("stat.czo.health_regen"),
    STAMINA_REGEN("stat.czo.stamina_regen"),
    HEALING_EFFICIENCY("stat.czo.healing_efficiency"),
    RECOIL("stat.czo.recoil"),
    BLEED_RESISTANCE("stat.czo.bleed_resistance"),
    TOUGHNESS("stat.czo.toughness");

    private final String translationKey;

    StatType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
