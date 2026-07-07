package com.czo.inventory.equipment;

/** Результат операции экипировки/снятия предмета. */
public record EquipmentChangeResult(Status status, String message) {
    public enum Status {
        OK,
        EMPTY_STACK,
        SLOT_REJECTED,
        SLOT_OCCUPIED,
        SLOT_EMPTY
    }

    public boolean ok() {
        return status == Status.OK;
    }

    public boolean failed() {
        return status != Status.OK;
    }

    /**
     * ВАЖНО: метод специально называется success(), а не ok().
     *
     * У record в Java автоматически создаётся accessor с именем компонента.
     * Если record когда-нибудь объявлен как EquipmentChangeResult(boolean ok, ...),
     * то метод ok() уже существует, и static ok() ломает компиляцию.
     */
    public static EquipmentChangeResult success() {
        return new EquipmentChangeResult(Status.OK, "ok");
    }

    public static EquipmentChangeResult fail(Status status, String message) {
        if (status == Status.OK) {
            throw new IllegalArgumentException("Use success() for successful result");
        }
        return new EquipmentChangeResult(status, message);
    }
}
