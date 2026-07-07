package com.czo.anomaly;

import com.czo.CZO;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Stage 8F.6: pure holder for new anomaly ids and registered objects.
 *
 * <p>Do NOT create Block/Item instances in this class. NeoForge 26.x can load
 * @EventBusSubscriber classes after registries are already frozen during mod
 * construction. If a Block is created from a static initializer, Minecraft
 * throws "Registry is already frozen". This class now stores ids and nullable
 * references only; real registration is performed by CzoAnomalyRegistryEvents.</p>
 */
public final class CzoAnomalyBlocks {
    public static final Identifier ZERO_GRAVITY_ID = id("anomaly_zero_gravity");
    public static final Identifier COBRA_ID = id("anomaly_cobra");
    public static final Identifier OBR_116_ID = id("anomaly_obr_116");
    public static final Identifier BUBBLE_ID = id("anomaly_bubble");

    private static Block zeroGravity;
    private static Block cobra;
    private static Block obr116;
    private static Block bubble;

    private static Item zeroGravityItem;
    private static Item cobraItem;
    private static Item obr116Item;
    private static Item bubbleItem;

    private CzoAnomalyBlocks() {
    }

    static void setZeroGravity(Block block, Item item) {
        zeroGravity = block;
        zeroGravityItem = item;
    }

    static void setCobra(Block block, Item item) {
        cobra = block;
        cobraItem = item;
    }

    static void setObr116(Block block, Item item) {
        obr116 = block;
        obr116Item = item;
    }

    static void setBubble(Block block, Item item) {
        bubble = block;
        bubbleItem = item;
    }

    public static Block zeroGravity() {
        return zeroGravity;
    }

    public static Block cobra() {
        return cobra;
    }

    public static Block obr116() {
        return obr116;
    }

    public static Block bubble() {
        return bubble;
    }

    public static Item zeroGravityItem() {
        return zeroGravityItem;
    }

    public static Item cobraItem() {
        return cobraItem;
    }

    public static Item obr116Item() {
        return obr116Item;
    }

    public static Item bubbleItem() {
        return bubbleItem;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CZO.MODID, path);
    }
}
