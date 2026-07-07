package com.czo.client.gunrender;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CzoObjGunModelCache {
    private static final Map<Identifier, CzoObjGunModel> CACHE = new HashMap<>();

    private CzoObjGunModelCache() {
    }

    public static CzoObjGunModel get(Identifier id) {
        CzoObjGunModel cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines().toList();
                CzoObjGunModel model = CzoObjGunModel.parse(lines);
                CACHE.put(id, model);
                return model;
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
