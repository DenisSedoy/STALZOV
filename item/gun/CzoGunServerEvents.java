package com.czo.item.gun;

import com.czo.CZO;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(
        modid = CZO.MODID
)
public final class CzoGunServerEvents {
    private CzoGunServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();

        // 1. Тикаем перезарядку только у оружия в основной руке.
        if (mainHandStack.getItem() instanceof GunItem gunItem) {
            gunItem.serverTickReload(player, mainHandStack);
        }

        // 2. Всё оружие, которое перезаряжается, но больше НЕ в основной руке — сбрасываем.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

            if (stack == mainHandStack) {
                continue;
            }

            if (stack.getItem() instanceof GunItem gunItem && gunItem.isReloading(stack)) {
                gunItem.cancelReload(player, stack);
            }
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ItemStack stack = event.getEntity().getItem();

        if (stack.getItem() instanceof GunItem gunItem) {
            gunItem.cancelReload(stack);
        }
    }
}