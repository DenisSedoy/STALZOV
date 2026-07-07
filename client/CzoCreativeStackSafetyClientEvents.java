package com.czo.client;

import com.czo.CZO;
import com.czo.inventory.grid.CzoStackCounts;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side safety net for creative stacks with large physical counts.
 *
 * <p>Some vanilla/creative packets validate ItemStack counts strictly. If creative gives a
 * real 300-count ammo stack, CZO converts it into a packet-safe physical count plus logical
 * CZO count before it can be dropped or moved through unsafe vanilla paths.</p>
 */
@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoCreativeStackSafetyClientEvents {
    private CzoCreativeStackSafetyClientEvents() {
    }

    @SubscribeEvent
    public static void normalizeCreativeStacks(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (CzoStackCounts.normalizePhysicalCount(stack)) {
                inventory.setItem(slot, stack);
            }
        }

        if (minecraft.player.containerMenu != null) {
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            if (CzoStackCounts.normalizePhysicalCount(carried)) {
                minecraft.player.containerMenu.setCarried(carried);
            }
        }
    }
}
