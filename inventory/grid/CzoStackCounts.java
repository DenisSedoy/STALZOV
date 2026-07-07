package com.czo.inventory.grid;

import com.czo.registry.CzoDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * Logical stack-count helper for the CZO backpack.
 *
 * <p>Minecraft 1.21.6 validates ItemStack counts in a lot of vanilla packets/menus
 * and creative mode can disconnect the client if a physical ItemStack count is larger
 * than the vanilla codec accepts. CZO therefore keeps the real ItemStack count network-safe
 * and stores the backpack amount in a CZO data component.</p>
 */
public final class CzoStackCounts {
    /** Requested CZO limit for normal 64-stack items. */
    public static final int CZO_DEFAULT_MAX_STACK_SIZE = 5000;

    /** Vanilla ItemStack.CODEC currently accepts 1..99 during strict packet validation. */
    private static final int VANILLA_PACKET_SAFE_COUNT = 99;

    private CzoStackCounts() {
    }

    public static int maxLogicalStackSize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int vanillaMax = Math.max(1, stack.getMaxStackSize());
        return vanillaMax == 64 ? CZO_DEFAULT_MAX_STACK_SIZE : vanillaMax;
    }

    public static int logicalCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return Math.max(0, stack.getOrDefault(CzoDataComponents.CZO_STACK_COUNT.get(), stack.getCount()));
    }

    public static void setLogicalCount(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (count <= 0) {
            stack.remove(CzoDataComponents.CZO_STACK_COUNT.get());
            stack.setCount(0);
            return;
        }

        int clamped = Math.min(count, maxLogicalStackSize(stack));
        int physical = Math.min(clamped, physicalSafeMax(stack));
        stack.setCount(Math.max(1, physical));

        if (clamped == stack.getCount()) {
            stack.remove(CzoDataComponents.CZO_STACK_COUNT.get());
        } else {
            stack.set(CzoDataComponents.CZO_STACK_COUNT.get(), clamped);
        }
    }

    public static void shrinkLogical(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return;
        }
        setLogicalCount(stack, logicalCount(stack) - amount);
    }

    public static void growLogical(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return;
        }
        setLogicalCount(stack, logicalCount(stack) + amount);
    }

    public static boolean normalizePhysicalCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Integer storedLogicalCount = stack.get(CzoDataComponents.CZO_STACK_COUNT.get());
        int logical = storedLogicalCount == null ? stack.getCount() : storedLogicalCount;
        if (logical <= 0) {
            stack.remove(CzoDataComponents.CZO_STACK_COUNT.get());
            stack.setCount(0);
            return true;
        }
        if (stack.getCount() > physicalSafeMax(stack) || storedLogicalCount != null) {
            int oldPhysical = stack.getCount();
            Integer oldLogical = storedLogicalCount;
            setLogicalCount(stack, logical);
            Integer newLogical = stack.get(CzoDataComponents.CZO_STACK_COUNT.get());
            return oldPhysical != stack.getCount() || !java.util.Objects.equals(oldLogical, newLogical);
        }
        return false;
    }

    public static String countOverlay(ItemStack stack) {
        int count = logicalCount(stack);
        return count > 1 ? Integer.toString(count) : "";
    }

    public static boolean canMerge(ItemStack target, ItemStack incoming) {
        return target != null
                && incoming != null
                && !target.isEmpty()
                && !incoming.isEmpty()
                && logicalCount(target) < maxLogicalStackSize(target)
                && sameItemSameGameplayComponents(target, incoming);
    }

    /**
     * Same-item comparison for CZO stacks. We ignore CZO bookkeeping components that must not
     * prevent normal stack merging: grid x/y/rotation and virtual stack count.
     */
    public static boolean sameItemSameGameplayComponents(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) {
            return false;
        }
        ItemStack a = first.copyWithCount(1);
        ItemStack b = second.copyWithCount(1);
        stripCzoBookkeeping(a);
        stripCzoBookkeeping(b);
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public static void stripCzoBookkeeping(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(CzoDataComponents.CZO_STACK_COUNT.get());
        stack.remove(CzoDataComponents.INVENTORY_GRID_X.get());
        stack.remove(CzoDataComponents.INVENTORY_GRID_Y.get());
        stack.remove(CzoDataComponents.INVENTORY_GRID_ROTATED.get());
    }

    private static int physicalSafeMax(ItemStack stack) {
        return Math.max(1, Math.min(VANILLA_PACKET_SAFE_COUNT, Math.max(1, stack.getMaxStackSize())));
    }
}
