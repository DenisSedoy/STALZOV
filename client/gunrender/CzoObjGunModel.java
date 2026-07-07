package com.czo.client.gunrender;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny OBJ model split by 'o part_name'.
 *
 * ВАЖНО:
 * Minecraft RenderTypes.entityCutout(...) в этой версии собирает QUADS, а не TRIANGLES.
 * Поэтому OBJ-треугольники нельзя отправлять как 3 вершины — каждая 4-я вершина будет взята
 * из следующего треугольника, и модель превратится в мясо. Каждый треугольник отправляем
 * как degenerate quad: A, B, C, C.
 */
public final class CzoObjGunModel {
    /**
     * Пока держим true, потому что OBJ из Blender часто односторонний.
     * Если когда-нибудь перейдём на RenderType no-cull, можно выключить.
     */
    private static final boolean DOUBLE_SIDED_RENDER = true;

    /**
     * Текущий OBJ-экспорт ПМа приходит из Blender не в CZO-осях layout'а.
     * CZO: X right, Y forward/barrel, Z up.
     */
    private static final boolean CONVERT_BLENDER_OBJ_AXES = true;

    private final Map<String, ObjPart> parts;
    private final Bounds bounds;

    public CzoObjGunModel(Map<String, ObjPart> parts) {
        this.parts = Map.copyOf(parts);
        this.bounds = computeBounds(this.parts);
    }

    public Bounds bounds() {
        return bounds;
    }

    public boolean hasPart(String name) {
        return parts.containsKey(name);
    }

    public void renderPart(String name, Identifier texture, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        ObjPart part = parts.get(name);
        if (part == null) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderTypes.entityCutout(texture));
        PoseStack.Pose pose = poseStack.last();

