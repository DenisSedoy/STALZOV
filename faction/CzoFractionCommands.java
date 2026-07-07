package com.czo.faction;

import com.czo.CZO;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Commands:
 * /fraction join <player> <fraction>
 * /fraction leave <player>
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoFractionCommands {
    private CzoFractionCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fraction")
                .then(Commands.literal("join")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("fraction", StringArgumentType.word())
                                        .executes(ctx -> join(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "fraction")
                                        )))))
                .then(Commands.literal("leave")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> leave(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))));

        event.getDispatcher().register(root);
    }

    private static int join(CommandSourceStack source, ServerPlayer player, String rawFraction) {
        String id = CzoFractions.normalize(rawFraction);
        String display = CzoFractions.displayName(id);
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = ensureTeam(source.getServer(), scoreboard, id, display);

        removeFromCzoTeams(scoreboard, player);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " теперь в группировке: " + display), true);
        player.sendSystemMessage(Component.literal("Группировка: " + display), true);
        return 1;
    }

    private static int leave(CommandSourceStack source, ServerPlayer player) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        removeFromCzoTeams(scoreboard, player);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " покинул группировку CZO"), true);
        player.sendSystemMessage(Component.literal("Группировка снята"), true);
        return 1;
    }

    private static PlayerTeam ensureTeam(MinecraftServer server, Scoreboard scoreboard, String id, String display) {
        String teamName = CzoFractions.teamName(id);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        team.setDisplayName(Component.literal(display));
        return team;
    }

    private static void removeFromCzoTeams(Scoreboard scoreboard, ServerPlayer player) {
        String playerName = player.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(playerName);
        if (current != null && current.getName().startsWith(CzoFractions.TEAM_PREFIX)) {
            scoreboard.removePlayerFromTeam(playerName, current);
        }
    }
}
