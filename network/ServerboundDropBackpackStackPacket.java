package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.grid.CzoStackCounts;
import com.czo.inventory.grid.GridInventorySlots;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Drops the whole logical stack from a CZO backpack slot. Bound to G while hovering an item. */
public record ServerboundDropBackpackStackPacket(int vanillaSlot) implements CustomPacketPayload {
    public static final Type<ServerboundDropBackpackStackPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "drop_backpack_stack"));

    public static final StreamCodec<ByteBuf, ServerboundDropBackpackStackPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundDropBackpackStackPacket::vanillaSlot,
                    ServerboundDropBackpackStackPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundDropBackpackStackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!GridInventorySlots.isPlayerBackpackSlot(packet.vanillaSlot())) {
                return;
            }

            Inventory inventory = player.getInventory();
            ItemStack stack = inventory.getItem(packet.vanillaSlot());
            if (stack.isEmpty()) {
                return;
            }

            ItemStack dropping = stack.copy();
            GridInventorySlots.clearStoredPosition(dropping);
            CzoStackCounts.normalizePhysicalCount(dropping);
            inventory.setItem(packet.vanillaSlot(), ItemStack.EMPTY);
            inventory.setChanged();
            player.drop(dropping, false);
            player.inventoryMenu.broadcastChanges();
        });
    }
}
