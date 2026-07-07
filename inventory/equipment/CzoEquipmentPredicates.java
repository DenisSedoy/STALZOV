package com.czo.inventory.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

/**
 * Первичные правила совместимости предметов со слотами CZO-экипировки.
 *
 * <p>Важно: не используем SwordItem напрямую. В текущих Mojang/NeoForge mappings проекта
 * этот класс может отсутствовать/быть переименован, из-за чего компиляция падает.
 * Для мечей и кастомного ближнего оружия пока используем item id/path-проверку.</p>
 */
public final class CzoEquipmentPredicates {
    private CzoEquipmentPredicates() {
    }

    public static boolean canAccept(CzoEquipmentSlot slot, ItemStack stack) {
        if (slot == null || stack == null || stack.isEmpty()) {
            return false;
        }

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();

        return switch (slot) {
            case PRIMARY_WEAPON_1, PRIMARY_WEAPON_2 -> isPrimaryWeapon(stack, path);
            case SECONDARY_WEAPON -> isSecondaryWeapon(path);
            case MELEE_WEAPON -> isMeleeWeapon(stack, path);
            case BOLT -> containsAny(path, "bolt", "throwing_bolt");
            case THROWABLE, EXTRA_POUCH -> containsAny(path, "grenade", "throwable", "explosive", "mine", "tnt", "smoke", "flashbang");
            case BODY_ARMOR -> isBodyArmor(path);
            case HELMET -> isHelmet(path);
            case GAS_MASK -> containsAny(path, "gas_mask", "gasmask", "respirator", "gp5", "pmk");
            case EXTRA_PROTECTION -> containsAny(path, "extra_protection", "plate", "padding", "kevlar", "shield_module");
            case BACKPACK -> containsAny(path, "backpack", "rucksack", "bag", "satchel");
            case ARTIFACT_CONTAINER -> containsAny(path, "artifact_container", "artefact_container", "container_artifact");
        };
    }

    private static boolean isPrimaryWeapon(ItemStack stack, String path) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || containsAny(path, "rifle", "shotgun", "smg", "ak", "ar15", "primary_weapon");
    }

    private static boolean isSecondaryWeapon(String path) {
        return containsAny(path, "pistol", "revolver", "secondary_weapon");
    }

    private static boolean isMeleeWeapon(ItemStack stack, String path) {
        return stack.getItem() instanceof AxeItem
                || containsAny(path, "knife", "melee", "machete", "bayonet", "sword", "katana", "dagger");
    }

    private static boolean isBodyArmor(String path) {
        return containsAny(path, "chestplate", "body_armor", "bodyarmor", "vest", "bronik", "armor_vest");
    }

    private static boolean isHelmet(String path) {
        return containsAny(path, "helmet", "helm", "casque", "kaska");
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
