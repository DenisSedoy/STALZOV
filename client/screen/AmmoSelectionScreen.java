package com.czo.client.screen;

import com.czo.item.gun.GunItem;
import com.czo.item.gun.ammo.CzoAmmo;
import com.czo.item.gun.ammo.CzoAmmoDefinition;
import com.czo.item.gun.definition.CzoGuns;
import com.czo.item.gun.definition.GunDefinition;
import com.czo.network.ServerboundSetSelectedAmmoPacket;
import com.czo.registry.CzoDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AmmoSelectionScreen extends Screen {
    private static final int COLOR_BACKGROUND = 0x88000000;
    private static final int COLOR_PANEL = 0xCC080808;
    private static final int COLOR_PANEL_HOVER = 0xDD1A1A10;
    private static final int COLOR_OUTLINE = 0xFF777777;
    private static final int COLOR_ACTIVE = 0xFF66FF66;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFFAAAAAA;
    private static final int COLOR_WARNING = 0xFFFFCC55;

    private final List<AmmoButton> buttons = new ArrayList<>();

    // Экран выбора патронов остаётся Screen, чтобы можно было выбирать патроны мышью.
    // Но пока он открыт, мы вручную синхронизируем movement key mappings,
    // чтобы WASD/прыжок/присед/спринт не глохли из-за GUI.
    private static long cachedWindowHandle = -1L;

    public AmmoSelectionScreen() {
        super(
                Minecraft.getInstance(),
                Minecraft.getInstance().font,
                Component.literal("Выбор патронов")
        );

        // В этой версии Screen нет поля passEvents.
        // Движение пробрасываем вручную через KeyMapping#setDown(...) ниже.
    }

    @Override
    public void tick() {
        super.tick();
        syncMovementKeysWhileOpen();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.buttons.clear();

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            drawCenteredText(graphics, "Нет игрока", this.height / 2, 0xFFFF5555);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem gunItem)) {
            drawCenteredText(graphics, "В руках нет оружия", this.height / 2, 0xFFFF5555);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        GunDefinition gun = getGunDefinition(stack);
        List<CzoAmmoDefinition> ammoList = CzoAmmo.getCompatibleAmmo(gun);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        drawCenteredText(graphics, "ВЫБОР ПАТРОНОВ", centerY - 76, COLOR_TEXT);
        drawCenteredText(graphics, "ЛКМ — выбрать   |   отпусти R — закрыть", centerY - 60, COLOR_MUTED);

        String selectedAmmoId = gunItem.getSelectedAmmoId(stack);
        drawCenteredText(graphics, "Текущий: " + gunItem.getSelectedAmmoDisplayName(stack), centerY - 38, COLOR_WARNING);

        int buttonWidth = 104;
        int buttonHeight = 42;
        double radiusX = 116.0D;
        double radiusY = 70.0D;

        List<CzoAmmoDefinition> availableAmmoList = new ArrayList<>();

        for (CzoAmmoDefinition ammo : ammoList) {
            int count = countAmmo(minecraft.player, ammo.id());

            if (count > 0 || ammo.id().equals(selectedAmmoId)) {
                availableAmmoList.add(ammo);
            }
        }

        for (int i = 0; i < availableAmmoList.size(); i++) {
            CzoAmmoDefinition ammo = availableAmmoList.get(i);
            double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * i / Math.max(1, availableAmmoList.size()));

            int x = centerX + (int) Math.round(Math.cos(angle) * radiusX) - buttonWidth / 2;
            int y = centerY + (int) Math.round(Math.sin(angle) * radiusY) - buttonHeight / 2;

            int count = countAmmo(minecraft.player, ammo.id());
            boolean hovered = contains(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
            boolean active = ammo.id().equals(selectedAmmoId);

            this.buttons.add(new AmmoButton(ammo.id(), x, y, buttonWidth, buttonHeight, count));

            int panelColor = hovered ? COLOR_PANEL_HOVER : COLOR_PANEL;
            graphics.fill(x, y, x + buttonWidth, y + buttonHeight, panelColor);

            int outlineColor;
            if (active) {
                outlineColor = COLOR_ACTIVE;
            } else if (hovered) {
                outlineColor = COLOR_WARNING;
            } else {
                outlineColor = COLOR_OUTLINE;
            }

            drawOutline(graphics, x, y, buttonWidth, buttonHeight, outlineColor);

            drawText(graphics, ammo.displayName(), x + 8, y + 8, active ? COLOR_ACTIVE : COLOR_TEXT);
            drawText(graphics, count + " шт.", x + 8, y + 22, count > 0 ? COLOR_MUTED : 0xFFFF7777);
        }

        if (availableAmmoList.isEmpty()) {
            drawCenteredText(graphics, "В инвентаре нет подходящих патронов", centerY + 16, 0xFFFF7777);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int countAmmo(Player player, String ammoId) {
        int total = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (itemId != null && itemId.toString().equals(ammoId)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private GunDefinition getGunDefinition(ItemStack stack) {
        String gunId = stack.get(CzoDataComponents.GUN_ID.get());

        if (gunId == null || gunId.isBlank()) {
            return CzoGuns.get(CzoGuns.PM);
        }

        return CzoGuns.get(gunId);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        for (AmmoButton button : buttons) {
            if (contains(button.x, button.y, button.w, button.h, mouseX, mouseY)) {
                if (button.count <= 0) {
                    return true;
                }

                ClientPacketDistributor.sendToServer(new ServerboundSetSelectedAmmoPacket(button.ammoId));
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (syncMovementKeyFromEvent(event.key(), true)) {
            // Не считаем клавишу полностью съеденной экраном.
            // Так движение меньше конфликтует с обычным клиентским input.
            return false;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R) {
            this.onClose();
            return true;
        }

        if (syncMovementKeyFromEvent(event.key(), false)) {
            return false;
        }

        return super.keyReleased(event);
    }

    private void syncMovementKeysWhileOpen() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.options == null) {
            return;
        }

        long windowHandle = getWindowHandle(minecraft);

        if (windowHandle == 0L) {
            return;
        }

        syncMovementKey(minecraft.options.keyUp, windowHandle);
        syncMovementKey(minecraft.options.keyDown, windowHandle);
        syncMovementKey(minecraft.options.keyLeft, windowHandle);
        syncMovementKey(minecraft.options.keyRight, windowHandle);
        syncMovementKey(minecraft.options.keyJump, windowHandle);
        syncMovementKey(minecraft.options.keyShift, windowHandle);
        syncMovementKey(minecraft.options.keySprint, windowHandle);
    }

    private boolean syncMovementKeyFromEvent(int keyCode, boolean down) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options == null) {
            return false;
        }

        boolean matched = false;

        matched |= syncMovementKeyFromEvent(minecraft.options.keyUp, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keyDown, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keyLeft, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keyRight, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keyJump, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keyShift, keyCode, down);
        matched |= syncMovementKeyFromEvent(minecraft.options.keySprint, keyCode, down);

        return matched;
    }

    private boolean syncMovementKeyFromEvent(KeyMapping keyMapping, int keyCode, boolean down) {
        int mappedKeyCode = getMappedKeyCode(keyMapping);

        if (mappedKeyCode != keyCode) {
            return false;
        }

        keyMapping.setDown(down);
        return true;
    }

    private void syncMovementKey(KeyMapping keyMapping, long windowHandle) {
        int keyCode = getMappedKeyCode(keyMapping);

        if (keyCode <= 0) {
            return;
        }

        keyMapping.setDown(GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS);
    }

    private int getMappedKeyCode(KeyMapping keyMapping) {
        try {
            Method getKeyMethod = keyMapping.getClass().getMethod("getKey");
            Object key = getKeyMethod.invoke(keyMapping);

            Method getValueMethod = key.getClass().getMethod("getValue");
            Object value = getValueMethod.invoke(key);

            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // На случай смены mappings просто не синхронизируем эту кнопку.
        }

        return -1;
    }

    private long getWindowHandle(Minecraft minecraft) {
        if (cachedWindowHandle > 0L) {
            return cachedWindowHandle;
        }

        Object window = minecraft.getWindow();

        if (window == null) {
            return 0L;
        }

        String[] methodNames = {
                "getWindow",
                "getWindowHandle",
                "getHandle",
                "window",
                "handle"
        };

        for (String methodName : methodNames) {
            try {
                Method method = window.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                Object value = method.invoke(window);

                if (value instanceof Number number) {
                    cachedWindowHandle = number.longValue();
                    return cachedWindowHandle;
                }
            } catch (ReflectiveOperationException ignored) {
                // Пробуем следующий вариант имени.
            }
        }

        String[] fieldNames = {
                "window",
                "handle",
                "windowHandle"
        };

        for (String fieldName : fieldNames) {
            try {
                Field field = window.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(window);

                if (value instanceof Number number) {
                    cachedWindowHandle = number.longValue();
                    return cachedWindowHandle;
                }
            } catch (ReflectiveOperationException ignored) {
                // Пробуем следующее имя поля.
            }
        }

        for (Field field : window.getClass().getDeclaredFields()) {
            if (field.getType() != long.class && field.getType() != Long.TYPE) {
                continue;
            }

            try {
                field.setAccessible(true);
                cachedWindowHandle = field.getLong(window);
                return cachedWindowHandle;
            } catch (ReflectiveOperationException ignored) {
                // Ничего страшного, просто не смогли найти handle.
            }
        }

        return 0L;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.text(this.font, Component.literal(text), x, y, color, true);
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String text, int y, int color) {
        drawText(graphics, text, this.width / 2 - this.font.width(text) / 2, y, color);
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private boolean contains(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    private record AmmoButton(String ammoId, int x, int y, int w, int h, int count) {
    }
}
