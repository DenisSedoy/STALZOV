package com.czo.contamination;

import com.czo.CZO;
import com.czo.anomaly.CzoAnomalies;
import com.czo.contamination.block.ContaminationZoneBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class CzoContaminations {
    private CzoContaminations() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CZO.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CZO.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CZO.MODID);

    private static final List<DeferredItem<BlockItem>> ALL_ZONE_ITEMS = new ArrayList<>();

    public static final DeferredBlock<ContaminationZoneBlock> RADIATION_ZONE_1 = registerZoneBlock("radiation_zone_1", ContaminationType.RADIATION, 1, MapColor.COLOR_RED);
    public static final DeferredBlock<ContaminationZoneBlock> RADIATION_ZONE_2 = registerZoneBlock("radiation_zone_2", ContaminationType.RADIATION, 2, MapColor.COLOR_RED);
    public static final DeferredBlock<ContaminationZoneBlock> RADIATION_ZONE_3 = registerZoneBlock("radiation_zone_3", ContaminationType.RADIATION, 3, MapColor.COLOR_RED);
    public static final DeferredBlock<ContaminationZoneBlock> RADIATION_ZONE_4 = registerZoneBlock("radiation_zone_4", ContaminationType.RADIATION, 4, MapColor.COLOR_RED);
    public static final DeferredBlock<ContaminationZoneBlock> RADIATION_ZONE_5 = registerZoneBlock("radiation_zone_5", ContaminationType.RADIATION, 5, MapColor.COLOR_RED);

    public static final DeferredBlock<ContaminationZoneBlock> PSY_ZONE_1 = registerZoneBlock("psy_zone_1", ContaminationType.PSY, 1, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ContaminationZoneBlock> PSY_ZONE_2 = registerZoneBlock("psy_zone_2", ContaminationType.PSY, 2, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ContaminationZoneBlock> PSY_ZONE_3 = registerZoneBlock("psy_zone_3", ContaminationType.PSY, 3, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ContaminationZoneBlock> PSY_ZONE_4 = registerZoneBlock("psy_zone_4", ContaminationType.PSY, 4, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ContaminationZoneBlock> PSY_ZONE_5 = registerZoneBlock("psy_zone_5", ContaminationType.PSY, 5, MapColor.COLOR_PURPLE);

    public static final DeferredBlock<ContaminationZoneBlock> BIO_ZONE_1 = registerZoneBlock("bio_zone_1", ContaminationType.BIOLOGICAL, 1, MapColor.COLOR_GREEN);
    public static final DeferredBlock<ContaminationZoneBlock> BIO_ZONE_2 = registerZoneBlock("bio_zone_2", ContaminationType.BIOLOGICAL, 2, MapColor.COLOR_GREEN);
    public static final DeferredBlock<ContaminationZoneBlock> BIO_ZONE_3 = registerZoneBlock("bio_zone_3", ContaminationType.BIOLOGICAL, 3, MapColor.COLOR_GREEN);
    public static final DeferredBlock<ContaminationZoneBlock> BIO_ZONE_4 = registerZoneBlock("bio_zone_4", ContaminationType.BIOLOGICAL, 4, MapColor.COLOR_GREEN);
    public static final DeferredBlock<ContaminationZoneBlock> BIO_ZONE_5 = registerZoneBlock("bio_zone_5", ContaminationType.BIOLOGICAL, 5, MapColor.COLOR_GREEN);

    public static final DeferredBlock<ContaminationZoneBlock> THERMAL_ZONE_1 = registerZoneBlock("thermal_zone_1", ContaminationType.THERMAL, 1, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ContaminationZoneBlock> THERMAL_ZONE_2 = registerZoneBlock("thermal_zone_2", ContaminationType.THERMAL, 2, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ContaminationZoneBlock> THERMAL_ZONE_3 = registerZoneBlock("thermal_zone_3", ContaminationType.THERMAL, 3, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ContaminationZoneBlock> THERMAL_ZONE_4 = registerZoneBlock("thermal_zone_4", ContaminationType.THERMAL, 4, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ContaminationZoneBlock> THERMAL_ZONE_5 = registerZoneBlock("thermal_zone_5", ContaminationType.THERMAL, 5, MapColor.COLOR_ORANGE);

    public static final DeferredItem<BlockItem> RADIATION_ZONE_1_ITEM = registerZoneItem("radiation_zone_1", RADIATION_ZONE_1);
    public static final DeferredItem<BlockItem> RADIATION_ZONE_2_ITEM = registerZoneItem("radiation_zone_2", RADIATION_ZONE_2);
    public static final DeferredItem<BlockItem> RADIATION_ZONE_3_ITEM = registerZoneItem("radiation_zone_3", RADIATION_ZONE_3);
    public static final DeferredItem<BlockItem> RADIATION_ZONE_4_ITEM = registerZoneItem("radiation_zone_4", RADIATION_ZONE_4);
    public static final DeferredItem<BlockItem> RADIATION_ZONE_5_ITEM = registerZoneItem("radiation_zone_5", RADIATION_ZONE_5);

    public static final DeferredItem<BlockItem> PSY_ZONE_1_ITEM = registerZoneItem("psy_zone_1", PSY_ZONE_1);
    public static final DeferredItem<BlockItem> PSY_ZONE_2_ITEM = registerZoneItem("psy_zone_2", PSY_ZONE_2);
    public static final DeferredItem<BlockItem> PSY_ZONE_3_ITEM = registerZoneItem("psy_zone_3", PSY_ZONE_3);
    public static final DeferredItem<BlockItem> PSY_ZONE_4_ITEM = registerZoneItem("psy_zone_4", PSY_ZONE_4);
    public static final DeferredItem<BlockItem> PSY_ZONE_5_ITEM = registerZoneItem("psy_zone_5", PSY_ZONE_5);

    public static final DeferredItem<BlockItem> BIO_ZONE_1_ITEM = registerZoneItem("bio_zone_1", BIO_ZONE_1);
    public static final DeferredItem<BlockItem> BIO_ZONE_2_ITEM = registerZoneItem("bio_zone_2", BIO_ZONE_2);
    public static final DeferredItem<BlockItem> BIO_ZONE_3_ITEM = registerZoneItem("bio_zone_3", BIO_ZONE_3);
    public static final DeferredItem<BlockItem> BIO_ZONE_4_ITEM = registerZoneItem("bio_zone_4", BIO_ZONE_4);
    public static final DeferredItem<BlockItem> BIO_ZONE_5_ITEM = registerZoneItem("bio_zone_5", BIO_ZONE_5);

    public static final DeferredItem<BlockItem> THERMAL_ZONE_1_ITEM = registerZoneItem("thermal_zone_1", THERMAL_ZONE_1);
    public static final DeferredItem<BlockItem> THERMAL_ZONE_2_ITEM = registerZoneItem("thermal_zone_2", THERMAL_ZONE_2);
    public static final DeferredItem<BlockItem> THERMAL_ZONE_3_ITEM = registerZoneItem("thermal_zone_3", THERMAL_ZONE_3);
    public static final DeferredItem<BlockItem> THERMAL_ZONE_4_ITEM = registerZoneItem("thermal_zone_4", THERMAL_ZONE_4);
    public static final DeferredItem<BlockItem> THERMAL_ZONE_5_ITEM = registerZoneItem("thermal_zone_5", THERMAL_ZONE_5);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTAMINATION_TAB = CREATIVE_MODE_TABS.register(
            "contaminations",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.czo.contaminations"))
                    .icon(() -> RADIATION_ZONE_1_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredItem<BlockItem> item : ALL_ZONE_ITEMS) {
                            output.accept(item.get());
                        }
                    })
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static DeferredBlock<ContaminationZoneBlock> registerZoneBlock(String id, ContaminationType type, int level, MapColor mapColor) {
        return BLOCKS.registerBlock(
                id,
                props -> new ContaminationZoneBlock(type, level, props),
                props -> props
                        .mapColor(mapColor)
                        .strength(0.1F)
        );
    }

    private static DeferredItem<BlockItem> registerZoneItem(String id, DeferredBlock<ContaminationZoneBlock> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(id, block);
        ALL_ZONE_ITEMS.add(item);
        return item;
    }

    public static ContaminationType getType(Block block) {
        if (block instanceof ContaminationZoneBlock zoneBlock) {
            return zoneBlock.contaminationType();
        }

        return null;
    }

    public static int getLevel(Block block) {
        if (block instanceof ContaminationZoneBlock zoneBlock) {
            return zoneBlock.contaminationLevel();
        }

        return 0;
    }
}