        for (Triangle triangle : part.triangles()) {
            emitTriangleAsQuad(consumer, pose, triangle.a(), triangle.b(), triangle.c(), light, overlay, false);

            if (DOUBLE_SIDED_RENDER) {
                emitTriangleAsQuad(consumer, pose, triangle.c(), triangle.b(), triangle.a(), light, overlay, true);
            }
        }
    }

    /**
     * RenderType ждёт QUAD: 4 вершины. OBJ даёт triangle: 3 вершины.
     * Дублируем C, чтобы получить вырожденный четырёхугольник A-B-C-C.
     */
    private static void emitTriangleAsQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            ObjVertex a,
            ObjVertex b,
            ObjVertex c,
            int light,
            int overlay,
            boolean backFace
    ) {
        emit(consumer, pose, a, light, overlay, backFace);
        emit(consumer, pose, b, light, overlay, backFace);
        emit(consumer, pose, c, light, overlay, backFace);
        emit(consumer, pose, c, light, overlay, backFace);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, ObjVertex vertex, int light, int overlay, boolean backFace) {
        Vector3f normal = backFace ? new Vector3f(vertex.normal()).negate() : vertex.normal();
        consumer.addVertex(pose, vertex.position().x(), vertex.position().y(), vertex.position().z())
                .setColor(255, 255, 255, 255)
                .setUv(vertex.uv().x(), vertex.uv().y())
                .setOverlay(overlay)
                .setLight(0x00F000F0)
                .setNormal(pose, normal.x(), normal.y(), normal.z());
    }

    private static Bounds computeBounds(Map<String, ObjPart> parts) {
        boolean any = false;
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        for (ObjPart part : parts.values()) {
            for (Triangle triangle : part.triangles()) {
                any |= include(min, max, triangle.a().position());
                any |= include(min, max, triangle.b().position());
                any |= include(min, max, triangle.c().position());
            }
        }

        if (!any) {
            return new Bounds(new Vector3f(), new Vector3f());
        }

        return new Bounds(min, max);
    }

    private static boolean include(Vector3f min, Vector3f max, Vector3f value) {
        min.x = Math.min(min.x, value.x());
        min.y = Math.min(min.y, value.y());
        min.z = Math.min(min.z, value.z());
        max.x = Math.max(max.x, value.x());
        max.y = Math.max(max.y, value.y());
        max.z = Math.max(max.z, value.z());
        return true;
    }

    public static CzoObjGunModel parse(List<String> lines) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        Map<String, ObjPart.Mutable> mutableParts = new HashMap<>();

        String currentObject = "default";
        mutableParts.put(currentObject, new ObjPart.Mutable(currentObject));

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] split = line.split("\\s+");
            switch (split[0]) {
                case "o", "g" -> {
                    if (split.length >= 2) {
                        currentObject = split[1];
                        mutableParts.computeIfAbsent(currentObject, ObjPart.Mutable::new);
                    }
                }
                case "v" -> positions.add(convertObjExportedVertexToCzo(new Vector3f(
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        Float.parseFloat(split[3])
                )));
                case "vt" -> uvs.add(new Vector2f(
                        Float.parseFloat(split[1]),
                        1.0F - Float.parseFloat(split[2])
                ));
                case "vn" -> normals.add(convertObjExportedNormalToCzo(new Vector3f(
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        Float.parseFloat(split[3])
                )));
                case "f" -> {
                    if (split.length < 4) {
                        continue;
                    }

                    ObjVertex first = parseVertex(split[1], positions, uvs, normals);
                    for (int i = 2; i < split.length - 1; i++) {
                        ObjVertex b = parseVertex(split[i], positions, uvs, normals);
                        ObjVertex c = parseVertex(split[i + 1], positions, uvs, normals);
                        mutableParts.get(currentObject).triangles.add(new Triangle(first, b, c));
                    }
                }
                default -> {
                }
            }
        }

        Map<String, ObjPart> finalParts = new HashMap<>();
        for (ObjPart.Mutable mutable : mutableParts.values()) {
            if (!mutable.triangles.isEmpty()) {
                finalParts.put(mutable.name, new ObjPart(mutable.name, List.copyOf(mutable.triangles)));
            }
        }
        return new CzoObjGunModel(finalParts);
    }

    /**
     * Current PM export comes out as:
     * OBJ X = CZO X,
     * OBJ Z = -CZO Y,
     * OBJ Y = CZO Z.
     */
    private static Vector3f convertObjExportedVertexToCzo(Vector3f v) {
        if (!CONVERT_BLENDER_OBJ_AXES) {
            return v;
        }
        return new Vector3f(v.x(), -v.z(), v.y());
    }

    private static Vector3f convertObjExportedNormalToCzo(Vector3f n) {
        if (!CONVERT_BLENDER_OBJ_AXES) {
            return n.normalize();
        }
        return new Vector3f(n.x(), -n.z(), n.y()).normalize();
    }

    private static ObjVertex parseVertex(String token, List<Vector3f> positions, List<Vector2f> uvs, List<Vector3f> normals) {
        String[] indices = token.split("/");
        Vector3f position = positions.get(resolveIndex(indices[0], positions.size()));
        Vector2f uv = indices.length > 1 && !indices[1].isEmpty()
                ? uvs.get(resolveIndex(indices[1], uvs.size()))
                : new Vector2f(0.0F, 0.0F);
        Vector3f normal = indices.length > 2 && !indices[2].isEmpty()
                ? normals.get(resolveIndex(indices[2], normals.size()))
                : new Vector3f(0.0F, 1.0F, 0.0F);
        return new ObjVertex(position, uv, normal);
    }

    private static int resolveIndex(String raw, int size) {
        int index = Integer.parseInt(raw);
        return index < 0 ? size + index : index - 1;
    }

    public record Bounds(Vector3f min, Vector3f max) {
        public Vector3f center() {
            return new Vector3f(min).add(max).mul(0.5F);
        }

        public Vector3f size() {
            return new Vector3f(max).sub(min);
        }

        public float maxDimension() {
            Vector3f size = size();
            return Math.max(0.001F, Math.max(size.x(), Math.max(size.y(), size.z())));
        }
    }

    public record ObjPart(String name, List<Triangle> triangles) {
        private static final class Mutable {
            private final String name;
            private final List<Triangle> triangles = new ArrayList<>();

            private Mutable(String name) {
                this.name = name;
            }
        }
    }

    public record Triangle(ObjVertex a, ObjVertex b, ObjVertex c) {
    }

    public record ObjVertex(Vector3f position, Vector2f uv, Vector3f normal) {
    }
}
