package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.grim.grimac.api.storage.backend.Backend;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.thunder.thunder.command.BuildableCommand;
import ac.grim.grimac.internal.storage.copy.BackendToBackendCopier;
import ac.thunder.thunder.manager.datastore.DataStoreLifecycle;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.Map;

/**
 * {@code /thunder history copy <src-backend-id> <dst-backend-id> [--delete]} —
 * cross-backend copy of sessions + violations + player identities via
 * {@link BackendToBackendCopier}. The {@code --delete} flag wipes the source
 * after the copy completes.
 * <p>
 * Source/destination are resolved by backend id against whatever backends the
 * active routing has wired up. The two built-in backends are {@code sqlite}
 * and {@code memory}; any third-party backend registered via
 * {@link ac.grim.grimac.api.storage.backend.BackendRegistry} plugs in here
 * without changes to this command.
 * <p>
 * Runs synchronously on the command thread so RCON callers see progress and
 * result before the reply channel closes. Sessions dedup via primary key;
 * violations get fresh autoincrement IDs per insert, so running the command
 * twice without {@code --delete} on the destination will duplicate violation
 * rows — see the copier's docstring for details.
 */
public class ThunderHistoryCopy implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("history")
                        .literal("copy")
                        .permission("thunder.history.copy")
                        .required("src", StringParser.stringParser())
                        .required("dst", StringParser.stringParser())
                        .flag(commandManager.flagBuilder("delete")
                                .withDescription(org.incendo.cloud.description.Description.of(
                                        "Стереть исходный бэкенд после копирования")))
                        .handler(this::handle)
        );
    }

    private void handle(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String srcId = context.get("src");
        String dstId = context.get("dst");
        boolean delete = context.flags().hasFlag("delete");

        DataStoreLifecycle lifecycle = ThunderAPI.INSTANCE.getDataStoreLifecycle();
        // Disabled in database.yml, or start() failed — either way there's
        // no backend pool to copy between.
        if (!lifecycle.isLoaded()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-copy-not-loaded",
                    "&#D080AC▍ &#CACABEИстория не загружена!"));
            return;
        }

        Map<String, Backend> backends = lifecycle.allBackendsForCommands();
        Backend src = backends.get(srcId);
        Backend dst = backends.get(dstId);
        if (src == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-copy-unknown-src",
                    "&#D080AC▍ &#CACABEНеизвестный исходный бэкенд: %src% (настроены: %backends%)",
                    Map.of("%src%", srcId, "%backends%", backends.keySet().toString())));
            return;
        }
        if (dst == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-copy-unknown-dst",
                    "&#D080AC▍ &#CACABEНеизвестный конечный бэкенд: %dst% (настроены: %backends%)",
                    Map.of("%dst%", dstId, "%backends%", backends.keySet().toString())));
            return;
        }
        if (src == dst) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-copy-same-backend",
                    "&#D080AC▍ &#CACABEИсточник и назначение — один бэкенд; копирование отменено."));
            return;
        }

        logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-copying",
                "&#D080AC▍ &#CACABEКопирование истории v1 %src% → %dst%",
                Map.of("%src%", srcId, "%dst%", dstId)));

        try {
            BackendToBackendCopier copier = new BackendToBackendCopier(src, dst);
            BackendToBackendCopier.Result result = copier.run(count -> {
                if (count > 0 && count % 5000 == 0) {
                    logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-progress",
                            "&#D080AC▍ &#CACABE… скопировано нарушений: %count%",
                            Map.of("%count%", String.valueOf(count))));
                }
            });
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-complete",
                    "&#D080AC▍ &#CACABEКопирование завершено: игроков: %players%, сессий: %sessions%, нарушений: %violations% за %ms%мс",
                    Map.of("%players%", String.valueOf(result.players()),
                            "%sessions%", String.valueOf(result.sessions()),
                            "%violations%", String.valueOf(result.violations()),
                            "%ms%", String.valueOf(result.elapsedMs()))));
            if (delete) {
                logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-wiping",
                        "&#D080AC▍ &#CACABE--delete запрошен — очистка источника %src%…",
                        Map.of("%src%", srcId)));
                copier.dropSource();
                logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-wiped",
                        "&#D080AC▍ &#CACABEИсточник %src% очищен.",
                        Map.of("%src%", srcId)));
            }
        } catch (BackendException e) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-copy-failed",
                    "&#D080AC▍ &#CACABEКопирование не удалось: %error%",
                    Map.of("%error%", MessageUtil.miniMessageSafe(e.getMessage()))));
            LogUtil.error("Копия v1 не удалась (/thunder history copy)", e);
        }
    }

    private static void logBoth(Sender sender, Component msg) {
        sender.sendMessage(msg);
        LogUtil.info(plain(msg));
    }

    private static String plain(Component c) {
        StringBuilder sb = new StringBuilder();
        flatten(c, sb);
        return sb.toString();
    }

    private static void flatten(Component c, StringBuilder sb) {
        if (c instanceof net.kyori.adventure.text.TextComponent tc) sb.append(tc.content());
        for (Component child : c.children()) flatten(child, sb);
    }
}
