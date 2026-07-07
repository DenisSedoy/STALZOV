package com.czo.client;

import com.czo.CZO;
import com.czo.client.screen.AmmoSelectionScreen;
import com.czo.client.screen.GunModificationScreen;
import com.czo.item.gun.GunItem;
import com.czo.network.ServerboundFireGunPacket;
import com.czo.network.ServerboundReloadGunPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoClientGameEvents {
    private static final int RELOAD_WHEEL_HOLD_TICKS = 8;
    private static final Identifier AIMING_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(CZO.MODID, "aiming_speed_penalty");
    private static final double AIMING_SPEED_PENALTY = -0.10D;

    private static boolean reloadKeyDown;
    private static boolean reloadWheelOpened;
    private static long reloadKeyDownTick;

    private CzoClientGameEvents() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (!(minecraft.player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }

        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            ClientPacketDistributor.sendToServer(ServerboundFireGunPacket.INSTANCE);
        }

        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (event.getKey() == GLFW.GLFW_KEY_R) {
            handleReloadKey(event, minecraft);
            return;
        }

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (!(minecraft.player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_M) {
            minecraft.setScreen(new GunModificationScreen());
        }
    }

    private static void handleReloadKey(InputEvent.Key event, Minecraft minecraft) {
        if (minecraft.player == null) {
            resetReloadKeyState();
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (minecraft.screen != null) {
                return;
            }

            if (!(minecraft.player.getMainHandItem().getItem() instanceof GunItem)) {
                return;
            }

            reloadKeyDown = true;
            reloadWheelOpened = false;
            reloadKeyDownTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
            return;
        }

        if (event.getAction() == GLFW.GLFW_RELEASE) {
            boolean ammoWheelWasOpen = minecraft.screen instanceof AmmoSelectionScreen;
            boolean shouldReload = reloadKeyDown && !reloadWheelOpened && minecraft.screen == null;
            resetReloadKeyState();

            if (ammoWheelWasOpen) {
                minecraft.setScreen(null);
                return;
            }

            if (!shouldReload) {
                return;
            }

            if (minecraft.player.getMainHandItem().getItem() instanceof GunItem) {
                ClientPacketDistributor.sendToServer(ServerboundReloadGunPacket.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        updateAimingSpeedModifier(minecraft);

        // Закрытие колеса выбора патронов делаем через InputEvent.Key/GLFW_RELEASE.
        // В NeoForge 26.1.2 у Window нет minecraft.getWindow().getWindow(),
        // поэтому не опрашиваем GLFW напрямую из тика.
        if (!reloadKeyDown || reloadWheelOpened) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem)) {
            resetReloadKeyState();
            return;
        }

        long heldTicks = minecraft.level.getGameTime() - reloadKeyDownTick;

        if (heldTicks >= RELOAD_WHEEL_HOLD_TICKS) {
            reloadWheelOpened = true;
            minecraft.setScreen(new AmmoSelectionScreen());
        }
    }

    private static void updateAimingSpeedModifier(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        AttributeInstance movementSpeed = minecraft.player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed == null) {
            return;
        }

        boolean shouldSlowDown =
                minecraft.player.getMainHandItem().getItem() instanceof GunItem
                        && minecraft.options.keyUse.isDown();

        if (shouldSlowDown) {
            if (!movementSpeed.hasModifier(AIMING_SPEED_MODIFIER_ID)) {
                movementSpeed.addTransientModifier(
                        new AttributeModifier(
                                AIMING_SPEED_MODIFIER_ID,
                                AIMING_SPEED_PENALTY,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )
                );
            }

            return;
        }

        if (movementSpeed.hasModifier(AIMING_SPEED_MODIFIER_ID)) {
            movementSpeed.removeModifier(AIMING_SPEED_MODIFIER_ID);
        }
    }

    private static void resetReloadKeyState() {
        reloadKeyDown = false;
        reloadWheelOpened = false;
        reloadKeyDownTick = 0L;
    }

    @SubscribeEvent
    public static void onRenderGunHud(RenderGuiLayerEvent.Post event) {
        // Stage 7F: старый текстовый gun HUD отключён.
        // Боезапас, тип патрона и иконка патрона теперь рисуются в CzoSurvivalHudRenderer.
    }


    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();

        boolean holdingGun = player.getMainHandItem().getItem() instanceof GunItem;

        if (!holdingGun) {
            return;
        }

        boolean aiming = minecraft.options.keyUse.isDown();
        boolean holdingBreath = aiming && player.isShiftKeyDown();

        if (holdingBreath) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.84F);
        } else if (aiming) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.92F);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        boolean holdingGun =
                minecraft.player.getMainHandItem().getItem() instanceof GunItem
                        || minecraft.player.getOffhandItem().getItem() instanceof GunItem;

        if (!holdingGun) {
            return;
        }

        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }
}
