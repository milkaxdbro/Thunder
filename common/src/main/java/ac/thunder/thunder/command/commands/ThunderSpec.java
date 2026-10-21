package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.command.CloudCommandService;
import ac.thunder.thunder.command.requirements.PlayerSenderRequirement;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Заявки на слежку: игрок пишет /spec — все онлайн-модераторы с правом
 * thunder.spec получают сообщение с кнопками «Пропустить» (красная)
 * и «Телепортироваться» (жёлтая). Принятие переводит модератора в GM 3
 * (спектатор) и телепортирует его к заявителю.
 */
public class ThunderSpec implements BuildableCommand {
    /** Право, дающее получение заявок и кнопки принять/пропустить. */
    public static final String SPEC_PERMISSION = "thunder.spec";

    private static final long REQUEST_TTL_MS = 120_000;
    private static final long REQUEST_COOLDOWN_MS = 60_000;

    /** UUID заявителя -> время заявки. */
    private final Map<UUID, Long> pendingRequests = new ConcurrentHashMap<>();
    /** UUID заявителя -> имя на момент заявки (для кнопок). */
    private final Map<UUID, String> pendingNames = new ConcurrentHashMap<>();
    /** UUID заявителя -> время последней заявки (кулдаун). */
    private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        // /spec — оставить заявку (только игроки, без права)
        commandManager.command(
                commandManager.commandBuilder("spec")
                        .handler(this::handleRequest)
                        .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.INSTANCE))
        );
        // /spec accept <игрок> — слежка (модераторы)
        commandManager.command(
                commandManager.commandBuilder("spec")
                        .literal("accept")
                        .permission(SPEC_PERMISSION)
                        .required("player", StringParser.stringParser())
                        .handler(this::handleAccept)
        );
        // /spec dismiss <игрок> — пропустить (модераторы)
        commandManager.command(
                commandManager.commandBuilder("spec")
                        .literal("dismiss")
                        .permission(SPEC_PERMISSION)
                        .required("player", StringParser.stringParser())
                        .handler(this::handleDismiss)
        );
        // /spec stop — выйти из слежки (модераторы)
        commandManager.command(
                commandManager.commandBuilder("spec")
                        .literal("stop")
                        .permission(SPEC_PERMISSION)
                        .handler(this::handleStop)
        );
    }

    private void handleRequest(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlatformPlayer player = Objects.requireNonNull(sender.getPlatformPlayer(), "platformPlayer");
        UUID uuid = sender.getUniqueId();
        long now = System.currentTimeMillis();

        purgeExpired(now);

        Long last = lastRequest.get(uuid);
        if (last != null && now - last < REQUEST_COOLDOWN_MS) {
            long wait = (REQUEST_COOLDOWN_MS - (now - last) + 999) / 1000;
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-cooldown",
                    "&#D080AC[ℹ] &#CACABEПодожди %time% сек. перед следующей заявкой.",
                    Map.of("%time%", Long.toString(wait))));
            return;
        }

        if (pendingRequests.containsKey(uuid)) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-already",
                    "&#D080AC[ℹ] &#CACABEУ тебя уже есть активная заявка!"));
            return;
        }

        pendingRequests.put(uuid, now);
        pendingNames.put(uuid, player.getName());
        lastRequest.put(uuid, now);

        sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-request-sent",
                "&#D080AC[ℹ] &#CACABEЗаявка отправлена! Модераторы уведомлены, ожидай."));

        broadcastRequest(player);
    }

    private void handleAccept(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String name = context.get("player");
        long now = System.currentTimeMillis();
        purgeExpired(now);

        UUID targetUuid = findPending(name);
        if (targetUuid == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-no-request",
                    "&#D080AC[ℹ] &#CACABEАктивных заявок нет!"));
            return;
        }

        if (targetUuid.equals(sender.getUniqueId())) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-run-on-self",
                    "&#D080AC▍ &#CACABEНельзя &#D080AC&nиспользовать&r&#CACABE эту команду на себе!"));
            return;
        }

        PlatformPlayer target = findOnlinePlayer(targetUuid);
        if (target == null) {
            pendingRequests.remove(targetUuid);
            pendingNames.remove(targetUuid);
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-offline",
                    "&#D080AC[ℹ] &#CACABEИгрок уже вышел!"));
            return;
        }

        if (target.isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-this-server",
                    "&#D080AC▍ &#CACABEЭтого игрока нет на этом сервере!"));
            return;
        }

        pendingRequests.remove(targetUuid);
        pendingNames.remove(targetUuid);

        PlatformPlayer moderator = sender.getPlatformPlayer();
        if (moderator == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "run-as-player",
                    "&#D080AC▍ &#CACABEЭта команда только для игроков!"));
            return;
        }

        if (ThunderAPI.INSTANCE.getSpectateManager().enable(moderator)) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spectate-return",
                    "&#D080AC▍ &#CACABEНажми, чтобы вернуться назад. Используй /thunder stopspectating"));
        }

        moderator.setGameMode(GameMode.SPECTATOR);
        moderator.teleportAsync(target.getLocation());

        sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-accepted",
                "&#D080AC[ℹ] &#CACABEТы следишь за игроком. Возврат: &#D080AC/spec stop"));

        target.sendMessage(MessageUtil.getParsedComponent(target.getSender(), "spec-notify-accepted",
                "&#D080AC[ℹ] &#CACABEМодератор &#D080AC&n%name%&r&#CACABE летит к теве!",
                Map.of("%name%", MessageUtil.toSmallCaps(MessageUtil.miniMessageSafe(sender.getName())))));
    }

    private void handleDismiss(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String name = context.get("player");
        purgeExpired(System.currentTimeMillis());

        UUID targetUuid = findPending(name);
        if (targetUuid == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-no-request",
                    "&#D080AC[ℹ] &#CACABEАктивных заявок нет!"));
            return;
        }

        pendingRequests.remove(targetUuid);
        pendingNames.remove(targetUuid);

        sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-dismissed",
                "&#D080AC[ℹ] &#CACABEЗаявка отклонена."));

        PlatformPlayer target = findOnlinePlayer(targetUuid);
        if (target != null) {
            target.sendMessage(MessageUtil.getParsedComponent(target.getSender(), "spec-notify-dismissed",
                    "&#D080AC[ℹ] &#CACABEТвоя заявка &#D080AC&nотклонена&r&#CACABE."));
        }
    }

    private void handleStop(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (ThunderAPI.INSTANCE.getSpectateManager().isSpectating(sender.getUniqueId())) {
            PlatformPlayer player = sender.getPlatformPlayer();
            if (player == null) {
                sender.sendMessage(MessageUtil.getParsedComponent(sender, "run-as-player",
                        "&#D080AC▍ &#CACABEЭта команда только для игроков!"));
                return;
            }
            ThunderAPI.INSTANCE.getSpectateManager().disable(player, true);
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spec-stopped",
                    "&#D080AC[ℹ] &#CACABEТы вернулся на прежнее место."));
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-spectate-return",
                    "&#D080AC▍ &#CACABEТы ни за кем не следишь!"));
        }
    }

    private void broadcastRequest(PlatformPlayer requester) {
        String name = requester.getName();
        String safeName = MessageUtil.miniMessageSafe(name);

        String template = ThunderAPI.INSTANCE.getConfigManager().getConfig().getStringElse("spec-broadcast",
                "&#D080AC[ℹ] &#CACABEИгрок &#D080AC&n%name_display%&r&#CACABE просит проследить за ним! <click:run_command:/spec dismiss %name%><hover:show_text:\"Отклонить заявку\">&#D080AC&n[Пропустить]</underlined></hover></click> <click:run_command:/spec accept %name%><hover:show_text:\"GM 3 + телепорт к игроку\">&#D080AC&n[Телепортироваться]</underlined></hover></click>");
        template = template.replace("%name_display%", safeName).replace("%name%", safeName);
        Component message;
        try {
            message = MessageUtil.miniMessage(MessageUtil.replacePlaceholders(requester, template));
        } catch (RuntimeException e) {
            // Broken MiniMessage tags in a customised template must not silence the request:
            // fall back to plain text without buttons.
            message = MessageUtil.miniMessage(MessageUtil.replacePlaceholders(requester,
                    "&#D080AC[ℹ] &#CACABEИгрок &#D080AC&n%name_display%&r&#CACABE просит проследить за ним!".replace("%name_display%", safeName)));
        }

        for (PlatformPlayer online : ThunderAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
            if (online.getUniqueId().equals(requester.getUniqueId())) continue;
            if (!online.hasPermission(SPEC_PERMISSION)) continue;
            online.sendMessage(message);
        }
    }

    private void purgeExpired(long now) {
        pendingRequests.entrySet().removeIf(entry -> now - entry.getValue() > REQUEST_TTL_MS);
        pendingNames.keySet().removeIf(uuid -> !pendingRequests.containsKey(uuid));
    }

    private @Nullable UUID findPending(String name) {
        for (Map.Entry<UUID, String> entry : pendingNames.entrySet()) {
            if (!pendingRequests.containsKey(entry.getKey())) continue;
            if (entry.getValue().equalsIgnoreCase(name)) return entry.getKey();
        }
        return null;
    }

    private @Nullable PlatformPlayer findOnlinePlayer(UUID uuid) {
        for (PlatformPlayer online : ThunderAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
            if (online.getUniqueId().equals(uuid)) return online;
        }
        return null;
    }
}
