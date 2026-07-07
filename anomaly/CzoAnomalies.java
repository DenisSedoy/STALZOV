package com.czo.anomaly;

import com.czo.CZO;
import com.czo.anomaly.block.AnomalyBlock;

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

public final class CzoAnomalies {
    private CzoAnomalies() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CZO.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CZO.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CZO.MODID);

    public static final DeferredBlock<AnomalyBlock> BURNER_ANOMALY = registerAnomalyBlock("burner_anomaly", AnomalyType.BURNER, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<AnomalyBlock> INFERNO_ANOMALY = registerAnomalyBlock("inferno_anomaly", AnomalyType.INFERNO, MapColor.COLOR_BLUE);
    public static final DeferredBlock<AnomalyBlock> STEAM_ANOMALY = registerAnomalyBlock("steam_anomaly", AnomalyType.STEAM, MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<AnomalyBlock> GAS_ANOMALY = registerAnomalyBlock("gas_anomaly", AnomalyType.GAS, MapColor.COLOR_YELLOW);

    public static final DeferredBlock<AnomalyBlock> VORTEX_ANOMALY = registerAnomalyBlock("vortex_anomaly", AnomalyType.VORTEX, MapColor.COLOR_GREEN);
    public static final DeferredBlock<AnomalyBlock> CAROUSEL_ANOMALY = registerAnomalyBlock("carousel_anomaly", AnomalyType.CAROUSEL, MapColor.COLOR_GRAY);
    public static final DeferredBlock<AnomalyBlock> HYDRAULIC_ANOMALY = registerAnomalyBlock("hydraulic_anomaly", AnomalyType.HYDRAULIC, MapColor.COLOR_GRAY);

    public static final DeferredBlock<AnomalyBlock> ELECTRO_ANOMALY = registerAnomalyBlock("electro_anomaly", AnomalyType.ELECTRO, MapColor.COLOR_CYAN);
    public static final DeferredBlock<AnomalyBlock> ELECTROSTATIC_ANOMALY = registerAnomalyBlock("electrostatic_anomaly", AnomalyType.ELECTROSTATIC, MapColor.COLOR_BLUE);

    public static final DeferredBlock<AnomalyBlock> ACID_ANOMALY = registerAnomalyBlock("acid_anomaly", AnomalyType.ACID, MapColor.COLOR_LIGHT_GREEN);
    public static final DeferredBlock<AnomalyBlock> SPRINGBOARD_ANOMALY = registerAnomalyBlock("springboard_anomaly", AnomalyType.SPRINGBOARD, MapColor.COLOR_PURPLE);

    public static final DeferredItem<BlockItem> BURNER_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("burner_anomaly", BURNER_ANOMALY);
    public static final DeferredItem<BlockItem> INFERNO_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("inferno_anomaly", INFERNO_ANOMALY);
    public static final DeferredItem<BlockItem> STEAM_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("steam_anomaly", STEAM_ANOMALY);
    public static final DeferredItem<BlockItem> GAS_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("gas_anomaly", GAS_ANOMALY);

    public static final DeferredItem<BlockItem> VORTEX_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("vortex_anomaly", VORTEX_ANOMALY);
    public static final DeferredItem<BlockItem> CAROUSEL_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("carousel_anomaly", CAROUSEL_ANOMALY);
    public static final DeferredItem<BlockItem> HYDRAULIC_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("hydraulic_anomaly", HYDRAULIC_ANOMALY);

    public static final DeferredItem<BlockItem> ELECTRO_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("electro_anomaly", ELECTRO_ANOMALY);
    public static final DeferredItem<BlockItem> ELECTROSTATIC_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("electrostatic_anomaly", ELECTROSTATIC_ANOMALY);

    public static final DeferredItem<BlockItem> ACID_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("acid_anomaly", ACID_ANOMALY);
    public static final DeferredItem<BlockItem> SPRINGBOARD_ANOMALY_ITEM = ITEMS.registerSimpleBlockItem("springboard_anomaly", SPRINGBOARD_ANOMALY);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANOMALIES_TAB = CREATIVE_MODE_TABS.register(
            "anomalies",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.czo.anomalies"))
                    .icon(() -> VORTEX_ANOMALY_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BURNER_ANOMALY_ITEM.get());
                        output.accept(INFERNO_ANOMALY_ITEM.get());
                        output.accept(STEAM_ANOMALY_ITEM.get());
                        output.accept(GAS_ANOMALY_ITEM.get());

                        output.accept(VORTEX_ANOMALY_ITEM.get());
                        output.accept(CAROUSEL_ANOMALY_ITEM.get());
                        output.accept(HYDRAULIC_ANOMALY_ITEM.get());

                        output.accept(ELECTRO_ANOMALY_ITEM.get());
                        output.accept(ELECTROSTATIC_ANOMALY_ITEM.get());

                        output.accept(ACID_ANOMALY_ITEM.get());
                        output.accept(SPRINGBOARD_ANOMALY_ITEM.get());
                    })
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static DeferredBlock<AnomalyBlock> registerAnomalyBlock(String id, AnomalyType type, MapColor mapColor) {
        return BLOCKS.registerBlock(
                id,
                props -> new AnomalyBlock(type, props),
                props -> props
                        .mapColor(mapColor)
                        .strength(0.1F)
                        .lightLevel(AnomalyBlock::getCzoLightLevel)
        );
    }

    public static AnomalyType getType(Block block) {
        if (block instanceof AnomalyBlock anomalyBlock) {
            return anomalyBlock.anomalyType();
        }

        return null;
    }
}
