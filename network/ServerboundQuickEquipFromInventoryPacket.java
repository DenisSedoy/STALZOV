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

/** Быстро надевает предмет из grid-рюкзака в первый подходящий свободный CZO-слот. */
public record ServerboundQuickEquipFromInventoryPacket(
        int sourceVanillaSlot
) implements CustomPacketPayload {
    public static final Type<ServerboundQuickEquipFromInventoryPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "quick_equip_from_inventory"));

    public static final StreamCodec<ByteBuf, ServerboundQuickEquipFromInventoryPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundQuickEquipFromInventoryPacket::sourceVanillaSlot,
                    ServerboundQuickEquipFromInventoryPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundQuickEquipFromInventoryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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

            CzoEquipmentSlot target = firstCompatibleEmptySlot(player, moving);
            if (target == null) {
                warn(player, "Нет подходящего свободного слота экипировки");
                return;
            }

            ItemStack equipped = moving.copy();
            GridInventorySlots.clearStoredPosition(equipped);
            CzoEquipmentSlotStorage.set(player, target, equipped);
            inventory.setItem(packet.sourceVanillaSlot(), ItemStack.EMPTY);

            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static CzoEquipmentSlot firstCompatibleEmptySlot(ServerPlayer player, ItemStack stack) {
        for (CzoEquipmentSlot slot : quickEquipOrder()) {
            if (slot.canAccept(stack) && CzoEquipmentSlotStorage.get(player, slot).isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    private static CzoEquipmentSlot[] quickEquipOrder() {
        return new CzoEquipmentSlot[] {
                CzoEquipmentSlot.PRIMARY_WEAPON_1,
                CzoEquipmentSlot.PRIMARY_WEAPON_2,
                CzoEquipmentSlot.SECONDARY_WEAPON,
                CzoEquipmentSlot.MELEE_WEAPON,
                CzoEquipmentSlot.BOLT,
                CzoEquipmentSlot.THROWABLE,
                CzoEquipmentSlot.EXTRA_POUCH,
                CzoEquipmentSlot.HELMET,
                CzoEquipmentSlot.BODY_ARMOR,
                CzoEquipmentSlot.GAS_MASK,
                CzoEquipmentSlot.EXTRA_PROTECTION,
                CzoEquipmentSlot.BACKPACK,
                CzoEquipmentSlot.ARTIFACT_CONTAINER
        };
    }

    private static void warn(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}
