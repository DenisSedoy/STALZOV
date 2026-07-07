package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.CzoInventoryBootstrap;
import com.czo.inventory.equipment.CzoEquipmentSlot;
import com.czo.inventory.equipment.CzoEquipmentSlotStorage;
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

/** Снимает предмет из CZO-слота экипировки в конкретную клетку grid-рюкзака. */
public record ServerboundUnequipToGridPositionPacket(
        int equipmentSlotOrdinal,
        int gridX,
        int gridY,
        int rotatedFlag
) implements CustomPacketPayload {
    public static final Type<ServerboundUnequipToGridPositionPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "unequip_to_grid_position"));

    public static final StreamCodec<ByteBuf, ServerboundUnequipToGridPositionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundUnequipToGridPositionPacket::equipmentSlotOrdinal,
                    ByteBufCodecs.INT,
                    ServerboundUnequipToGridPositionPacket::gridX,
                    ByteBufCodecs.INT,
                    ServerboundUnequipToGridPositionPacket::gridY,
                    ByteBufCodecs.INT,
                    ServerboundUnequipToGridPositionPacket::rotatedFlag,
                    ServerboundUnequipToGridPositionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundUnequipToGridPositionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            CzoEquipmentSlot slot = decodeSlot(packet.equipmentSlotOrdinal());
            if (slot == null) {
                warn(player, "Неизвестный слот экипировки");
                return;
            }

            if (packet.gridX() < 0 || packet.gridY() < 0
                    || packet.gridX() >= GridInventorySlots.GRID_COLUMNS
                    || packet.gridY() >= GridInventorySlots.GRID_ROWS) {
                warn(player, "Нельзя снять предмет за пределы рюкзака");
                return;
            }

            ItemStack equipped = CzoEquipmentSlotStorage.get(player, slot).copy();
            if (equipped.isEmpty()) {
                return;
            }

            Inventory inventory = player.getInventory();
            int targetVanillaSlot = firstEmptyBackpackSlot(inventory);
            if (targetVanillaSlot < 0) {
                warn(player, "В рюкзаке нет свободного vanilla-слота");
                return;
            }

            Map<UUID, Integer> slotByEntryId = new HashMap<>();
            GridInventory grid = GridInventorySlots.buildPlayerBackpack(
                    inventory,
                    CzoInventoryBootstrap.ITEM_SIZES,
                    slotByEntryId
            );

            boolean rotated = packet.rotatedFlag() != 0;
            ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(equipped, rotated);
            PlacementResult result = grid.canPlace(equipped, new GridPosition(packet.gridX(), packet.gridY()), size);
            if (!result.ok()) {
                warn(player, "Место занято или предмет не помещается");
                return;
            }

            GridInventorySlots.setStoredPosition(equipped, packet.gridX(), packet.gridY(), rotated);
            inventory.setItem(targetVanillaSlot, equipped);
            CzoEquipmentSlotStorage.set(player, slot, ItemStack.EMPTY);

            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static int firstEmptyBackpackSlot(Inventory inventory) {
        for (int slot = GridInventorySlots.FIRST_PLAYER_SLOT; slot <= GridInventorySlots.LAST_PLAYER_SLOT; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static CzoEquipmentSlot decodeSlot(int ordinal) {
        CzoEquipmentSlot[] values = CzoEquipmentSlot.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    private static void warn(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}
