package com.czo.mixin;

import com.czo.client.appearance.CzoAppearanceSelection;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla account/offline skin texture with CZO's composed skin.
 *
 * This is deliberately not a render layer. The texture is swapped before the
 * base avatar model is submitted, so the original skin is never drawn under
 * CZO clothing.
 */
@Mixin(AvatarRenderer.class)
public abstract class CzoAvatarRendererTextureMixin {
    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void czo$replaceAvatarTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(CzoAppearanceSelection.combinedTexture());
    }
}
