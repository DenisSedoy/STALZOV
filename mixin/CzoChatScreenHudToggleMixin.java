package com.czo.mixin;

import com.czo.client.hud.CzoHudVisibility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws CZO HUD hide buttons while chat is open. */
@Mixin(Screen.class)
public abstract class CzoChatScreenHudToggleMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void czo$drawHudToggleButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof ChatScreen)) {
            return;
        }
        CzoHudVisibility.renderChatToggles(graphics, Minecraft.getInstance(), mouseX, mouseY);
    }
}
