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

public record ServerboundSetGunModulePacket(
        String slotId,
        String moduleId
) implements CustomPacketPayload {
    public static final Type<ServerboundSetGunModulePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "set_gun_module"));

    public static final StreamCodec<ByteBuf, ServerboundSetGunModulePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ServerboundSetGunModulePacket::slotId,
                    ByteBufCodecs.STRING_UTF8,
                    ServerboundSetGunModulePacket::moduleId,
                    ServerboundSetGunModulePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetGunModulePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() instanceof GunItem gunItem) {
                gunItem.installModule(player, stack, packet.slotId(), packet.moduleId());
            }
        });
    }
}