package com.czo.inventory.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Готовый снимок характеристик для окна статистики. */
public final class PlayerStatsSnapshot {
    private final EnumMap<StatType, Double> stats;
    private final EnumMap<ProtectionType, Double> protections;

    public PlayerStatsSnapshot(Map<StatType, Double> stats, Map<ProtectionType, Double> protections) {
        this.stats = new EnumMap<>(StatType.class);
        this.protections = new EnumMap<>(ProtectionType.class);

        for (StatType type : StatType.values()) {
            this.stats.put(type, stats.getOrDefault(type, 0.0));
        }
        for (ProtectionType type : ProtectionType.values()) {
            this.protections.put(type, protections.getOrDefault(type, 0.0));
        }
    }

    public double stat(StatType type) {
        return stats.getOrDefault(type, 0.0);
    }

    public double protection(ProtectionType type) {
        return protections.getOrDefault(type, 0.0);
    }

    public Map<StatType, Double> stats() {
        return Collections.unmodifiableMap(stats);
    }

    public Map<ProtectionType, Double> protections() {
        return Collections.unmodifiableMap(protections);
    }
}
