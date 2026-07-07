package com.czo.inventory.grid;

import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Один предмет, размещённый в grid-инвентаре. */
public final class GridEntry {
    private final UUID id;
    private final ItemStack stack;
    private final GridRect rect;
    private final boolean rotated;

    public GridEntry(UUID id, ItemStack stack, GridRect rect, boolean rotated) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("stack cannot be empty");
        }
        if (rect == null) {
            throw new IllegalArgumentException("rect cannot be null");
        }
        this.id = id;
        this.stack = stack.copy();
        this.rect = rect;
        this.rotated = rotated;
    }

    public UUID id() {
        return id;
    }

    /** Возвращает копию стака, чтобы внешняя логика случайно не изменила содержимое сетки. */
    public ItemStack stack() {
        return stack.copy();
    }

    public GridRect rect() {
        return rect;
    }

    public boolean rotated() {
        return rotated;
    }

    public ItemSize size() {
        return rect.size();
    }

    public GridEntry movedTo(GridPosition position) {
        return new GridEntry(id, stack, GridRect.of(position, rect.size()), rotated);
    }

    public GridEntry withSizeAtSamePosition(ItemSize size, boolean rotated) {
        return new GridEntry(id, stack, GridRect.of(rect.position(), size), rotated);
    }
}
