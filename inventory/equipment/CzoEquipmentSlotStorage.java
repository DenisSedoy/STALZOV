package com.czo.inventory.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Временный storage-мост для CZO слотов экипировки.
 *
 * <p>На полноценном этапе эти слоты переедут в отдельное player attachment/capability.
 * Сейчас нам важно получить рабочую механику: предмет из grid-рюкзака можно экипировать,
 * а экипированный предмет можно снять обратно в grid-рюкзак.</p>
 */
public final class CzoEquipmentSlotStorage {
    private CzoEquipmentSlotStorage() {
    }

    /** Первые 10 inventory slots считаются CZO-экипировкой/резервом, а не рюкзаком. */
    public static final int BACKPACK_FIRST_SLOT = 10;

    public static ItemStack get(Player player, CzoEquipmentSlot slot) {
        if (player == null || slot == null) {
            return ItemStack.EMPTY;
        }

        EquipmentSlot vanillaSlot = vanillaEquipmentSlot(slot);
        if (vanillaSlot != null) {
            return player.getItemBySlot(vanillaSlot);
        }

        int inventorySlot = mappedInventorySlot(slot);
        if (inventorySlot >= 0) {
            Inventory inventory = player.getInventory();
            if (inventorySlot < inventory.getContainerSize()) {
                return inventory.getItem(inventorySlot);
            }
        }

        return ItemStack.EMPTY;
    }

    public static void set(Player player, CzoEquipmentSlot slot, ItemStack stack) {
        if (player == null || slot == null) {
            return;
        }

        ItemStack value = stack == null ? ItemStack.EMPTY : stack;
        EquipmentSlot vanillaSlot = vanillaEquipmentSlot(slot);
        if (vanillaSlot != null) {
            player.setItemSlot(vanillaSlot, value);
            return;
        }

        int inventorySlot = mappedInventorySlot(slot);
        if (inventorySlot >= 0) {
            Inventory inventory = player.getInventory();
            if (inventorySlot < inventory.getContainerSize()) {
                inventory.setItem(inventorySlot, value);
                inventory.setChanged();
            }
        }
    }

    public static boolean isEmpty(Player player, CzoEquipmentSlot slot) {
        return get(player, slot).isEmpty();
    }

    /** Возвращает vanilla inventory slot, если CZO slot временно хранится в inventory. */
    public static int mappedInventorySlot(CzoEquipmentSlot slot) {
        if (slot == null) {
            return -1;
        }
        return switch (slot) {
            case PRIMARY_WEAPON_1 -> 0;
            case PRIMARY_WEAPON_2 -> 1;
            case SECONDARY_WEAPON -> 2;
            case MELEE_WEAPON -> 3;
            case BOLT -> 4;
            case THROWABLE -> 5;
            case GAS_MASK -> 6;
            case BACKPACK -> 7;
            case ARTIFACT_CONTAINER -> 8;
            case EXTRA_POUCH -> 9;
            default -> -1;
        };
    }

    /** Возвращает настоящий vanilla armor/body slot, если он подходит под CZO slot. */
    public static EquipmentSlot vanillaEquipmentSlot(CzoEquipmentSlot slot) {
        if (slot == null) {
            return null;
        }
        return switch (slot) {
            case HELMET -> EquipmentSlot.HEAD;
            case BODY_ARMOR -> EquipmentSlot.CHEST;
            case EXTRA_PROTECTION -> EquipmentSlot.LEGS;
            default -> null;
        };
    }

    public static boolean isInventoryBackedEquipmentSlot(int vanillaSlot) {
        return vanillaSlot >= 0 && vanillaSlot < BACKPACK_FIRST_SLOT;
    }

    public static String displayName(CzoEquipmentSlot slot) {
        if (slot == null) {
            return "слот";
        }
        return switch (slot) {
            case PRIMARY_WEAPON_1 -> "оружие в руках";
            case PRIMARY_WEAPON_2 -> "оружие на ремне";
            case SECONDARY_WEAPON -> "кобура";
            case MELEE_WEAPON -> "ножны";
            case BOLT -> "подсумок";
            case THROWABLE -> "подсумок";
            case EXTRA_POUCH -> "подсумок";
            case BODY_ARMOR -> "тело";
            case HELMET -> "голова";
            case GAS_MASK -> "лицо";
            case EXTRA_PROTECTION -> "конечности";
            case BACKPACK -> "спина";
            case ARTIFACT_CONTAINER -> "пояс";
        };
    }
}
