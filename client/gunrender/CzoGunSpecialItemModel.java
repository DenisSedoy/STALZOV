package com.czo.client.gunrender;

import com.czo.CZO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Minecraft 26.1.2 item-render entrypoint.
 * Replaces the old IClientItemExtensions / BlockEntityWithoutLevelRenderer path.
 */
public record CzoGunSpecialItemModel(Identifier base) implements ItemModel.Unbaked {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CZO.MODID, "gun_renderer");

    public static final MapCodec<CzoGunSpecialItemModel> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("base").forGetter(CzoGunSpecialItemModel::base)
    ).apply(instance, CzoGunSpecialItemModel::new));

    @Override
    public MapCodec<? extends ItemModel.Unbaked> type() {
        return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(base);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
        ModelBaker baker = context.blockModelBaker();
        ResolvedModel resolved = baker.getModel(base);
        TextureSlots textureSlots = resolved.getTopTextureSlots();
        ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
        return new Baked(properties, transformation);
    }

    private record Baked(ModelRenderProperties properties, Matrix4fc transformation) implements ItemModel {
        private static final CustomRenderer RENDERER = new CustomRenderer();

        @Override
        public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
            renderState.appendModelIdentityElement(this);

            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();

            if (stack.hasFoil()) {
                ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
                layer.setFoilType(foilType);
                renderState.setAnimated();
                renderState.appendModelIdentityElement(foilType);
            }

            properties.applyToLayer(layer, displayContext);
            layer.setLocalTransform(transformation);
            layer.setupSpecialModel(RENDERER, new RenderArgument(stack.copy(), displayContext));
            renderState.appendModelIdentityElement(stack.getItem());
        }
    }

    private record RenderArgument(ItemStack stack, ItemDisplayContext displayContext) {
    }

    private static final class CustomRenderer implements SpecialModelRenderer<RenderArgument> {
        private static final Vector3fc[] EXTENTS = new Vector3fc[]{
                new Vector3f(-2.5F, -2.5F, -2.5F),
                new Vector3f(2.5F, 2.5F, 2.5F)
        };

        @Override
        public void submit(RenderArgument argument, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int seed) {
            CzoGunItemRenderer.renderByItem(
                    argument.stack(),
                    argument.displayContext(),
                    poseStack,
                    Minecraft.getInstance().renderBuffers().bufferSource(),
                    light,
                    overlay
            );
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            for (Vector3fc extent : EXTENTS) {
                consumer.accept(extent);
            }
        }

        @Override
        public RenderArgument extractArgument(ItemStack stack) {
            return new RenderArgument(stack.copy(), ItemDisplayContext.NONE);
        }
    }
}
