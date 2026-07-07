package com.czo.mixin;

import com.czo.client.hud.CzoHudVisibility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles clicks on CZO HUD hide buttons while chat is open. */
@Mixin(ChatScreen.class)
public abstract class CzoChatScreenHudToggleClickMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void czo$clickHudToggleButtons(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }
        if (CzoHudVisibility.handleChatClick(Minecraft.getInstance(), (int) event.x(), (int) event.y())) {
            cir.setReturnValue(true);
        }
    }
}
