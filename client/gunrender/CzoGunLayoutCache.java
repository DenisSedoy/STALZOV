package com.czo.client.gunrender;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Lazy client cache. We keep it simple for stage 1; resource-reload listener can come later. */
public final class CzoGunLayoutCache {
    private static final Gson GSON = new Gson();
    private static final Map<Identifier, CzoGunLayout> CACHE = new HashMap<>();

    private CzoGunLayoutCache() {
    }

    public static CzoGunLayout get(Identifier id) {
        CzoGunLayout cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                return null;
            }

            try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                CzoGunLayout layout = CzoGunLayout.fromJson(GSON.fromJson(reader, JsonObject.class));
                CACHE.put(id, layout);
                return layout;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}
