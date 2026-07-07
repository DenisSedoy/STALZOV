package com.czo.client;

import com.czo.CZO;
import com.czo.client.screen.inventory.CzoInventoryScreen;
import com.czo.network.ServerboundManualPickupItemPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Клиентский мост для нового CZO-инвентаря.
 *
 * <p>Survival/adventure: ванильный экран инвентаря заменяется на CZO HUD.
 * Stage 6: F больше не является автоподбором/хотбар-действием — F вручную подбирает предмет,
 * на который игрок смотрит в радиусе 2 блоков, и отправляет его в CZO-рюкзак.</p>
 */
@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoInventoryClientEvents {
    private static final double MANUAL_PICKUP_RANGE = 2.0D;

    private CzoInventoryClientEvents() {
    }

    @SubscribeEvent
    public static void handleManualItemPickup(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        // Creative/spectator оставляем ванильными.
        if (minecraft.player.hasInfiniteMaterials() || minecraft.player.isSpectator()) {
            return;
        }

        // Забираем F у vanilla swap-offhand, чтобы F стал кнопкой ручного подбора предметов.
        while (minecraft.options.keySwapOffhand.consumeClick()) {
            ItemEntity target = CzoWorldItemRaycast.findLookedAtItem(minecraft.player, MANUAL_PICKUP_RANGE).orElse(null);
            if (target != null) {
                ClientPacketDistributor.sendToServer(new ServerboundManualPickupItemPacket(target.getId()));
            }
        }
    }

    @SubscribeEvent
    public static void replaceVanillaSurvivalInventory(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        // Creative пока оставляем ванильным. Отдельная кнопка 32x32 возле корзины пойдёт следующим этапом.
        if (minecraft.player.hasInfiniteMaterials()) {
            return;
        }

        if (minecraft.screen != null && minecraft.screen.getClass() == InventoryScreen.class) {
            minecraft.setScreen(new CzoInventoryScreen());
        }
    }
}
