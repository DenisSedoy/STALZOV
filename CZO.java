package com.czo;

import org.slf4j.Logger;

import com.czo.item.gun.GunItem;
import com.czo.item.gun.ammo.CzoAmmo;
import com.czo.item.gun.definition.CzoGuns;
import com.czo.item.gun.module.CzoGunModules;
import com.czo.registry.CzoDataComponents;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.czo.anomaly.CzoAnomalies;
import com.czo.contamination.CzoContaminations;


@Mod(CZO.MODID)
public class CZO {
    public static final String MODID = "czo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredItem<GunItem> PISTOL =
            ITEMS.registerItem(
                    "pistol",
                    props -> new GunItem(props, CzoGuns.PM),
                    props -> props
                            .stacksTo(1)
                            .component(CzoDataComponents.GUN_ID.get(), CzoGuns.PM)
                            .component(CzoDataComponents.SELECTED_AMMO_ID.get(), CzoAmmo.AMMO_9X18_FMJ)
                            .component(CzoDataComponents.LOADED_AMMO_ID.get(), CzoAmmo.AMMO_9X18_FMJ)
                            .component(CzoDataComponents.AMMO_IN_MAG.get(), 7)
                            .component(CzoDataComponents.CHAMBERED_ROUND.get(), 1)
                            .component(CzoDataComponents.RELOAD_TICKS_LEFT.get(), 0)
                            .component(CzoDataComponents.MAGAZINE_ID.get(), CzoGuns.get(CzoGuns.PM).defaultMagazineId())
                            .component(CzoDataComponents.MUZZLE_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.OPTIC_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.STOCK_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.GRIP_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.BARREL_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.HANDGUARD_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.CHARM_ID.get(), CzoGunModules.NONE)
                            .component(CzoDataComponents.LASER_ID.get(), CzoGunModules.NONE)
            );

    // Текущие 9x18 теперь считаем FMJ, id оставляем старый, чтобы не ломать существующие стаки/ресурсы.
    public static final DeferredItem<Item> AMMO_9X18_FMJ =
            ITEMS.registerSimpleItem("ammo_9x18_fmj", props -> props.stacksTo(300));

    // Алиас для старого кода.
    public static final DeferredItem<Item> AMMO_9X18 = AMMO_9X18_FMJ;

    public static final DeferredItem<Item> AMMO_9X18_AP =
            ITEMS.registerSimpleItem("ammo_9x18_ap", props -> props.stacksTo(300));

    // Физический предмет только для расширенного магазина. Стоковый pm_mag_8 предметом НЕ регистрируем.
    public static final DeferredItem<Item> PM_MAG_12 =
            ITEMS.registerSimpleItem("pm_mag_12", props -> props.stacksTo(8));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.czo"))
            .icon(() -> PISTOL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(PISTOL.get());
                output.accept(AMMO_9X18_FMJ.get());
                output.accept(AMMO_9X18_AP.get());
                output.accept(PM_MAG_12.get());
            }).build());

    public CZO(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        CzoDataComponents.DATA_COMPONENTS.register(modEventBus);
        CzoAnomalies.register(modEventBus);
        CzoContaminations.register(modEventBus);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
