package com.czo.client;

import com.czo.item.gun.GunItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public record AimingItemModelProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<AimingItemModelProperty> MAP_CODEC =
            MapCodec.unit(new AimingItemModelProperty());

    @Override
    public boolean get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity entity,
            int seed,
            ItemDisplayContext context
    ) {
        if (!(stack.getItem() instanceof GunItem)) {
            return false;
        }

        if (!(entity instanceof Player player)) {
            return false;
        }

        boolean firstPerson =
                context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        if (!firstPerson) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        return player == minecraft.player
                && minecraft.options.keyUse.isDown();
    }

    @Override
    public MapCodec<AimingItemModelProperty> type() {
        return MAP_CODEC;
    }
}
