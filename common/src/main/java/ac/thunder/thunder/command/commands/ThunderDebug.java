package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.checks.debug.HitboxDebugHandler;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.platform.api.command.PlayerSelector;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThunderDebug implements BuildableCommand {

    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> thunderCommand = commandManager.commandBuilder("thunder", "th");

        // Register "debug" subcommand
        Command.Builder<Sender> debugCommand = thunderCommand
                .literal("debug", Description.of("Отладка предсказаний игрока"))
                .permission("thunder.debug")
                .optional("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleDebug);

        // Register "consoledebug" subcommand
        Command.Builder<Sender> consoleDebugCommand = thunderCommand
                .literal("consoledebug", Description.of("Отладка предсказаний в консоль"))
                .permission("thunder.consoledebug")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleConsoleDebug);

        Command.Builder<Sender> hitboxDebugCommand = thunderCommand
                .literal("hitboxdebug", Description.of("Отладка хитбоксов"))
                .permission("thunder.hitboxdebug")
                .optional("target", arguments.singlePlayerSelectorParser(), Description.of("Игрок для отладки (по умолчанию вы)"))
                .handler(this::handleHitboxDebug);

        // Register command
        commandManager.command(debugCommand);
        commandManager.command(consoleDebugCommand);
        commandManager.command(hitboxDebugCommand);
    }

    private void handleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        ThunderPlayer targetThunderPlayer = parseTarget(sender, playerSelector == null ? sender : playerSelector.getSinglePlayer());
        if (targetThunderPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        if (sender.isConsole()) {
            targetThunderPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else if (sender.isPlayer()) {
            ThunderPlayer senderThunderPlayer = ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
            if (senderThunderPlayer == null) {
                sender.sendMessage(MessageUtil.getParsedComponent(sender, "sender-not-found", "%prefix% &cYou cannot be exempt to use this command!"));
                return;
            }
            targetThunderPlayer.checkManager.getDebugHandler().toggleListener(senderThunderPlayer);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,
                    "run-as-player-or-console",
                    "%prefix% &cThis command can only be used by players or the console!")
            );
        }
    }

    private void handleConsoleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetName = context.getOrDefault("target", null);

        ThunderPlayer thunderPlayer = parseTarget(sender, targetName.getSinglePlayer());
        if (thunderPlayer == null) return;

        boolean isOutput = thunderPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        String playerName = thunderPlayer.user.getProfile().getName(); // Use user profile for name

        sender.sendMessage(MessageUtil.getParsedComponent(sender,
                isOutput ? "debug-console-on" : "debug-console-off",
                isOutput ? "&#D080AC▍ &#CACABEКонсольный вывод для %name% теперь включён."
                        : "&#D080AC▍ &#CACABEКонсольный вывод для %name% теперь выключен.",
                java.util.Map.of("%name%", MessageUtil.miniMessageSafe(playerName))));
    }

    private void handleHitboxDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        // Hitbox debug requires a *player* to be the listener (the one seeing the boxes)
        if (!sender.isPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,
                    "hitboxdebug-player-only",
                    "%prefix% &cОтладка хитбоксов доступна только игрокам.")
            );
            return;
        }

        // Determine the target player whose hitboxes are being debugged
        ThunderPlayer targetThunderPlayer = parseTarget(sender, playerSelector == null ? sender : playerSelector.getSinglePlayer());
        if (targetThunderPlayer == null) return;

        // Get the sender's ThunderPlayer data, as they are the listener
        ThunderPlayer senderThunderPlayer = ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
        if (senderThunderPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "debug-no-data",
                    "&#D080AC▍ &#CACABEНе найдены твои данные игрока для подписки."));
            return;
        }

        // Get the HitboxDebugHandler check instance and toggle the listener
        HitboxDebugHandler hitboxHandler = targetThunderPlayer.checkManager.get(HitboxDebugHandler.class);
        if (hitboxHandler == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "debug-no-hitbox",
                    "&#D080AC▍ &#CACABEПроверка HitboxDebugHandler не найдена для этого игрока."));
            return;
        }

        boolean enabled = hitboxHandler.toggleListener(senderThunderPlayer); // Pass the sender/listener

        // Send feedback message
        sender.sendMessage(MessageUtil.getParsedComponent(sender,
                enabled ? "debug-hitbox-on" : "debug-hitbox-off",
                enabled ? "&#D080AC▍ &#CACABEОтладка хитбоксов для %name% включена."
                        : "&#D080AC▍ &#CACABEОтладка хитбоксов для %name% выключена.",
                java.util.Map.of("%name%", MessageUtil.miniMessageSafe(targetThunderPlayer.getName()))));
    }

    private @Nullable ThunderPlayer parseTarget(@NotNull Sender sender, @Nullable Sender t) {
        if (sender.isConsole() && t == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "console-specify-target", "%prefix% &cYou must specify a target as the console!"));
            return null;
        }
        Sender target = t == null ? sender : t;

        ThunderPlayer thunderPlayer = ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (thunderPlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(sender.getPlatformPlayer().getNative());
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));

            if (user == null) {
                sender.sendMessage(MessageUtil.getParsedComponent(sender, "debug-unknown-user",
                        "&#D080AC▍ &#CACABEНеизвестный пользователь PacketEvents"));
            } else {
                boolean isExempt = ThunderAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(MessageUtil.getParsedComponent(sender, "debug-conn-state",
                            "&#D080AC▍ &#CACABEСостояние соединения: %state%",
                            java.util.Map.of("%state%", MessageUtil.miniMessageSafe(String.valueOf(user.getConnectionState())))));
                }
            }
        }

        return thunderPlayer;
    }
}
