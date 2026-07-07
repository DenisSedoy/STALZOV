package com.czo.network;

import com.czo.CZO;
import com.czo.item.gun.GunItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundFireGunPacket() implements CustomPacketPayload {
    public static final ServerboundFireGunPacket INSTANCE = new ServerboundFireGunPacket();

    public static final Type<ServerboundFireGunPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "fire_gun"));

    public static final StreamCodec<ByteBuf, ServerboundFireGunPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundFireGunPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() instanceof GunItem gunItem) {
                gunItem.tryFire(player, stack);
            }
        });
    }
}