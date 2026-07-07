package com.czo.registry;

import com.czo.CZO;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class CzoDataComponents {
    private CzoDataComponents() {
    }

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CZO.MODID);

    public static final Supplier<DataComponentType<String>> GUN_ID =
            DATA_COMPONENTS.registerComponentType(
                    "gun_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    // Выбранный тип патрона: какой тип игрок хочет заряжать следующим.
    public static final Supplier<DataComponentType<String>> SELECTED_AMMO_ID =
            DATA_COMPONENTS.registerComponentType(
                    "selected_ammo_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    // Реально заряженный тип патронов в магазине/патроннике.
    // Нужен, чтобы заряженные FMJ не превращались в БП при смене выбранного типа.
    public static final Supplier<DataComponentType<String>> LOADED_AMMO_ID =
            DATA_COMPONENTS.registerComponentType(
                    "loaded_ammo_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> MUZZLE_ID =
            DATA_COMPONENTS.registerComponentType(
                    "muzzle_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> OPTIC_ID =
            DATA_COMPONENTS.registerComponentType(
                    "optic_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> STOCK_ID =
            DATA_COMPONENTS.registerComponentType(
                    "stock_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> GRIP_ID =
            DATA_COMPONENTS.registerComponentType(
                    "grip_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> BARREL_ID =
            DATA_COMPONENTS.registerComponentType(
                    "barrel_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> HANDGUARD_ID =
            DATA_COMPONENTS.registerComponentType(
                    "handguard_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> CHARM_ID =
            DATA_COMPONENTS.registerComponentType(
                    "charm_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> LASER_ID =
            DATA_COMPONENTS.registerComponentType(
                    "laser_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<String>> MAGAZINE_ID =
            DATA_COMPONENTS.registerComponentType(
                    "magazine_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final Supplier<DataComponentType<Integer>> AMMO_IN_MAG =
            DATA_COMPONENTS.registerComponentType(
                    "ammo_in_mag",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    public static final Supplier<DataComponentType<Integer>> CHAMBERED_ROUND =
            DATA_COMPONENTS.registerComponentType(
                    "chambered_round",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    public static final Supplier<DataComponentType<Integer>> RELOAD_TICKS_LEFT =
            DATA_COMPONENTS.registerComponentType(
                    "reload_ticks_left",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    // Позиция предмета в CZO grid-инвентаре.
    // Храним прямо на ItemStack через data components, чтобы раскладка рюкзака
    // переживала закрытие GUI и синхронизировалась клиенту стандартным путём Minecraft.

    // Логическое количество предметов в CZO-рюкзаке.
    // Физический ItemStack.count держим <= vanilla packet-safe лимита, чтобы creative/vanilla menus
    // не падали на strict ItemStack validation.
    public static final Supplier<DataComponentType<Integer>> CZO_STACK_COUNT =
            DATA_COMPONENTS.registerComponentType(
                    "czo_stack_count",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    public static final Supplier<DataComponentType<Integer>> INVENTORY_GRID_X =
            DATA_COMPONENTS.registerComponentType(
                    "inventory_grid_x",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    public static final Supplier<DataComponentType<Integer>> INVENTORY_GRID_Y =
            DATA_COMPONENTS.registerComponentType(
                    "inventory_grid_y",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    // 0 = обычная ориентация, 1 = повёрнутый предмет. Integer используем намеренно,
    // чтобы не зависеть от имени bool-кодека в конкретной сборке NeoForge/Minecraft.
    public static final Supplier<DataComponentType<Integer>> INVENTORY_GRID_ROTATED =
            DATA_COMPONENTS.registerComponentType(
                    "inventory_grid_rotated",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

}
