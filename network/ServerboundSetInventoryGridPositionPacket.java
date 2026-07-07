package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.CzoInventoryBootstrap;
import com.czo.inventory.grid.GridInventory;
import com.czo.inventory.grid.GridInventorySlots;
import com.czo.inventory.grid.GridPosition;
import com.czo.inventory.grid.ItemSize;
import com.czo.inventory.grid.PlacementResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Серверная фиксация позиции предмета в CZO grid-инвентаре. */
public record ServerboundSetInventoryGridPositionPacket(
        int vanillaSlot,
        int gridX,
        int gridY,
        int rotatedFlag
) implements CustomPacketPayload {
    public static final Type<ServerboundSetInventoryGridPositionPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "set_inventory_grid_position"));

    public static final StreamCodec<ByteBuf, ServerboundSetInventoryGridPositionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundSetInventoryGridPositionPacket::vanillaSlot,
                    ByteBufCodecs.INT,
                    ServerboundSetInventoryGridPositionPacket::gridX,
                    ByteBufCodecs.INT,
                    ServerboundSetInventoryGridPositionPacket::gridY,
                    ByteBufCodecs.INT,
                    ServerboundSetInventoryGridPositionPacket::rotatedFlag,
                    ServerboundSetInventoryGridPositionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetInventoryGridPositionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!GridInventorySlots.isPlayerBackpackSlot(packet.vanillaSlot())) {
                return;
            }

            if (packet.gridX() < 0 || packet.gridY() < 0
                    || packet.gridX() >= GridInventorySlots.GRID_COLUMNS
                    || packet.gridY() >= GridInventorySlots.GRID_ROWS) {
                warn(player, "Нельзя положить предмет за пределы рюкзака");
                return;
            }

            Inventory inventory = player.getInventory();
            ItemStack moving = inventory.getItem(packet.vanillaSlot());
            if (moving.isEmpty()) {
                return;
            }

            boolean rotated = packet.rotatedFlag() != 0;
            ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(moving, rotated);
            Map<UUID, Integer> slotByEntryId = new HashMap<>();
            GridInventory grid = GridInventorySlots.buildPlayerBackpack(
                    inventory,
                    CzoInventoryBootstrap.ITEM_SIZES,
                    slotByEntryId
            );

            UUID ignoredEntryId = null;
            for (Map.Entry<UUID, Integer> entry : slotByEntryId.entrySet()) {
                if (entry.getValue() == packet.vanillaSlot()) {
                    ignoredEntryId = entry.getKey();
                    break;
                }
            }

            PlacementResult result = grid.canPlace(moving, new GridPosition(packet.gridX(), packet.gridY()), size, ignoredEntryId);
            if (!result.ok()) {
                warn(player, "Место занято или предмет не помещается");
                return;
            }

            GridInventorySlots.setStoredPosition(moving, packet.gridX(), packet.gridY(), rotated);
            inventory.setItem(packet.vanillaSlot(), moving);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void warn(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}
