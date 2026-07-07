package com.czo.inventory.grid;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Ядро EFT/STALKER-подобного инвентаря.
 *
 * <p>Этот класс не рисует GUI и не знает про Minecraft Screen. Он только хранит предметы,
 * проверяет занятость клеток и умеет размещать/двигать/поворачивать предметы.</p>
 */
public final class GridInventory {
    private final int width;
    private final int height;
    private final Map<UUID, GridEntry> entries = new LinkedHashMap<>();
    private final UUID[][] occupancy;
    private final Predicate<ItemStack> acceptedItems;

    public GridInventory(int width, int height) {
        this(width, height, stack -> true);
    }

    public GridInventory(int width, int height, Predicate<ItemStack> acceptedItems) {
        if (width <= 0) {
            throw new IllegalArgumentException("Grid width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Grid height must be positive");
        }
        this.width = width;
        this.height = height;
        this.occupancy = new UUID[width][height];
        this.acceptedItems = acceptedItems == null ? stack -> true : acceptedItems;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<GridEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries.values()));
    }

    public Optional<GridEntry> getEntry(UUID id) {
        return Optional.ofNullable(entries.get(id));
    }

    public Optional<GridEntry> getEntryAt(int x, int y) {
        if (!isCellInside(x, y)) {
            return Optional.empty();
        }
        UUID id = occupancy[x][y];
        return id == null ? Optional.empty() : Optional.ofNullable(entries.get(id));
    }

    public boolean isCellInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isCellFree(int x, int y) {
        return isCellInside(x, y) && occupancy[x][y] == null;
    }

    public PlacementResult canPlace(ItemStack stack, GridPosition position, ItemSize size) {
        return canPlace(stack, position, size, null);
    }

    public PlacementResult canPlace(ItemStack stack, GridPosition position, ItemSize size, UUID ignoredEntryId) {
        if (stack == null || stack.isEmpty()) {
            return PlacementResult.fail(PlacementResult.Status.EMPTY_STACK, "Cannot place empty stack");
        }
        if (size == null || size.width() <= 0 || size.height() <= 0) {
            return PlacementResult.fail(PlacementResult.Status.INVALID_SIZE, "Invalid item size");
        }
        if (!acceptedItems.test(stack)) {
            return PlacementResult.fail(PlacementResult.Status.ITEM_REJECTED, "This grid does not accept this stack");
        }

        GridRect rect = GridRect.of(position, size);
        if (!rect.isInside(width, height)) {
            return PlacementResult.fail(PlacementResult.Status.OUT_OF_BOUNDS, "Item is outside grid bounds");
        }

        for (GridPosition cell : rect.cells()) {
            UUID occupiedBy = occupancy[cell.x()][cell.y()];
            if (occupiedBy != null && !occupiedBy.equals(ignoredEntryId)) {
                return PlacementResult.fail(PlacementResult.Status.COLLIDES, "Target cells are already occupied");
            }
        }

        return PlacementResult.ok(ignoredEntryId);
    }

    public PlacementResult place(ItemStack stack, GridPosition position, ItemSize size, boolean rotated) {
        UUID id = UUID.randomUUID();
        PlacementResult result = canPlace(stack, position, size, null);
        if (!result.ok()) {
            return result;
        }
        GridEntry entry = new GridEntry(id, stack, GridRect.of(position, size), rotated);
        putEntry(entry);
        return PlacementResult.ok(id);
    }

    public PlacementResult place(ItemStack stack, int x, int y, ItemSize size, boolean rotated) {
        return place(stack, new GridPosition(x, y), size, rotated);
    }

    public Optional<GridPosition> findFirstFreePosition(ItemStack stack, ItemSize size) {
        if (stack == null || stack.isEmpty() || size == null) {
            return Optional.empty();
        }
        for (int y = 0; y <= height - size.height(); y++) {
            for (int x = 0; x <= width - size.width(); x++) {
                GridPosition position = new GridPosition(x, y);
                if (canPlace(stack, position, size).ok()) {
                    return Optional.of(position);
                }
            }
        }
        return Optional.empty();
    }

    public PlacementResult placeFirstFree(ItemStack stack, ItemSize size, boolean rotated) {
        Optional<GridPosition> position = findFirstFreePosition(stack, size);
        if (position.isEmpty()) {
            return PlacementResult.fail(PlacementResult.Status.OUT_OF_BOUNDS, "No free space for this item");
        }
        return place(stack, position.get(), size, rotated);
    }

    public PlacementResult move(UUID entryId, GridPosition newPosition) {
        GridEntry entry = entries.get(entryId);
        if (entry == null) {
            return PlacementResult.fail(PlacementResult.Status.NOT_FOUND, "Entry not found");
        }
        PlacementResult result = canPlace(entry.stack(), newPosition, entry.size(), entryId);
        if (!result.ok()) {
            return result;
        }
        removeOccupancy(entry);
        GridEntry moved = entry.movedTo(newPosition);
        putEntry(moved);
        return PlacementResult.ok(entryId);
    }

    public PlacementResult rotate(UUID entryId) {
        GridEntry entry = entries.get(entryId);
        if (entry == null) {
            return PlacementResult.fail(PlacementResult.Status.NOT_FOUND, "Entry not found");
        }
        ItemSize rotatedSize = entry.size().rotated();
        PlacementResult result = canPlace(entry.stack(), entry.rect().position(), rotatedSize, entryId);
        if (!result.ok()) {
            return result;
        }
        removeOccupancy(entry);
        GridEntry rotated = entry.withSizeAtSamePosition(rotatedSize, !entry.rotated());
        putEntry(rotated);
        return PlacementResult.ok(entryId);
    }

    public Optional<ItemStack> remove(UUID entryId) {
        GridEntry entry = entries.remove(entryId);
        if (entry == null) {
            return Optional.empty();
        }
        removeOccupancy(entry);
        return Optional.of(entry.stack());
    }

    public Optional<ItemStack> pickUpAt(int x, int y) {
        return getEntryAt(x, y).flatMap(entry -> remove(entry.id()));
    }

    public void clear() {
        entries.clear();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                occupancy[x][y] = null;
            }
        }
    }

    public int usedCells() {
        int used = 0;
        for (GridEntry entry : entries.values()) {
            used += entry.rect().width() * entry.rect().height();
        }
        return used;
    }

    public int totalCells() {
        return width * height;
    }

    public int freeCells() {
        return totalCells() - usedCells();
    }

    private void putEntry(GridEntry entry) {
        entries.put(entry.id(), entry);
        for (GridPosition cell : entry.rect().cells()) {
            occupancy[cell.x()][cell.y()] = entry.id();
        }
    }

    private void removeOccupancy(GridEntry entry) {
        for (GridPosition cell : entry.rect().cells()) {
            if (isCellInside(cell.x(), cell.y()) && entry.id().equals(occupancy[cell.x()][cell.y()])) {
                occupancy[cell.x()][cell.y()] = null;
            }
        }
    }
}
