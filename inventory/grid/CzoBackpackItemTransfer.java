package com.czo.inventory.grid;

import com.czo.inventory.CzoInventoryBootstrap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side helper for moving stacks into the CZO backpack grid without touching hidden hotbar slots. */
public final class CzoBackpackItemTransfer {
    /** Maximum logical stack capacity inside the CZO backpack for regular 64-stack items. */
    public static final int CZO_DEFAULT_MAX_STACK_SIZE = CzoStackCounts.CZO_DEFAULT_MAX_STACK_SIZE;

    private CzoBackpackItemTransfer() {
    }

    public static int maxStackSize(ItemStack stack) {
        return CzoStackCounts.maxLogicalStackSize(stack);
    }

    public static boolean canMerge(ItemStack target, ItemStack incoming) {
        return CzoStackCounts.canMerge(target, incoming);
    }

    /**
     * Safety net called from player tick. It converts unsafe physical stacks, including creative
     * 300-count ammo stacks, into packet-safe physical stacks + CZO logical count components.
     */
    public static void normalizePlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && CzoStackCounts.normalizePhysicalCount(stack)) {
                inventory.setItem(slot, stack);
                changed = true;
            }
        }
        if (changed) {
            inventory.setChanged();
        }
    }

    /** Inserts as much of {@code stack} as possible into the CZO backpack. The passed stack is mutated. */
    public static int insertIntoBackpack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return 0;
        }

        CzoStackCounts.normalizePhysicalCount(stack);
        Inventory inventory = player.getInventory();
        int before = CzoStackCounts.logicalCount(stack);
        mergeIntoExistingStacks(inventory, stack);
        if (!stack.isEmpty()) {
            placeIntoEmptyBackpackSlots(inventory, stack);
        }
        inventory.setChanged();
        return Math.max(0, before - CzoStackCounts.logicalCount(stack));
    }

    private static void mergeIntoExistingStacks(Inventory inventory, ItemStack incoming) {
        for (int slot = GridInventorySlots.FIRST_PLAYER_SLOT; slot <= GridInventorySlots.LAST_PLAYER_SLOT && !incoming.isEmpty(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (!CzoStackCounts.canMerge(existing, incoming)) {
                continue;
            }

            int limit = CzoStackCounts.maxLogicalStackSize(existing);
            int existingCount = CzoStackCounts.logicalCount(existing);
            int incomingCount = CzoStackCounts.logicalCount(incoming);
            int move = Math.min(limit - existingCount, incomingCount);
            if (move <= 0) {
                continue;
            }

            CzoStackCounts.setLogicalCount(existing, existingCount + move);
            CzoStackCounts.setLogicalCount(incoming, incomingCount - move);
            inventory.setItem(slot, existing);
        }
    }

    private static void placeIntoEmptyBackpackSlots(Inventory inventory, ItemStack incoming) {
        while (!incoming.isEmpty()) {
            int emptySlot = firstEmptyBackpackSlot(inventory);
            if (emptySlot < 0) {
                break;
            }

            int incomingCount = CzoStackCounts.logicalCount(incoming);
            int chunkCount = Math.min(CzoStackCounts.maxLogicalStackSize(incoming), incomingCount);
            ItemStack chunk = incoming.copyWithCount(1);
            CzoStackCounts.stripCzoBookkeeping(chunk);
            CzoStackCounts.setLogicalCount(chunk, chunkCount);
            CzoStackCounts.normalizePhysicalCount(chunk);

            GridEntry placedEntry = placeChunkInGrid(inventory, chunk);
            if (placedEntry == null) {
                break;
            }

            GridInventorySlots.setStoredPosition(chunk, placedEntry.rect().x(), placedEntry.rect().y(), placedEntry.rotated());
            inventory.setItem(emptySlot, chunk);
            CzoStackCounts.setLogicalCount(incoming, incomingCount - chunkCount);
        }
    }

    private static GridEntry placeChunkInGrid(Inventory inventory, ItemStack chunk) {
        GridInventory grid = buildGrid(inventory);
        ItemSize size = CzoInventoryBootstrap.ITEM_SIZES.sizeOf(chunk, false);
        PlacementResult result = grid.placeFirstFree(chunk, size, false);
        if (!result.ok() || result.entryId() == null) {
            return null;
        }
        return grid.getEntry(result.entryId()).orElse(null);
    }

    private static GridInventory buildGrid(Inventory inventory) {
        Map<UUID, Integer> slotByEntryId = new HashMap<>();
        return GridInventorySlots.buildPlayerBackpack(
                inventory,
                CzoInventoryBootstrap.ITEM_SIZES,
                slotByEntryId
        );
    }

    private static int firstEmptyBackpackSlot(Inventory inventory) {
        for (int slot = GridInventorySlots.FIRST_PLAYER_SLOT; slot <= GridInventorySlots.LAST_PLAYER_SLOT; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }
}
