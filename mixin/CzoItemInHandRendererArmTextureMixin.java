package com.czo.mixin;

import com.czo.client.appearance.CzoAppearanceSelection;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * First-person arms do not always pass through AvatarRenderer#getTextureLocation.
 * This replaces the texture argument used by the vanilla first-person arm render
 * with the same composed CZO skin that third-person rendering uses.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class CzoItemInHandRendererArmTextureMixin {
    @ModifyArg(
            method = "renderPlayerArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            ),
            index = 0,
            require = 0
    )
    private Identifier czo$replaceFirstPersonArmSolidTexture(Identifier original) {
        return CzoAppearanceSelection.combinedTexture();
    }

    @ModifyArg(
            method = "renderPlayerArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            ),
            index = 0,
            require = 0
    )
    private Identifier czo$replaceFirstPersonArmTranslucentTexture(Identifier original) {
        return CzoAppearanceSelection.combinedTexture();
    }

    @ModifyArg(
            method = "renderMapHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            ),
            index = 0,
            require = 0
    )
    private Identifier czo$replaceFirstPersonMapHandSolidTexture(Identifier original) {
        return CzoAppearanceSelection.combinedTexture();
    }

    @ModifyArg(
            method = "renderMapHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            ),
            index = 0,
            require = 0
    )
    private Identifier czo$replaceFirstPersonMapHandTranslucentTexture(Identifier original) {
        return CzoAppearanceSelection.combinedTexture();
    }
}
