package de.jvstvshd.velocitypunishment.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class KickCommand implements Command<CommandSource> {

    private final VelocityPunishmentPlugin plugin;

    public KickCommand(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
    }

    public static LiteralCommandNode<CommandSource> create(VelocityPunishmentPlugin plugin) {
        var command = new KickCommand(plugin);
        var node = LiteralArgumentBuilder.<CommandSource>literal("kick").executes(command).build();
        node.addChild(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word()).executes(command).build());
        node.addChild(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString()).executes(command).build());
        return node;
    }

    @Override
    public int run(CommandContext<CommandSource> context) {
        var source = context.getSource();
        if (context.getArguments().size() < 1) {
            source.sendMessage(plugin.getMessageProvider().provide("command.kick.usage", source, true).color(NamedTextColor.RED));
            return 0;
        }
        String playerArgument = StringArgumentType.getString(context, "player");
        Optional<Player> playerOptional = plugin.getServer().getPlayer(playerArgument);
        if (playerOptional.isEmpty()) {
            try {
                playerOptional = plugin.getServer().getPlayer(Util.parseUuid(playerArgument));
            } catch (Exception e) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                return 0;
            }
        }
        if (playerOptional.isEmpty()) {
            source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true, Component.text(playerArgument).color(NamedTextColor.YELLOW)));
            return 0;
        }
        var player = playerOptional.get();
        Component reason = new PunishmentHelper().parseComponent(1, context, Component.text("Kick").color(NamedTextColor.DARK_RED));
        player.disconnect(reason);
        return 0;
    }
}
