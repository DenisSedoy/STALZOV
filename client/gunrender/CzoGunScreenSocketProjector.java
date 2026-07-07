package com.czo.client.gunrender;

import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Projects Blender-exported layout sockets into the 2D modification screen.
 * It now mirrors the preview renderer's centering/fitting, so sockets visually track the OBJ.
 */
public final class CzoGunScreenSocketProjector {
    private CzoGunScreenSocketProjector() {
    }

    public static Vector2f projectSlot(
            ItemStack stack,
            String slotName,
            float centerX,
            float centerY,
            float pixelsPerItemUnit,
            float targetLocalSize,
            float yawDeg,
            float pitchDeg,
            float rollDeg
    ) {
        CzoGunLayout layout = CzoGunLayoutCache.get(CzoGunVisualResolver.layoutFor(stack));
        if (layout == null) {
            return new Vector2f(centerX, centerY);
        }

        CzoGunLayout.ModuleSlot slot = layout.moduleSlots().get(slotName);
        if (slot == null) {
            return new Vector2f(centerX, centerY);
        }

        return projectPoint(stack, layout, slot.socket(), centerX, centerY, pixelsPerItemUnit, targetLocalSize, yawDeg, pitchDeg, rollDeg);
    }

    public static Vector2f projectPoint(
            ItemStack stack,
            CzoGunLayout layout,
            String pointName,
            float centerX,
            float centerY,
            float pixelsPerItemUnit,
            float targetLocalSize,
            float yawDeg,
            float pitchDeg,
            float rollDeg
    ) {
        CzoGunLayout.Point point = layout.point(pointName);
        if (point == null) {
            return new Vector2f(centerX, centerY);
        }

        CzoObjGunModel model = CzoObjGunModelCache.get(layout.model());
        Vector3f modelCenter = model == null ? new Vector3f() : model.bounds().center();
        float fitScale = model == null ? 1.0F : fitScale(model, targetLocalSize);

        Vector3f p = new Vector3f(point.translation())
                .sub(modelCenter)
                .mul(fitScale);

        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(rollDeg))
                .rotateY((float) Math.toRadians(yawDeg))
                .rotateX((float) Math.toRadians(pitchDeg));
        p.rotate(rotation);

        // Orthographic GUI projection: local X -> screen X, local Z -> screen -Y.
        return new Vector2f(
                centerX + p.x() * pixelsPerItemUnit,
                centerY - p.z() * pixelsPerItemUnit
        );
    }

    private static float fitScale(CzoObjGunModel model, float targetLocalSize) {
        return targetLocalSize / Math.max(0.001F, model.bounds().maxDimension());
    }
}
