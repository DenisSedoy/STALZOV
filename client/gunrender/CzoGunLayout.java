package com.czo.client.gunrender;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only layout exported from Blender: parts are in OBJ, points/sockets are here. */
public final class CzoGunLayout {
    private final Identifier model;
    private final Identifier texture;
    private final String root;
    private final List<Part> parts;
    private final Map<String, Point> points;
    private final Map<String, ModuleSlot> moduleSlots;

    private CzoGunLayout(Identifier model, Identifier texture, String root, List<Part> parts, Map<String, Point> points, Map<String, ModuleSlot> moduleSlots) {
        this.model = model;
        this.texture = texture;
        this.root = root;
        this.parts = List.copyOf(parts);
        this.points = Map.copyOf(points);
        this.moduleSlots = Map.copyOf(moduleSlots);
    }

    public Identifier model() {
        return model;
    }

    public Identifier texture() {
        return texture;
    }

    public String root() {
        return root;
    }

    public List<Part> parts() {
        return parts;
    }

    public Map<String, Point> points() {
        return points;
    }

    public Point point(String name) {
        return points.get(name);
    }

    public Map<String, ModuleSlot> moduleSlots() {
        return moduleSlots;
    }

    public static CzoGunLayout fromJson(JsonObject json) {
        Identifier model = parseId(requiredString(json, "model"));
        Identifier texture = parseId(requiredString(json, "texture"));
        String root = requiredString(json, "root");

        List<Part> parts = new ArrayList<>();
        JsonArray partArray = json.getAsJsonArray("parts");
        if (partArray != null) {
            for (JsonElement element : partArray) {
                JsonObject obj = element.getAsJsonObject();
                parts.add(new Part(
                        requiredString(obj, "name"),
                        stringOr(obj, "parent", root),
                        vec(obj, "translation"),
                        vec(obj, "rotation_degrees"),
                        vec(obj, "scale")
                ));
            }
        }

        Map<String, Point> points = new HashMap<>();
        JsonArray pointArray = json.getAsJsonArray("points");
        if (pointArray != null) {
            for (JsonElement element : pointArray) {
                JsonObject obj = element.getAsJsonObject();
                Point point = new Point(
                        requiredString(obj, "name"),
                        stringOr(obj, "parent", root),
                        vec(obj, "translation"),
                        vec(obj, "rotation_degrees"),
                        vec(obj, "scale")
                );
                points.put(point.name(), point);
            }
        }

        Map<String, ModuleSlot> moduleSlots = new HashMap<>();
        JsonObject slotsJson = json.getAsJsonObject("module_slots");
        if (slotsJson != null) {
            for (Map.Entry<String, JsonElement> entry : slotsJson.entrySet()) {
                JsonObject obj = entry.getValue().getAsJsonObject();
                List<String> hidden = new ArrayList<>();
                JsonArray hiddenJson = obj.getAsJsonArray("hide_parts_when_installed");
                if (hiddenJson != null) {
                    for (JsonElement hiddenElement : hiddenJson) {
                        hidden.add(hiddenElement.getAsString());
                    }
                }
                moduleSlots.put(entry.getKey(), new ModuleSlot(
                        entry.getKey(),
                        requiredString(obj, "socket"),
                        hidden
                ));
            }
        }

        return new CzoGunLayout(model, texture, root, parts, points, moduleSlots);
    }

    private static Identifier parseId(String raw) {
        int split = raw.indexOf(':');
        if (split < 0) {
            return Identifier.fromNamespaceAndPath("minecraft", raw);
        }
        return Identifier.fromNamespaceAndPath(raw.substring(0, split), raw.substring(split + 1));
    }

    private static String requiredString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing string: " + key);
        }
        return element.getAsString();
    }

    private static String stringOr(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static Vector3f vec(JsonObject json, String key) {
        JsonArray array = json.getAsJsonArray(key);
        if (array == null || array.size() < 3) {
            return new Vector3f(0.0F, 0.0F, 0.0F);
        }
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public record Part(String name, String parent, Vector3f translation, Vector3f rotationDegrees, Vector3f scale) {
    }

    public record Point(String name, String parent, Vector3f translation, Vector3f rotationDegrees, Vector3f scale) {
    }

    public record ModuleSlot(String name, String socket, List<String> hidePartsWhenInstalled) {
        public ModuleSlot {
            hidePartsWhenInstalled = Collections.unmodifiableList(new ArrayList<>(hidePartsWhenInstalled));
        }
    }
}
