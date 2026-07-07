package com.czo.anomaly;

import com.czo.CZO;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Adds manually registered anomaly block-items to the CZO anomalies tab only.
 *
 * NeoForge 26.1.2 throws if the same stack is accepted into the same tab twice.
 * Do not inject these items into SEARCH / vanilla tabs here: the search tab can
 * already collect items from mod tabs and will crash on duplicates.
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoAnomalyCreativeTabs {
    private static final ResourceKey<CreativeModeTab> ANOMALIES_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(CZO.MODID, "anomalies")
    );

    private CzoAnomalyCreativeTabs() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!ANOMALIES_TAB_KEY.equals(event.getTabKey())) {
            return;
        }

        accept(event, CzoAnomalyBlocks.zeroGravityItem());
        accept(event, CzoAnomalyBlocks.cobraItem());
        accept(event, CzoAnomalyBlocks.obr116Item());
        accept(event, CzoAnomalyBlocks.bubbleItem());
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, Item item) {
        if (item != null) {
            event.accept(item);
        }
    }
}
