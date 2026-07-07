package com.czo.inventory.grid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

/** Категории для будущей автосортировки. */
public enum InventoryItemCategory {
    WEAPON(0),
    ARMOR(1),
    EQUIPMENT(2),
    ARTIFACT(3),
    AMMO(4),
    EXPLOSIVE(5),
    MEDICAL(6),
    FOOD(7),
    BLOCK(8),
    MISC(99);

    private final int sortOrder;

    InventoryItemCategory(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public static InventoryItemCategory detect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MISC;
        }

        String idPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();

        if (stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || containsAny(idPath, "gun", "pistol", "rifle", "shotgun", "smg", "weapon", "knife")) {
            return WEAPON;
        }

        if (containsAny(idPath, "armor", "vest", "helmet", "body_armor", "chestplate", "leggings", "boots")) {
            return ARMOR;
        }

        if (containsAny(idPath, "detector", "backpack", "container", "gas_mask", "respirator", "module", "attachment", "mag")) {
            return EQUIPMENT;
        }

        if (containsAny(idPath, "artifact", "artefact")) {
            return ARTIFACT;
        }

        if (containsAny(idPath, "ammo", "cartridge", "bullet", "round")) {
            return AMMO;
        }

        if (containsAny(idPath, "grenade", "explosive", "mine", "tnt")) {
            return EXPLOSIVE;
        }

        if (containsAny(idPath, "medkit", "aid", "bandage", "stim", "heal", "antirad")) {
            return MEDICAL;
        }

        if (containsAny(idPath, "bread", "apple", "food", "water", "drink", "meat", "soup", "ration", "conserve", "sausage")) {
            return FOOD;
        }

        if (stack.getItem() instanceof BlockItem) {
            return BLOCK;
        }

        return MISC;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
