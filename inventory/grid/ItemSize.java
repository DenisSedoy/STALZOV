package com.czo.inventory.grid;

/**
 * Размер предмета в клетках grid-инвентаря.
 *
 * <p>Смысл такой же, как в EFT-подобных инвентарях: пистолет может быть 2x2,
 * винтовка 2x5, патроны 1x1, броня 3x3 и так далее.</p>
 */
public record ItemSize(int width, int height) {
    public static final ItemSize ONE_BY_ONE = new ItemSize(1, 1);

    public ItemSize {
        if (width <= 0) {
            throw new IllegalArgumentException("Item width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Item height must be positive");
        }
    }

    public ItemSize rotated() {
        return new ItemSize(height, width);
    }

    public int area() {
        return width * height;
    }

    public boolean fitsInside(int gridWidth, int gridHeight) {
        return width <= gridWidth && height <= gridHeight;
    }
}
