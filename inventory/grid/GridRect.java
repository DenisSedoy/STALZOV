package com.czo.inventory.grid;

import java.util.ArrayList;
import java.util.List;

/** Прямоугольник, который предмет занимает в сетке. */
public record GridRect(int x, int y, int width, int height) {
    public GridRect {
        if (width <= 0) {
            throw new IllegalArgumentException("GridRect width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("GridRect height must be positive");
        }
    }

    public static GridRect of(GridPosition position, ItemSize size) {
        return new GridRect(position.x(), position.y(), size.width(), size.height());
    }

    public int rightExclusive() {
        return x + width;
    }

    public int bottomExclusive() {
        return y + height;
    }

    public boolean contains(int cellX, int cellY) {
        return cellX >= x && cellX < rightExclusive() && cellY >= y && cellY < bottomExclusive();
    }

    public boolean intersects(GridRect other) {
        return x < other.rightExclusive()
                && rightExclusive() > other.x
                && y < other.bottomExclusive()
                && bottomExclusive() > other.y;
    }

    public boolean isInside(int gridWidth, int gridHeight) {
        return x >= 0 && y >= 0 && rightExclusive() <= gridWidth && bottomExclusive() <= gridHeight;
    }

    public List<GridPosition> cells() {
        List<GridPosition> cells = new ArrayList<>(width * height);
        for (int yy = y; yy < bottomExclusive(); yy++) {
            for (int xx = x; xx < rightExclusive(); xx++) {
                cells.add(new GridPosition(xx, yy));
            }
        }
        return cells;
    }

    public GridPosition position() {
        return new GridPosition(x, y);
    }

    public ItemSize size() {
        return new ItemSize(width, height);
    }
}
