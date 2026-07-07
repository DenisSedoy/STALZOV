package com.czo.inventory.stats;

import java.util.EnumMap;

/**
 * Билдер снимка характеристик. Позже сюда подключаем экипировку, артефакты,
 * заражение, аномальные эффекты и оружие.
 */
public final class PlayerStatsBuilder {
    private final EnumMap<StatType, StatAccumulator> stats = new EnumMap<>(StatType.class);
    private final EnumMap<ProtectionType, StatAccumulator> protections = new EnumMap<>(ProtectionType.class);

    public PlayerStatsBuilder() {
        for (StatType type : StatType.values()) {
            stats.put(type, new StatAccumulator(defaultBase(type)));
        }
        for (ProtectionType type : ProtectionType.values()) {
            protections.put(type, new StatAccumulator(0.0));
        }
    }

    public PlayerStatsBuilder addStat(StatType type, StatModifier modifier) {
        stats.get(type).add(modifier);
        return this;
    }

    public PlayerStatsBuilder addProtection(ProtectionType type, StatModifier modifier) {
        protections.get(type).add(modifier);
        return this;
    }

    public PlayerStatsSnapshot build() {
        EnumMap<StatType, Double> statValues = new EnumMap<>(StatType.class);
        EnumMap<ProtectionType, Double> protectionValues = new EnumMap<>(ProtectionType.class);

        for (StatType type : StatType.values()) {
            statValues.put(type, stats.get(type).value());
        }
        for (ProtectionType type : ProtectionType.values()) {
            protectionValues.put(type, protections.get(type).value());
        }

        return new PlayerStatsSnapshot(statValues, protectionValues);
    }

    private static double defaultBase(StatType type) {
        return switch (type) {
            case SURVIVABILITY -> 100.0;
            case STAMINA -> 100.0;
            case WALK_SPEED -> 100.0;
            case RUN_SPEED -> 100.0;
            case CARRY_WEIGHT -> 60.0;
            case HEALTH_REGEN -> 0.0;
            case STAMINA_REGEN -> 100.0;
            case HEALING_EFFICIENCY -> 100.0;
            case RECOIL -> 0.0;
            case BLEED_RESISTANCE -> 0.0;
            case TOUGHNESS -> 0.0;
        };
    }
}
