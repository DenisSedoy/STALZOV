package com.czo.inventory.stats;

import net.minecraft.resources.Identifier;

/** Один числовой модификатор характеристики. */
public record StatModifier(Identifier id, double amount, StatOperation operation) {
    public StatModifier {
        if (id == null) {
            throw new IllegalArgumentException("Modifier id cannot be null");
        }
        if (operation == null) {
            operation = StatOperation.ADD;
        }
    }
}
