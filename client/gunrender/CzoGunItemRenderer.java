package com.czo.client.gunrender;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

/** Stage 1K renderer: OBJ parts + layout sockets + first visual module support. No animations/sounds yet. */
public final class CzoGunItemRenderer {
    private static float aimProgress;

    private CzoGunItemRenderer() {
    }

    public static void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        CzoGunLayout layout = CzoGunLayoutCache.get(CzoGunVisualResolver.layoutFor(stack));
        if (layout == null) {
            return;
        }

        CzoObjGunModel model = CzoObjGunModelCache.get(layout.model());
        if (model == null) {
            return;
        }

        Set<String> hidden = CzoGunVisualResolver.hiddenParts(stack, layout);

        poseStack.pushPose();
        applyBaseTransform(context, poseStack, model);

        renderLayoutParts(layout, model, hidden, poseStack, buffer, light, overlay);
        renderInstalledModules(stack, layout, poseStack, buffer, light, overlay);

        poseStack.popPose();
    }


    /**
     * Renders the same assembled gun model for custom screens, without vanilla item GUI transform.
     * The caller places the PoseStack at the desired screen/world position before calling this.
     */
    public static void renderModelPreview(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, float scale, float yawDeg, float pitchDeg, float rollDeg) {
        CzoGunLayout layout = CzoGunLayoutCache.get(CzoGunVisualResolver.layoutFor(stack));
        if (layout == null) {
            return;
        }

        CzoObjGunModel model = CzoObjGunModelCache.get(layout.model());
        if (model == null) {
            return;
        }

        poseStack.pushPose();
        // CZO model space: X right, Y barrel, Z up. Screen preview looks at the weapon from the side.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.scale(scale, scale, scale);

        Set<String> hidden = CzoGunVisualResolver.hiddenParts(stack, layout);
        renderLayoutParts(layout, model, hidden, poseStack, buffer, light, overlay);
        renderInstalledModules(stack, layout, poseStack, buffer, light, overlay);

        poseStack.popPose();
    }

    private static void renderLayoutParts(CzoGunLayout layout, CzoObjGunModel model, Set<String> hidden, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        for (CzoGunLayout.Part part : layout.parts()) {
            if (!hidden.contains(part.name())) {
                poseStack.pushPose();
                applyPartTransform(part, poseStack);
                model.renderPart(part.name(), layout.texture(), poseStack, buffer, light, overlay);
                poseStack.popPose();
            }
        }
    }

    private static void renderInstalledModules(ItemStack stack, CzoGunLayout gunLayout, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        for (CzoGunVisualResolver.VisualModule module : CzoGunVisualResolver.installedVisualModules(stack, gunLayout)) {
            CzoGunLayout.ModuleSlot slot = gunLayout.moduleSlots().get(module.slot());
            if (slot == null) {
                continue;
            }

            CzoGunLayout.Point socket = gunLayout.point(slot.socket());
            if (socket == null) {
                continue;
            }

            CzoGunLayout moduleLayout = CzoGunLayoutCache.get(module.layout());
            if (moduleLayout == null) {
                continue;
            }

            CzoObjGunModel moduleModel = CzoObjGunModelCache.get(moduleLayout.model());
            if (moduleModel == null) {
                continue;
            }

            poseStack.pushPose();
            applyPointTransform(socket, poseStack);
            renderLayoutParts(moduleLayout, moduleModel, Set.of(), poseStack, buffer, light, overlay);
            poseStack.popPose();
        }
    }

    private static void applyBaseTransform(ItemDisplayContext context, PoseStack poseStack, CzoObjGunModel model) {
        // CZO Blender space after OBJ axis fix: X right, Y forward/barrel, Z up.
        // Minecraft item/camera space wants barrel roughly into -Z, so X -90 maps CZO +Y -> render -Z.
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                float aim = firstPersonAimAmount(true);
                poseStack.translate(
                        lerp(0.56F, 0.50F, aim),
                        lerp(-0.25F, -0.34F, aim),
                        lerp(-0.72F, -0.62F, aim)
                );
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(lerp(-4.0F, 0.0F, aim)));
                poseStack.scale(lerp(1.65F, 1.85F, aim), lerp(1.65F, 1.85F, aim), lerp(1.65F, 1.85F, aim));
            }
            case FIRST_PERSON_LEFT_HAND -> {
                float aim = firstPersonAimAmount(true);
                poseStack.translate(
                        lerp(0.44F, 0.50F, aim),
                        lerp(-0.25F, -0.34F, aim),
                        lerp(-0.72F, -0.62F, aim)
                );
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(lerp(4.0F, 0.0F, aim)));
                poseStack.scale(-lerp(1.65F, 1.85F, aim), lerp(1.65F, 1.85F, aim), lerp(1.65F, 1.85F, aim));
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                firstPersonAimAmount(false);
                poseStack.translate(0.0F, 0.13F, -0.02F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
                poseStack.scale(0.95F, 0.95F, 0.95F);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                firstPersonAimAmount(false);
                poseStack.translate(0.0F, 0.13F, -0.02F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
                poseStack.scale(-0.95F, 0.95F, 0.95F);
            }
            case GUI -> {
                firstPersonAimAmount(false);
                CzoGunPreviewGui.PreviewContext preview = CzoGunPreviewGui.currentPreviewContext();
                poseStack.translate(0.5F, 0.5F, 0.0F);

                if (preview != null) {
                    // GunModificationScreen: true OBJ/Layout preview, centered by model bounds.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(preview.rollDegrees()));
                    poseStack.mulPose(Axis.YP.rotationDegrees(preview.yawDegrees()));
                    poseStack.mulPose(Axis.XP.rotationDegrees(preview.pitchDegrees()));
                    applyGuiModelFit(model, poseStack, preview.targetLocalSize());
                } else {
                    // Normal 16x16 inventory / hotbar icon: side-ish readable weapon silhouette.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-35.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
                    applyGuiModelFit(model, poseStack, CzoGunPreviewGui.ICON_TARGET_LOCAL_SIZE);
                }
            }
            case GROUND -> {
                firstPersonAimAmount(false);
                poseStack.translate(0.5F, 0.08F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.scale(0.85F, 0.85F, 0.85F);
            }
            case FIXED -> {
                firstPersonAimAmount(false);
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-38.0F));
                poseStack.scale(0.85F, 0.85F, 0.85F);
            }
            default -> {
                firstPersonAimAmount(false);
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(1.0F, 1.0F, 1.0F);
            }
        }
    }


    private static void applyGuiModelFit(CzoObjGunModel model, PoseStack poseStack, float targetLocalSize) {
        float fitScale = targetLocalSize / Math.max(0.001F, model.bounds().maxDimension());
        Vector3f center = model.bounds().center();
        poseStack.scale(fitScale, fitScale, fitScale);
        poseStack.translate(-center.x(), -center.y(), -center.z());
    }

    private static float firstPersonAimAmount(boolean firstPerson) {
        float target = 0.0F;
        if (firstPerson) {
            long window = GLFW.glfwGetCurrentContext();
            if (window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {
                target = 1.0F;
            }
        }
        aimProgress += (target - aimProgress) * 0.35F;
        if (aimProgress < 0.001F) {
            aimProgress = 0.0F;
        }
        if (aimProgress > 0.999F) {
            aimProgress = 1.0F;
        }
        return aimProgress;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static void applyPartTransform(CzoGunLayout.Part part, PoseStack poseStack) {
        poseStack.translate(part.translation().x(), part.translation().y(), part.translation().z());
        poseStack.mulPose(Axis.XP.rotationDegrees(part.rotationDegrees().x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(part.rotationDegrees().y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(part.rotationDegrees().z()));
        poseStack.scale(part.scale().x(), part.scale().y(), part.scale().z());
    }

    private static void applyPointTransform(CzoGunLayout.Point point, PoseStack poseStack) {
        poseStack.translate(point.translation().x(), point.translation().y(), point.translation().z());
        poseStack.mulPose(Axis.XP.rotationDegrees(point.rotationDegrees().x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(point.rotationDegrees().y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(point.rotationDegrees().z()));
        poseStack.scale(point.scale().x(), point.scale().y(), point.scale().z());
    }
}
