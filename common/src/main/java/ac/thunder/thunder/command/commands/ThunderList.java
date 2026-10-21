package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.grim.grimac.api.GrimIdentity;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.command.CommandUtils;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ThunderList implements BuildableCommand {

    // Mainly for debugging purposes. Useful for seeing which players are exempt or not.

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(commandManager.commandBuilder("thunder", "th")
                .literal("list")
                .permission("thunder.list")
                .required("list", StringParser.stringParser(), SUGGESTIONS)
                .handler(commandContext -> handleList(commandContext.sender(), commandContext.getOrDefault("list", "?").toLowerCase()))
                .build());
    }

    private final SuggestionProvider<Sender> SUGGESTIONS = CommandUtils.fromStrings("players");

    private void handleList(Sender sender, String id) {
        switch (id) {
            case "players" -> handleListPlayers(sender);
            default -> sender.sendMessage(MessageUtil.getParsedComponent(sender, "list-invalid",
                    "&#D080AC▍ &#CACABEНеверный аргумент: %id%",
                    java.util.Map.of("%id%", MessageUtil.miniMessageSafe(id))));
        }
    }

    private Component playerComponent(String name, UUID uuid, boolean online, boolean exempt) {
        return Component.text(name)
                .color(exempt ? TextColor.color(0xD080AC) : TextColor.color(0xCACABE))
                .clickEvent(ClickEvent.copyToClipboard(name))
                .hoverEvent(HoverEvent.showText(playerHoverComponent(uuid, online, exempt, true)));
    }

    private Component playerHoverComponent(UUID uuid, boolean online, boolean exempt, boolean registered) {
        var builder = Component.text();
        builder.append(Component.text()
                .append(MessageUtil.miniMessage(cfgText("list-uuid", "UUID: ")).colorIfAbsent(TextColor.color(0xCACABE)))
                .append(Component.text(uuid + "").color(TextColor.color(0xCACABE)))
                .append(Component.newline())
                .append(MessageUtil.miniMessage(cfgText("list-status", "Статус: ")).colorIfAbsent(TextColor.color(0xCACABE)))
                .append(online ? MessageUtil.miniMessage(cfgText("list-online", "В сети")).colorIfAbsent(TextColor.color(0xD080AC))
                        : MessageUtil.miniMessage(cfgText("list-offline", "Не в сети")).colorIfAbsent(TextColor.color(0xCACABE))));
        if (exempt) {
            builder.append(Component.newline());
            builder.append(MessageUtil.miniMessage(cfgText("list-exempt", "Освобождён")).colorIfAbsent(TextColor.color(0xD080AC)));
        }
        if (!registered) {
            builder.append(Component.newline());
            builder.append(MessageUtil.miniMessage(cfgText("list-unregistered", "Не зарегистрирован")).colorIfAbsent(TextColor.color(0xD080AC)));
        }
        return builder.build();
    }

    /** Plain-text snippet from messages config so hover labels are changeable. */
    private static String cfgText(String key, String fallback) {
        try {
            String value = ThunderAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallback);
            return value == null ? fallback : value;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private void handleListPlayers(Sender sender) {
        final TextComponent.Builder builder = Component.text();

        final Map<UUID, PlatformPlayer> onlinePlayers = ThunderAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()
                .stream().collect(Collectors.toMap(GrimIdentity::getUniqueId, Function.identity()));
        //
        final Set<PlatformPlayer> unregisteredPlayers = new HashSet<>(onlinePlayers.values());

        Component separator = MessageUtil.miniMessage(cfgText("list-separator", "&#CACABE, "));
        boolean after = false;
        builder.append(MessageUtil.miniMessage(cfgText("list-header", "&#D080AC▍ &#CACABEИгроки [")));
        //
        for (ThunderPlayer entry : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (after) {
                builder.append(separator);
            } else {
                after = true;
            }
            PlatformPlayer platformPlayer = onlinePlayers.get(entry.getUniqueId());
            if (platformPlayer != null) unregisteredPlayers.remove(platformPlayer);
            boolean online = platformPlayer != null && platformPlayer.isOnline();
            boolean exempt = !ThunderAPI.INSTANCE.getPlayerDataManager().shouldCheck(entry.user);
            builder.append(playerComponent(entry.getName(), entry.getUniqueId(), online, exempt));
        }
        //
        for (PlatformPlayer platformPlayer : unregisteredPlayers) {
            if (after) {
                builder.append(separator);
            } else {
                after = true;
            }
            builder.append(Component.text(platformPlayer.getName()).color(TextColor.color(0xD080AC))
                    .clickEvent(ClickEvent.suggestCommand(platformPlayer.getName()))
                    .hoverEvent(HoverEvent.showText(playerHoverComponent(platformPlayer.getUniqueId(), platformPlayer.isOnline(), false, false)))
            );
        }
        // close and send
        builder.append(MessageUtil.miniMessage(cfgText("list-footer", "&#CACABE]")));
        sender.sendMessage(builder.build());
    }

}
