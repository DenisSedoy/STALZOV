package com.czo.inventory.grid;

import java.util.Optional;
import java.util.UUID;

/** Результат попытки положить, сдвинуть или повернуть предмет в сетке. */
public record PlacementResult(Status status, UUID entryId, String message) {
    public enum Status {
        OK,
        EMPTY_STACK,
        INVALID_SIZE,
        ITEM_REJECTED,
        OUT_OF_BOUNDS,
        COLLIDES,
        NOT_FOUND
    }

    public boolean ok() {
        return status == Status.OK;
    }

    public Optional<UUID> entryIdOptional() {
        return Optional.ofNullable(entryId);
    }

    public static PlacementResult ok(UUID entryId) {
        return new PlacementResult(Status.OK, entryId, "ok");
    }

    public static PlacementResult fail(Status status, String message) {
        if (status == Status.OK) {
            throw new IllegalArgumentException("Use PlacementResult.ok for successful result");
        }
        return new PlacementResult(status, null, message);
    }
}
