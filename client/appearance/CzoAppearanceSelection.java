package com.czo.client.appearance;

import com.czo.faction.CzoFractions;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;


/**
 * Temporary client-side selection storage for the first appearance pass.
 *
 * Server persistence/sync will replace this later. The important technical rule
 * is already enforced here: skin -> pants -> boots -> top/jacket.
 */
public final class CzoAppearanceSelection {
    public static final String SKIN_BASE = "base";
    public static final String SKIN_ISKATEL = "iskatel";
    public static final String SKIN_CONTRABANDIST = "contrabandist";

    public static final String TOP_BASE_SWEATER = "base_sweater";
    public static final String TOP_ISKATEL = "iskatel_jacket";
    public static final String TOP_CONTRABANDIST = "contrabandist_coat";

    public static final String PANTS_BASE_JEANS = "base_jeans";
    public static final String PANTS_ISKATEL = "iskatel_pants";
    public static final String PANTS_CONTRABANDIST = "contrabandist_pants";

    public static final String BOOTS_BASE = "base_boots";
    public static final String BOOTS_ISKATEL = "iskatel_boots";
    public static final String BOOTS_CONTRABANDIST = "contrabandist_sneakers";

    private static String skin = SKIN_BASE;
    private static String top = TOP_BASE_SWEATER;
    private static String pants = PANTS_BASE_JEANS;
    private static String boots = BOOTS_BASE;

    private CzoAppearanceSelection() {
    }

    public static String skin() {
        return skin;
    }

    public static String top() {
        return top;
    }

    public static String pants() {
        return pants;
    }

    public static String boots() {
        return boots;
    }

    public static void setSkin(String value) {
        skin = value == null ? SKIN_BASE : value;
    }

    public static void setTop(String value) {
        top = value == null ? TOP_BASE_SWEATER : value;
    }

    public static void setPants(String value) {
        pants = value == null ? PANTS_BASE_JEANS : value;
    }

    public static void setBoots(String value) {
        boots = value == null ? BOOTS_BASE : value;
    }

    /** Removes faction-locked appearance when the player no longer has access. */
    public static void sanitize(LocalPlayer player) {
        if (!isSelectionUnlocked(player, skin)) {
            skin = SKIN_BASE;
        }
        if (!isSelectionUnlocked(player, top)) {
            top = TOP_BASE_SWEATER;
        }
        if (!isSelectionUnlocked(player, pants)) {
            pants = PANTS_BASE_JEANS;
        }
        if (!isSelectionUnlocked(player, boots)) {
            boots = BOOTS_BASE;
        }
    }

    private static boolean isSelectionUnlocked(LocalPlayer player, String id) {
        if (id == null) {
            return true;
        }
        if (isContrabandistId(id)) {
            return isContrabandist(player);
        }
        if (isPathfinderId(id)) {
            return isPathfinder(player);
        }
        return true;
    }

    public static boolean isUnlocked(LocalPlayer player, Unlock required) {
        if (required == Unlock.COMMON) {
            return true;
        }
        if (required == Unlock.CONTRABANDIST) {
            return isContrabandist(player);
        }
        if (required == Unlock.PATHFINDER) {
            return isPathfinder(player);
        }
        return false;
    }

    public static Identifier skinTexture() {
        return switch (skin) {
            case SKIN_ISKATEL -> CzoAppearanceTextures.ISKATEL_SKIN;
            case SKIN_CONTRABANDIST -> CzoAppearanceTextures.CONTRABANDIST_SKIN;
            default -> CzoAppearanceTextures.BASE_SKIN;
        };
    }

    public static Identifier pantsTexture() {
        return switch (pants) {
            case PANTS_ISKATEL -> CzoAppearanceTextures.ISKATEL_PANTS;
            case PANTS_CONTRABANDIST -> CzoAppearanceTextures.CONTRABANDIST_PANTS;
            default -> CzoAppearanceTextures.BASE_PANTS;
        };
    }

    public static Identifier bootsTexture() {
        return switch (boots) {
            case BOOTS_ISKATEL -> CzoAppearanceTextures.ISKATEL_BOOTS;
            case BOOTS_CONTRABANDIST -> CzoAppearanceTextures.CONTRABANDIST_BOOTS;
            default -> CzoAppearanceTextures.BASE_BOOTS;
        };
    }


    public static Identifier combinedTexture() {
        return CzoAppearanceTextures.combined(combinedFileName());
    }

    public static String combinedFileName() {
        return skin + "__" + pants + "__" + boots + "__" + top + ".png";
    }

    public static Identifier topTexture() {
        return switch (top) {
            case TOP_ISKATEL -> CzoAppearanceTextures.ISKATEL_TOP;
            case TOP_CONTRABANDIST -> CzoAppearanceTextures.CONTRABANDIST_TOP;
            default -> CzoAppearanceTextures.BASE_TOP;
        };
    }

    public static Identifier previewTexture() {
        if (SKIN_CONTRABANDIST.equals(skin)
                || TOP_CONTRABANDIST.equals(top)
                || PANTS_CONTRABANDIST.equals(pants)
                || BOOTS_CONTRABANDIST.equals(boots)) {
            return CzoAppearanceTextures.CONTRABANDIST_PREVIEW;
        }
        if (SKIN_ISKATEL.equals(skin)
                || TOP_ISKATEL.equals(top)
                || PANTS_ISKATEL.equals(pants)
                || BOOTS_ISKATEL.equals(boots)) {
            return CzoAppearanceTextures.ISKATEL_PREVIEW;
        }
        return CzoAppearanceTextures.BASE_PREVIEW;
    }

    public static String caption() {
        return "кожа → штаны → ботинки → куртка";
    }

    private static boolean isPathfinderId(String id) {
        return SKIN_ISKATEL.equals(id)
                || TOP_ISKATEL.equals(id)
                || PANTS_ISKATEL.equals(id)
                || BOOTS_ISKATEL.equals(id);
    }

    private static boolean isContrabandistId(String id) {
        return SKIN_CONTRABANDIST.equals(id)
                || TOP_CONTRABANDIST.equals(id)
                || PANTS_CONTRABANDIST.equals(id)
                || BOOTS_CONTRABANDIST.equals(id);
    }

    private static boolean isContrabandist(LocalPlayer player) {
        return player != null && CzoFractions.isContrabandist(player);
    }

    private static boolean isPathfinder(LocalPlayer player) {
        // In the current CZO faction setup every non-contrabandist player is treated
        // as an Iskateli/Pathfinder member. The previous HUD-subtitle string check
        // was too fragile and could hide all Iskateli clothes if the subtitle text
        // changed even a little.
        return player != null && !CzoFractions.isContrabandist(player);
    }

    public enum Unlock {
        COMMON,
        PATHFINDER,
        CONTRABANDIST
    }
}
