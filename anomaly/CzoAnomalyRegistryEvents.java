package com.czo.anomaly;

import com.czo.CZO;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StructureVoidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Stage 8F.6: event-driven registration for new anomaly blocks/items.
 *
 * <p>All Block/BlockItem instances are created only from RegisterEvent callbacks,
 * never from static initialization. This avoids "Registry is already frozen" on
 * Minecraft 26.1.2 / NeoForge 26.1.2.</p>
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoAnomalyRegistryEvents {
    private static Block zeroGravityBlock;
    private static Block cobraBlock;
    private static Block obr116Block;
    private static Block bubbleBlock;

    private CzoAnomalyRegistryEvents() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            zeroGravityBlock = structureVoidAnomalyBlock(CzoAnomalyBlocks.ZERO_GRAVITY_ID);
            cobraBlock = structureVoidAnomalyBlock(CzoAnomalyBlocks.COBRA_ID);
            obr116Block = anomalyBlock(CzoAnomalyBlocks.OBR_116_ID);
            bubbleBlock = structureVoidAnomalyBlock(CzoAnomalyBlocks.BUBBLE_ID);

            helper.register(CzoAnomalyBlocks.ZERO_GRAVITY_ID, zeroGravityBlock);
            helper.register(CzoAnomalyBlocks.COBRA_ID, cobraBlock);
            helper.register(CzoAnomalyBlocks.OBR_116_ID, obr116Block);
            helper.register(CzoAnomalyBlocks.BUBBLE_ID, bubbleBlock);
        });

        event.register(Registries.ITEM, helper -> {
            if (zeroGravityBlock != null) {
                Item item = anomalyBlockItem(zeroGravityBlock, CzoAnomalyBlocks.ZERO_GRAVITY_ID);
                helper.register(CzoAnomalyBlocks.ZERO_GRAVITY_ID, item);
                CzoAnomalyBlocks.setZeroGravity(zeroGravityBlock, item);
            }
            if (cobraBlock != null) {
                Item item = anomalyBlockItem(cobraBlock, CzoAnomalyBlocks.COBRA_ID);
                helper.register(CzoAnomalyBlocks.COBRA_ID, item);
                CzoAnomalyBlocks.setCobra(cobraBlock, item);
            }
            if (obr116Block != null) {
                Item item = anomalyBlockItem(obr116Block, CzoAnomalyBlocks.OBR_116_ID);
                helper.register(CzoAnomalyBlocks.OBR_116_ID, item);
                CzoAnomalyBlocks.setObr116(obr116Block, item);
            }
            if (bubbleBlock != null) {
                Item item = anomalyBlockItem(bubbleBlock, CzoAnomalyBlocks.BUBBLE_ID);
                helper.register(CzoAnomalyBlocks.BUBBLE_ID, item);
                CzoAnomalyBlocks.setBubble(bubbleBlock, item);
            }
        });
    }

    private static Block anomalyBlock(Identifier blockId) {
        return new Block(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, blockId))
                .strength(0.2F)
                .noCollision()
                .noOcclusion());
    }

    private static Block structureVoidAnomalyBlock(Identifier blockId) {
        return new StructureVoidBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, blockId))
                .replaceable()
                .noCollision()
                .noLootTable()
                .noTerrainParticles()
                .pushReaction(PushReaction.DESTROY));
    }

    private static Item anomalyBlockItem(Block block, Identifier itemId) {
        return new BlockItem(block, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, itemId)));
    }
}
