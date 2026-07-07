package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.equipment.CzoEquipmentSlot;
import com.czo.inventory.equipment.CzoEquipmentSlotStorage;
import com.czo.inventory.grid.GridInventorySlots;

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

/** Кладёт предмет из grid-рюкзака в выбранный CZO слот экипировки. */
public record ServerboundEquipFromInventoryPacket(
        int equipmentSlotOrdinal,
        int sourceVanillaSlot
) implements CustomPacketPayload {
    public static final Type<ServerboundEquipFromInventoryPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "equip_from_inventory"));

    public static final StreamCodec<ByteBuf, ServerboundEquipFromInventoryPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundEquipFromInventoryPacket::equipmentSlotOrdinal,
                    ByteBufCodecs.INT,
                    ServerboundEquipFromInventoryPacket::sourceVanillaSlot,
                    ServerboundEquipFromInventoryPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundEquipFromInventoryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            CzoEquipmentSlot slot = decodeSlot(packet.equipmentSlotOrdinal());
            if (slot == null) {
                warn(player, "Неизвестный слот экипировки");
                return;
            }

            if (!GridInventorySlots.isPlayerBackpackSlot(packet.sourceVanillaSlot())) {
                warn(player, "Этот предмет не лежит в рюкзаке");
                return;
            }

            Inventory inventory = player.getInventory();
            ItemStack moving = inventory.getItem(packet.sourceVanillaSlot());
            if (moving.isEmpty()) {
                return;
            }

            if (!slot.canAccept(moving)) {
                warn(player, "Предмет не подходит в слот: " + CzoEquipmentSlotStorage.displayName(slot));
                return;
            }

            int oldX = GridInventorySlots.gridX(moving);
            int oldY = GridInventorySlots.gridY(moving);
            boolean oldRotated = GridInventorySlots.rotated(moving);

            ItemStack previous = CzoEquipmentSlotStorage.get(player, slot).copy();
            ItemStack equipped = moving.copy();
            GridInventorySlots.clearStoredPosition(equipped);
            CzoEquipmentSlotStorage.set(player, slot, equipped);

            if (previous.isEmpty()) {
                inventory.setItem(packet.sourceVanillaSlot(), ItemStack.EMPTY);
            } else {
                GridInventorySlots.setStoredPosition(previous, oldX, oldY, oldRotated);
                inventory.setItem(packet.sourceVanillaSlot(), previous);
            }

            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
        });
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
