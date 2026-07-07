package com.czo.inventory.equipment;

import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Хранилище экипировки персонажа.
 *
 * <p>Это не GUI и не capability/attachment. Это чистая модель данных, которую позже можно
 * подключить к NeoForge attachments/data components и синхронизации.</p>
 */
public final class PlayerEquipmentInventory {
    private final EnumMap<CzoEquipmentSlot, ItemStack> stacks = new EnumMap<>(CzoEquipmentSlot.class);

    public PlayerEquipmentInventory() {
        for (CzoEquipmentSlot slot : CzoEquipmentSlot.values()) {
            stacks.put(slot, ItemStack.EMPTY);
        }
    }

    public ItemStack get(CzoEquipmentSlot slot) {
        ItemStack stack = stacks.get(slot);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public boolean isEmpty(CzoEquipmentSlot slot) {
        return get(slot).isEmpty();
    }

    public Map<CzoEquipmentSlot, ItemStack> copyView() {
        EnumMap<CzoEquipmentSlot, ItemStack> copy = new EnumMap<>(CzoEquipmentSlot.class);
        for (Map.Entry<CzoEquipmentSlot, ItemStack> entry : stacks.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    public EquipmentChangeResult equip(CzoEquipmentSlot slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.EMPTY_STACK, "Cannot equip empty stack");
        }
        if (!slot.canAccept(stack)) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.SLOT_REJECTED, "This item cannot be placed into this equipment slot");
        }
        if (!isEmpty(slot)) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.SLOT_OCCUPIED, "Equipment slot is already occupied");
        }
        stacks.put(slot, stack.copy());
        return EquipmentChangeResult.success();
    }

    /** Заменяет предмет в слоте и возвращает старый предмет, если он был. */
    public Optional<ItemStack> set(CzoEquipmentSlot slot, ItemStack stack) {
        ItemStack previous = stacks.getOrDefault(slot, ItemStack.EMPTY).copy();
        stacks.put(slot, stack == null ? ItemStack.EMPTY : stack.copy());
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    public Optional<ItemStack> unequip(CzoEquipmentSlot slot) {
        ItemStack previous = stacks.getOrDefault(slot, ItemStack.EMPTY).copy();
        stacks.put(slot, ItemStack.EMPTY);
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    public EquipmentChangeResult move(CzoEquipmentSlot from, CzoEquipmentSlot to) {
        ItemStack moving = get(from);
        if (moving.isEmpty()) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.SLOT_EMPTY, "Source slot is empty");
        }
        if (!to.canAccept(moving)) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.SLOT_REJECTED, "Target slot rejects this item");
        }
        if (!isEmpty(to)) {
            return EquipmentChangeResult.fail(EquipmentChangeResult.Status.SLOT_OCCUPIED, "Target slot is occupied");
        }
        stacks.put(from, ItemStack.EMPTY);
        stacks.put(to, moving.copy());
        return EquipmentChangeResult.success();
    }
}
