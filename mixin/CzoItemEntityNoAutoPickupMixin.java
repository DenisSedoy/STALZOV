package com.czo.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla collision pickup for survival/adventure players.
 * Items are picked up only by CZO manual F-pickup into the grid backpack.
 */
@Mixin(ItemEntity.class)
public abstract class CzoItemEntityNoAutoPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void czo$disableVanillaAutoPickup(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer && !player.isCreative() && !player.isSpectator()) {
            ci.cancel();
        }
    }
}
