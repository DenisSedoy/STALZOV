package com.czo.inventory.stats;

import java.util.ArrayList;
import java.util.List;

/** Маленький калькулятор характеристик: база + набор модификаторов. */
public final class StatAccumulator {
    private final double baseValue;
    private final List<StatModifier> modifiers = new ArrayList<>();

    public StatAccumulator(double baseValue) {
        this.baseValue = baseValue;
    }

    public void add(StatModifier modifier) {
        if (modifier != null) {
            modifiers.add(modifier);
        }
    }

    public double value() {
        double add = 0.0;
        double multiplyBase = 0.0;
        double total = baseValue;

        for (StatModifier modifier : modifiers) {
            if (modifier.operation() == StatOperation.ADD) {
                add += modifier.amount();
            } else if (modifier.operation() == StatOperation.MULTIPLY_BASE) {
                multiplyBase += modifier.amount();
            }
        }

        total += add;
        total += baseValue * multiplyBase;

        for (StatModifier modifier : modifiers) {
            if (modifier.operation() == StatOperation.MULTIPLY_TOTAL) {
                total *= 1.0 + modifier.amount();
            }
        }

        return total;
    }

    public double baseValue() {
        return baseValue;
    }

    public List<StatModifier> modifiers() {
        return List.copyOf(modifiers);
    }
}
