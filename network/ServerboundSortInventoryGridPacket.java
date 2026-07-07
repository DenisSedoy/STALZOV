package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.CzoInventoryBootstrap;
import com.czo.inventory.grid.GridInventory;
import com.czo.inventory.grid.GridInventorySlots;
import com.czo.inventory.grid.InventoryItemCategory;
import com.czo.inventory.grid.ItemSize;
import com.czo.inventory.grid.PlacementResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Серверная автосортировка CZO grid-раскладки. */
public record ServerboundSortInventoryGridPacket() implements CustomPacketPayload {
    public static final ServerboundSortInventoryGridPacket INSTANCE = new ServerboundSortInventoryGridPacket();

    public static final Type<ServerboundSortInventoryGridPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "sort_inventory_grid"));

    public static final StreamCodec<ByteBuf, ServerboundSortInventoryGridPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSortInventoryGridPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Inventory inventory = player.getInventory();
            List<Integer> slots = new ArrayList<>();
            for (int slot = GridInventorySlots.FIRST_PLAYER_SLOT; slot <= GridInventorySlots.LAST_PLAYER_SLOT; slot++) {
                if (!inventory.getItem(slot).isEmpty()) {
                    slots.add(slot);
                }
            }

            slots.sort(Comparator
                    .comparingInt((Integer slot) -> InventoryItemCategory.detect(inventory.getItem(slot)).sortOrder())
                    .thenComparingInt(slot -> -CzoInventoryBootstrap.ITEM_SIZES.sizeOf(inventory.getItem(slot)).area())
                    .thenComparing(slot -> inventory.getItem(slot).getHoverName().getString()));

            GridInventory grid = new GridInventory(GridInventorySlots.GRID_COLUMNS, GridInventorySlots.GRID_ROWS);
            for (int slot : slots) {
                ItemStack stack = inventory.getItem(slot);
                ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(stack, false);
                PlacementResult placed = grid.placeFirstFree(stack, size, false);
                if (!placed.ok()) {
                    // Если крупный предмет не влез из-за текущей конфигурации, не выкидываем его:
                    // кладём как 1x1, чтобы GUI не пропал и не сломал раскладку.
                    placed = grid.placeFirstFree(stack, ItemSize.ONE_BY_ONE, false);
                }
                if (!placed.ok() || placed.entryId() == null) {
                    player.sendSystemMessage(Component.literal("Автосортировка: не хватило места"), true);
                    return;
                }

                grid.getEntry(placed.entryId()).ifPresent(entry -> {
                    GridInventorySlots.setStoredPosition(stack, entry.rect().x(), entry.rect().y(), entry.rotated());
                    inventory.setItem(slot, stack);
                });
            }

            player.inventoryMenu.broadcastChanges();
        });
    }
}
