package com.czo.client.appearance;

import com.czo.CZO;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Appearance rendering is now handled by CzoAvatarRendererTextureMixin.
 *
 * The previous layer-based approach rendered CZO textures after the vanilla
 * player skin, which caused the original skin to bleed through or z-fight.
 * Do not add a separate player render layer here.
 */
@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoAppearanceClientEvents {
    private CzoAppearanceClientEvents() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Intentionally empty.
        // The base avatar texture is replaced by CzoAvatarRendererTextureMixin.
    }
}
