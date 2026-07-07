package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.CzoInventoryBootstrap;
import com.czo.inventory.equipment.CzoEquipmentSlot;
import com.czo.inventory.equipment.CzoEquipmentSlotStorage;
import com.czo.inventory.grid.GridEntry;
import com.czo.inventory.grid.GridInventory;
import com.czo.inventory.grid.GridInventorySlots;
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

/** Снимает предмет из CZO слота экипировки в первое свободное место grid-рюкзака. */
public record ServerboundUnequipToBackpackPacket(
        int equipmentSlotOrdinal
) implements CustomPacketPayload {
    public static final Type<ServerboundUnequipToBackpackPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "unequip_to_backpack"));

    public static final StreamCodec<ByteBuf, ServerboundUnequipToBackpackPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundUnequipToBackpackPacket::equipmentSlotOrdinal,
                    ServerboundUnequipToBackpackPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundUnequipToBackpackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            CzoEquipmentSlot slot = decodeSlot(packet.equipmentSlotOrdinal());
            if (slot == null) {
                warn(player, "Неизвестный слот экипировки");
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

            ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(equipped, false);
            PlacementResult placed = grid.placeFirstFree(equipped, size, false);
            if (!placed.ok() && !ItemSize.ONE_BY_ONE.equals(size)) {
                placed = grid.placeFirstFree(equipped, ItemSize.ONE_BY_ONE, false);
            }
            if (!placed.ok() || placed.entryId() == null) {
                warn(player, "В grid-рюкзаке нет места");
                return;
            }

            GridEntry entry = grid.getEntry(placed.entryId()).orElse(null);
            if (entry == null) {
                warn(player, "Не удалось найти позицию в рюкзаке");
                return;
            }

            GridInventorySlots.setStoredPosition(equipped, entry.rect().x(), entry.rect().y(), entry.rotated());
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
