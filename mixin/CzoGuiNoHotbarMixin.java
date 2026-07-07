package com.czo.mixin;

import com.czo.client.CzoWorldItemRaycast;
import com.czo.client.hud.CzoMinimapHudRenderer;
import com.czo.client.hud.CzoSurvivalHudRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Survival/adventure HUD overrides for CZO inventory.
 *
 * <p>Stage 7E дополнительно глушит vanilla hearts/food/xp, потому что в текущей
 * версии Minecraft часть полос рендерится отдельными internal extract-методами.</p>
 */
@Mixin(Gui.class)
public abstract class CzoGuiNoHotbarMixin {
    private static final double CZO_PICKUP_HINT_RANGE = 2.0D;
    private static final int CZO_DOT_COLOR = 0xAAFFFFFF;
    private static final int CZO_HINT_TEXT_COLOR = 0xDDFFFFFF;
    private static final int CZO_HINT_BACKDROP = 0x66000000;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractHotbar", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaHotbarAndRenderCzoHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            CzoSurvivalHudRenderer.render(graphics, minecraft);
            CzoMinimapHudRenderer.render(graphics, minecraft);
            ci.cancel();
        }
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaPlayerHealthBlock(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHealthLevel", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaHealthLevel(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractArmorLevel", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaArmorLevel(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractFoodLevel", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaFoodLevel(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractAirLevel", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaAirLevel(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaExperienceLevel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractContextualInfoBarBackground", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaContextualInfoBarBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractContextualInfoBar", at = @At("HEAD"), cancellable = true)
    private void czo$hideVanillaContextualInfoBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (isCzoSurvivalHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void czo$extractCzoCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!isCzoSurvivalHudActive() || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        ci.cancel();
        graphics.nextStratum();

        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, CZO_DOT_COLOR);

        if (CzoWorldItemRaycast.findLookedAtItem(minecraft.player, CZO_PICKUP_HINT_RANGE).isPresent()) {
            String hint = "подобрать - F";
            int textWidth = minecraft.font.width(hint);
            int x = cx - textWidth / 2;
            int y = cy + 8;
            graphics.fill(x - 4, y - 3, x + textWidth + 4, y + 10, CZO_HINT_BACKDROP);
            graphics.text(minecraft.font, hint, x, y, CZO_HINT_TEXT_COLOR, true);
        }
    }

    private boolean isCzoSurvivalHudActive() {
        return minecraft.player != null && !minecraft.player.isCreative() && !minecraft.player.isSpectator();
    }
}
