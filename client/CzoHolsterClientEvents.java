package com.czo.client;

import com.czo.CZO;
import com.czo.network.ServerboundSetCzoSelectedSlotPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Stage 7B: H убирает активное оружие/предмет из рук.
 *
 * <p>Пока CZO-экипировка хранится во временных vanilla slots, holster реализован как
 * переключение selected hotbar slot на 9-й слот. Он должен оставаться пустым и служить
 * техническим empty-hand слотом. Повторное нажатие H возвращает последний активный слот 1-6.</p>
 */
@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoHolsterClientEvents {
    private static final int ACTIVE_FIRST_SLOT = 0;
    private static final int ACTIVE_LAST_SLOT = 5;
    private static final int HOLSTER_SLOT = 8;

    private static boolean holstered;
    private static int lastActiveSlot = 0;

    private CzoHolsterClientEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_H || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!canUseCzoHolster(minecraft)) {
            return;
        }

        toggleHolster(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            holstered = false;
            return;
        }

        if (minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            holstered = false;
            return;
        }

        int selected = minecraft.player.getInventory().getSelectedSlot();
        if (!holstered && isActiveSlot(selected)) {
            lastActiveSlot = selected;
        }

        // Если игрок вручную вернулся на активные слоты 1-6, считаем оружие поднятым.
        if (holstered && isActiveSlot(selected)) {
            holstered = false;
            lastActiveSlot = selected;
        }
    }

    public static boolean isHolstered() {
        return holstered;
    }

    public static int lastActiveSlot() {
        return lastActiveSlot;
    }

    private static void toggleHolster(Minecraft minecraft) {
        Inventory inventory = minecraft.player.getInventory();
        int selected = inventory.getSelectedSlot();

        if (!holstered) {
            if (isActiveSlot(selected)) {
                lastActiveSlot = selected;
            }
            selectSlot(inventory, HOLSTER_SLOT);
            holstered = true;
            ClientPacketDistributor.sendToServer(new ServerboundSetCzoSelectedSlotPacket(HOLSTER_SLOT));
            return;
        }

        int restoreSlot = isActiveSlot(lastActiveSlot) ? lastActiveSlot : ACTIVE_FIRST_SLOT;
        selectSlot(inventory, restoreSlot);
        holstered = false;
        ClientPacketDistributor.sendToServer(new ServerboundSetCzoSelectedSlotPacket(restoreSlot));
    }

    private static void selectSlot(Inventory inventory, int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        inventory.setSelectedSlot(slot);
    }

    private static boolean canUseCzoHolster(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.screen == null
                && !minecraft.player.isCreative()
                && !minecraft.player.isSpectator();
    }

    private static boolean isActiveSlot(int slot) {
        return slot >= ACTIVE_FIRST_SLOT && slot <= ACTIVE_LAST_SLOT;
    }
}
