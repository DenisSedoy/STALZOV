package com.czo.faction;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/** Small server/client helper for CZO player factions. Scoreboard teams are used so the result syncs to clients. */
public final class CzoFractions {
    public static final String TEAM_PREFIX = "czo_fraction_";
    public static final String ISKATELI_ID = "iskateli";
    public static final String KONTRABANDISTY_ID = "kontrabandisty";

    private CzoFractions() {
    }

    public static String teamName(String id) {
        return TEAM_PREFIX + normalize(id);
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return ISKATELI_ID;
        }
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('ё', 'е')
                .replace(" ", "_")
                .replace("-", "_");
        return switch (value) {
            case "iskateli", "искатель", "искатели", "stalker", "stalkers", "сталкер", "сталкеры", "pathfinder", "pathfinders" -> ISKATELI_ID;
            case "kontrabandisty", "контрабандист", "контрабандисты", "contrabandist", "contrabandists", "smuggler", "smugglers", "bandit", "bandits", "бандит", "бандиты" -> KONTRABANDISTY_ID;
            default -> value;
        };
    }

    public static String displayName(String raw) {
        return switch (normalize(raw)) {
            case ISKATELI_ID -> "Искатели";
            case KONTRABANDISTY_ID -> "Контрабандисты";
            default -> raw == null || raw.isBlank() ? "Искатели" : raw;
        };
    }

    public static String hudSubtitle(Player player) {
        return displayName(currentId(player)) + " / CZO";
    }

    public static String currentId(Player player) {
        if (player == null || player.getTeam() == null) {
            return ISKATELI_ID;
        }
        Team team = player.getTeam();
        String name = team.getName();
        if (name != null && name.startsWith(TEAM_PREFIX)) {
            return normalize(name.substring(TEAM_PREFIX.length()));
        }
        return ISKATELI_ID;
    }

    public static boolean isContrabandist(Player player) {
        return KONTRABANDISTY_ID.equals(currentId(player));
    }
}
