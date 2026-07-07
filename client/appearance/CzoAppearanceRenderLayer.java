package com.czo.client.appearance;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders CZO appearance as one pre-composited player skin.
 *
 * The old implementation submitted skin/pants/boots/top as four full player models,
 * which caused cursed doubled outer layers and z-fighting. Now the selection is
 * composed beforehand into a single 64x64 texture and submitted once.
 */
public final class CzoAppearanceRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public CzoAppearanceRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        if (Minecraft.getInstance() != null) {
            CzoAppearanceSelection.sanitize(Minecraft.getInstance().player);
        }

        submitTexture(collector, poseStack, lightCoords, CzoAppearanceSelection.combinedTexture());
    }

    private void submitTexture(SubmitNodeCollector collector, PoseStack poseStack, int lightCoords, Identifier texture) {
        PlayerModel model = this.getParentModel();

        // Base model.
        submitPart(collector, poseStack, lightCoords, texture, model.head);
        submitPart(collector, poseStack, lightCoords, texture, model.body);
        submitPart(collector, poseStack, lightCoords, texture, model.rightArm);
        submitPart(collector, poseStack, lightCoords, texture, model.leftArm);
        submitPart(collector, poseStack, lightCoords, texture, model.rightLeg);
        submitPart(collector, poseStack, lightCoords, texture, model.leftLeg);

        // Vanilla second skin layer: hat, jacket, sleeves, pants.
        // It is submitted from the same combined texture, so it no longer stacks
        // separately for every clothing category.
        submitPart(collector, poseStack, lightCoords, texture, model.hat);
        submitPart(collector, poseStack, lightCoords, texture, model.jacket);
        submitPart(collector, poseStack, lightCoords, texture, model.rightSleeve);
        submitPart(collector, poseStack, lightCoords, texture, model.leftSleeve);
        submitPart(collector, poseStack, lightCoords, texture, model.rightPants);
        submitPart(collector, poseStack, lightCoords, texture, model.leftPants);
    }

    private static void submitPart(SubmitNodeCollector collector, PoseStack poseStack, int lightCoords, Identifier texture, ModelPart part) {
        if (part == null || !part.visible) {
            return;
        }

        collector.submitModelPart(
                part,
                poseStack,
                RenderTypes.entityCutout(texture),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                null
        );
    }
}
