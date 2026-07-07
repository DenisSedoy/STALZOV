package com.czo.inventory.grid;

/** Позиция верхней левой клетки предмета в grid-инвентаре. */
public record GridPosition(int x, int y) {
    public GridPosition offset(int dx, int dy) {
        return new GridPosition(x + dx, y + dy);
    }
}
