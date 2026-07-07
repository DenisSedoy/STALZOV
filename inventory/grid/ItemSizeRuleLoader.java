package com.czo.inventory.grid;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.Map;

/**
 * Минимальный загрузчик JSON-правил размеров.
 *
 * <p>Формат:</p>
 * <pre>
 * {
 *   "default": [1, 1],
 *   "items": { "czo:pistol": [2, 2] },
 *   "tags": { "minecraft:swords": [1, 2] }
 * }
 * </pre>
 */
public final class ItemSizeRuleLoader {
    private static final Gson GSON = new Gson();

    private ItemSizeRuleLoader() {
    }

    public static ItemSizeRegistry load(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        ItemSize fallback = root.has("default") ? readSize(root.get("default")) : ItemSize.ONE_BY_ONE;
        ItemSizeRegistry registry = new ItemSizeRegistry(fallback);

        if (root.has("items") && root.get("items").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("items").entrySet()) {
                registry.registerItem(entry.getKey(), readSize(entry.getValue()));
            }
        }

        if (root.has("tags") && root.get("tags").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("tags").entrySet()) {
                registry.registerTag(entry.getKey(), readSize(entry.getValue()));
            }
        }

        if (root.has("name_hints") && root.get("name_hints").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("name_hints").entrySet()) {
                registry.registerByCommonNameHint(entry.getKey(), readSize(entry.getValue()));
            }
        }

        return registry;
    }

    private static ItemSize readSize(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return ItemSize.ONE_BY_ONE;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 2) {
            throw new IllegalArgumentException("Item size must be [width, height]: " + GSON.toJson(element));
        }
        return new ItemSize(array.get(0).getAsInt(), array.get(1).getAsInt());
    }
}
