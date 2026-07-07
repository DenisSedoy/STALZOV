package com.czo.inventory.stats;

/** Как модификатор влияет на итоговое значение. */
public enum StatOperation {
    /** Просто добавить число: 100 + 20. */
    ADD,
    /** Умножить базу: base + base * 0.20. */
    MULTIPLY_BASE,
    /** Умножить итог после ADD/MULTIPLY_BASE: total * 1.20. */
    MULTIPLY_TOTAL
}
