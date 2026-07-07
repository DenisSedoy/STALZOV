package com.czo.inventory;

import com.czo.CZO;
import com.czo.inventory.grid.CzoBackpackItemTransfer;
import com.czo.inventory.grid.CzoStackCounts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side safety net for CZO inventory data and survival HUD rules. */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoInventoryServerEvents {
    private CzoInventoryServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CzoBackpackItemTransfer.normalizePlayerInventory(player);
        disableVanillaHungerForCzoModes(player);
    }


    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        ItemStack dropped = event.getEntity().getItem();
        if (CzoStackCounts.normalizePhysicalCount(dropped)) {
            event.getEntity().setItem(dropped);
        }
    }

    private static void disableVanillaHungerForCzoModes(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() != 20) {
            foodData.setFoodLevel(20);
        }
        if (foodData.getSaturationLevel() < 20.0F) {
            foodData.setSaturation(20.0F);
        }
    }
}
