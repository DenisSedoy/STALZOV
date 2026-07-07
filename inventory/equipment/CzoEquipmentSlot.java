package com.czo.inventory.equipment;

import net.minecraft.world.item.ItemStack;

/**
 * Слоты персонажа для STALKER/EFT-инвентаря.
 * Активные слоты могут быть привязаны к клавишам 1-6.
 */
public enum CzoEquipmentSlot {
    PRIMARY_WEAPON_1(true, 1, "slot.czo.primary_weapon_1"),
    PRIMARY_WEAPON_2(true, 2, "slot.czo.primary_weapon_2"),
    SECONDARY_WEAPON(true, 3, "slot.czo.secondary_weapon"),
    MELEE_WEAPON(true, 4, "slot.czo.melee_weapon"),
    BOLT(true, 5, "slot.czo.bolt"),
    THROWABLE(true, 6, "slot.czo.throwable"),

    BODY_ARMOR(false, -1, "slot.czo.body_armor"),
    HELMET(false, -1, "slot.czo.helmet"),
    GAS_MASK(false, -1, "slot.czo.gas_mask"),
    EXTRA_PROTECTION(false, -1, "slot.czo.extra_protection"),
    BACKPACK(false, -1, "slot.czo.backpack"),
    ARTIFACT_CONTAINER(false, -1, "slot.czo.artifact_container"),

    /** Дополнительный подсумок. Добавлен в конец enum, чтобы не сдвигать старые ordinal-ы. */
    EXTRA_POUCH(true, 6, "slot.czo.extra_pouch");

    private final boolean active;
    private final int hotbarKey;
    private final String translationKey;

    CzoEquipmentSlot(boolean active, int hotbarKey, String translationKey) {
        this.active = active;
        this.hotbarKey = hotbarKey;
        this.translationKey = translationKey;
    }

    public boolean active() {
        return active;
    }

    public boolean passive() {
        return !active;
    }

    public int hotbarKey() {
        return hotbarKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean canAccept(ItemStack stack) {
        return CzoEquipmentPredicates.canAccept(this, stack);
    }
}
