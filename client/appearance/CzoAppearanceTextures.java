package com.czo.client.appearance;

import com.czo.CZO;

import net.minecraft.resources.Identifier;

/** Texture registry for hardcoded CZO appearance parts. */
public final class CzoAppearanceTextures {
    public static final Identifier BASE_SKIN = tex("base_layer_skin.png");
    public static final Identifier BASE_PANTS = tex("base_jeans.png");
    public static final Identifier BASE_BOOTS = tex("base_boots.png");
    public static final Identifier BASE_TOP = tex("base_sweater.png");

    public static final Identifier ISKATEL_SKIN = tex("iskatel_base.png");
    public static final Identifier ISKATEL_TOP = tex("base_iskatel.png");
    public static final Identifier ISKATEL_PANTS = tex("pants_iskatel.png");
    public static final Identifier ISKATEL_BOOTS = tex("boots_iskatel.png");

    public static final Identifier CONTRABANDIST_SKIN = tex("kontrab_base.png");
    public static final Identifier CONTRABANDIST_TOP = tex("coat_contraban.png");
    public static final Identifier CONTRABANDIST_PANTS = tex("pants_contrab.png");
    public static final Identifier CONTRABANDIST_BOOTS = tex("kedi_kontrabandist.png");

    public static final Identifier BASE_COMBINED = tex("base_combined.png");
    public static final Identifier ISKATEL_COMBINED = tex("iskatel_combined.png");
    public static final Identifier CONTRABANDIST_COMBINED = tex("contrabandist_combined.png");

    public static final Identifier BASE_PREVIEW = gui("base_player_preview.png");
    public static final Identifier ISKATEL_PREVIEW = gui("iskatel_player_preview.png");
    public static final Identifier CONTRABANDIST_PREVIEW = gui("contrabandist_player_preview.png");

    private CzoAppearanceTextures() {
    }

    public static Identifier combined(String fileName) {
        return tex("combined/" + fileName);
    }

    private static Identifier tex(String fileName) {
        return Identifier.fromNamespaceAndPath(CZO.MODID, "textures/entity/player/customization/" + fileName);
    }

    private static Identifier gui(String fileName) {
        return Identifier.fromNamespaceAndPath(CZO.MODID, "textures/gui/customization/" + fileName);
    }
}
