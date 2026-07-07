package com.czo.inventory.grid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр размеров предметов для grid-инвентаря.
 *
 * <p>Правила можно задавать точечно по item id или шире по item tag. Точные правила
 * по item id всегда имеют приоритет над tag-правилами.</p>
 */
public final class ItemSizeRegistry {
    private ItemSize fallbackSize;
    private final Map<Identifier, ItemSize> itemRules = new LinkedHashMap<>();
    private final Map<Identifier, ItemSize> tagRules = new LinkedHashMap<>();

    public ItemSizeRegistry() {
        this(ItemSize.ONE_BY_ONE);
    }

    public ItemSizeRegistry(ItemSize fallbackSize) {
        this.fallbackSize = fallbackSize == null ? ItemSize.ONE_BY_ONE : fallbackSize;
    }

    public static ItemSizeRegistry createDefault() {
        ItemSizeRegistry registry = new ItemSizeRegistry(ItemSize.ONE_BY_ONE);

        // Vanilla-like defaults.
        registry.registerTag("minecraft:swords", new ItemSize(1, 2));
        registry.registerTag("minecraft:axes", new ItemSize(1, 2));
        registry.registerItem("minecraft:shield", new ItemSize(2, 2));
        registry.registerItem("minecraft:bow", new ItemSize(1, 3));
        registry.registerItem("minecraft:crossbow", new ItemSize(2, 2));
        registry.registerItem("minecraft:trident", new ItemSize(1, 3));
        registry.registerItem("minecraft:elytra", new ItemSize(2, 2));

        // CZO current items from the project archive / expected STALKER-like layout.
        registry.registerItem("czo:pistol", new ItemSize(2, 2));
        registry.registerItem("czo:pm_mag_12", new ItemSize(1, 2));
        registry.registerItem("czo:ammo_9x18_fmj", new ItemSize(1, 1));
        registry.registerItem("czo:ammo_9x18_ap", new ItemSize(1, 1));

        // Future naming conventions.
        registry.registerByCommonNameHint("rifle", new ItemSize(2, 5));
        registry.registerByCommonNameHint("shotgun", new ItemSize(2, 5));
        registry.registerByCommonNameHint("smg", new ItemSize(2, 3));
        registry.registerByCommonNameHint("pistol", new ItemSize(2, 2));
        registry.registerByCommonNameHint("mag", new ItemSize(1, 2));
        registry.registerByCommonNameHint("ammo", new ItemSize(1, 1));
        registry.registerByCommonNameHint("body_armor", new ItemSize(3, 3));
        registry.registerByCommonNameHint("helmet", new ItemSize(2, 2));
        registry.registerByCommonNameHint("gas_mask", new ItemSize(2, 2));
        registry.registerByCommonNameHint("backpack", new ItemSize(3, 4));
        registry.registerByCommonNameHint("artifact", new ItemSize(1, 1));

        return registry;
    }

    public ItemSize fallbackSize() {
        return fallbackSize;
    }

    public void setFallbackSize(ItemSize fallbackSize) {
        this.fallbackSize = fallbackSize == null ? ItemSize.ONE_BY_ONE : fallbackSize;
    }

    public void registerItem(String itemId, ItemSize size) {
        registerItem(parseId(itemId), size);
    }

    public void registerItem(Identifier itemId, ItemSize size) {
        if (itemId == null || size == null) {
            return;
        }
        itemRules.put(itemId, size);
    }

    public void registerTag(String tagId, ItemSize size) {
        registerTag(parseTagId(tagId), size);
    }

    public void registerTag(Identifier tagId, ItemSize size) {
        if (tagId == null || size == null) {
            return;
        }
        tagRules.put(tagId, size);
    }

    /**
     * Удобный мост на раннем этапе разработки: если предмет называется czo:ak_rifle,
     * а точного правила ещё нет, можно получить размер по части имени.
     */
    public void registerByCommonNameHint(String pathPart, ItemSize size) {
        if (pathPart == null || pathPart.isBlank() || size == null) {
            return;
        }
        // Хранится как специальный namespace czo_hint. Это не Minecraft id, а внутреннее правило.
        itemRules.put(Identifier.fromNamespaceAndPath("czo_hint", pathPart.toLowerCase()), size);
    }

    public ItemSize sizeOf(ItemStack stack) {
        return sizeOf(stack, false);
    }

    public ItemSize sizeOf(ItemStack stack, boolean rotated) {
        ItemSize size = resolve(stack).orElse(fallbackSize);
        return rotated ? size.rotated() : size;
    }

    public Optional<ItemSize> resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ItemSize exact = itemRules.get(itemId);
        if (exact != null) {
            return Optional.of(exact);
        }

        for (Map.Entry<Identifier, ItemSize> entry : tagRules.entrySet()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, entry.getKey());
            if (stack.is(tag)) {
                return Optional.of(entry.getValue());
            }
        }

        String path = itemId.getPath().toLowerCase();
        for (Map.Entry<Identifier, ItemSize> entry : itemRules.entrySet()) {
            Identifier ruleId = entry.getKey();
            if ("czo_hint".equals(ruleId.getNamespace()) && path.contains(ruleId.getPath())) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    public Map<Identifier, ItemSize> itemRules() {
        return Collections.unmodifiableMap(itemRules);
    }

    public Map<Identifier, ItemSize> tagRules() {
        return Collections.unmodifiableMap(tagRules);
    }

    private static Identifier parseId(String raw) {
        Identifier parsed = Identifier.tryParse(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid resource location: " + raw);
        }
        return parsed;
    }

    private static Identifier parseTagId(String raw) {
        String clean = raw.startsWith("#") ? raw.substring(1) : raw;
        return parseId(clean);
    }
}
