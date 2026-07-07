package com.czo.network;

import com.czo.CZO;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Синхронизирует выбранный hidden hotbar slot для CZO active/holster logic.
 */
public record ServerboundSetCzoSelectedSlotPacket(
        int selectedSlot
) implements CustomPacketPayload {
    public static final Type<ServerboundSetCzoSelectedSlotPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "set_czo_selected_slot"));

    public static final StreamCodec<ByteBuf, ServerboundSetCzoSelectedSlotPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundSetCzoSelectedSlotPacket::selectedSlot,
                    ServerboundSetCzoSelectedSlotPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetCzoSelectedSlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            int slot = packet.selectedSlot();
            if (slot < 0 || slot > 8) {
                return;
            }

            // CZO uses vanilla selected slot internally for active slots 1-6 and empty hand slot 9.
            player.getInventory().setSelectedSlot(slot);
            player.inventoryMenu.broadcastChanges();
        });
    }
}
