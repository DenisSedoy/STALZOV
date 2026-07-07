package com.czo.network;

import com.czo.CZO;
import com.czo.item.gun.GunItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundSetSelectedAmmoPacket(
        String ammoId
) implements CustomPacketPayload {
    public static final Type<ServerboundSetSelectedAmmoPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "set_selected_ammo"));

    public static final StreamCodec<ByteBuf, ServerboundSetSelectedAmmoPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ServerboundSetSelectedAmmoPacket::ammoId,
                    ServerboundSetSelectedAmmoPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetSelectedAmmoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() instanceof GunItem gunItem) {
                gunItem.setSelectedAmmo(player, stack, packet.ammoId());
            }
        });
    }
}
