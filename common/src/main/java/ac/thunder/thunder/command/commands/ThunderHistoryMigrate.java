package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.grim.grimac.api.storage.backend.BackendException;
import ac.thunder.thunder.command.BuildableCommand;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackend;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.migrate.LegacyMigrator;
import ac.grim.grimac.internal.storage.migrate.V0Reader;
import ac.thunder.thunder.manager.datastore.ClientVersionResolver;
import ac.thunder.thunder.manager.datastore.DataStoreLifecycle;
import ac.thunder.thunder.manager.datastore.V0Sources;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * {@code /thunder history migrate [--delete]} — on-demand v0 → v1 migration outside
 * the startup path. Detects the legacy source by reading
 * {@code history.database.type / host / port / database / username / password}
 * from {@code config.yml} (the same keys the pre-cutover plugin wrote), builds
 * the matching JDBC URL, and runs the same {@code LegacyMigrator} startup uses.
 * <p>
 * {@code --delete} (off by default) drops the v0 {@code grim_history_*} tables
 * after state flips to {@code COMPLETE}. Operator-requested destructive action;
 * no confirmation prompt — the flag itself is the confirmation.
 * <p>
 * Runs synchronously on the command thread so RCON callers get the full output
 * before the reply channel closes. Migration against a large v0 can take
 * seconds to minutes; tolerable for a one-shot admin command.
 */
public class ThunderHistoryMigrate implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("history")
                        .literal("migrate")
                        .permission("thunder.history.migrate")
                        .flag(commandManager.flagBuilder("delete")
                                .withDescription(org.incendo.cloud.description.Description.of(
                                        "Удалить старые таблицы v0 после миграции")))
                        .handler(this::handle)
        );
    }

    private void handle(CommandContext<Sender> context) {
        Sender sender = context.sender();
        boolean delete = context.flags().hasFlag("delete");

        DataStoreLifecycle lifecycle = ThunderAPI.INSTANCE.getDataStoreLifecycle();
        // isEnabled() is false when database.yml sets enabled=false;
        // isLoaded() is false when start() caught an init failure. Surface
        // each separately so the operator sees why the command won't run.
        if (!lifecycle.isEnabled()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "thunder-history-disabled",
                    "&#D080AC▍ &#CACABEИстория &#D080AC&nвыключена&r&#CACABE!"));
            return;
        }
        if (!lifecycle.isLoaded()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "thunder-history-load-failure",
                    "&#D080AC▍ &#CACABEИстория &#D080AC&nне загрузилась&r&#CACABE! Проверь консоль сервера."));
            return;
        }

        V0Sources.V0Source source = V0Sources.detect(
                ThunderAPI.INSTANCE.getThunderPlugin().getDataFolder().toPath(),
                ThunderAPI.INSTANCE.getConfigManager().getConfig());
        if (source == null) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-no-source",
                    "&#D080AC▍ &#CACABEСтарый источник v0 не найден — мигрировать нечего."));
            return;
        }

        logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-started",
                "&#D080AC▍ &#CACABEМиграция v0 → v1 из %source%",
                java.util.Map.of("%source%", MessageUtil.miniMessageSafe(source.summary()))));

        if (lifecycle.sqliteBackendForCommands() == null) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-no-sqlite",
                    "&#D080AC▍ &#CACABEДля миграции нужен SQLite: переключи категорию в database.yml или используй /thunder history copy"));
            return;
        }

        try {
            LegacyMigrator.Result result =
                    runLegacy(lifecycle, source, sender);
            Component done = MessageUtil.getParsedComponent(sender, "history-migrate-complete",
                    "&#D080AC▍ &#CACABEМиграция завершена: сессий: %sessions%, нарушений: %violations% за %ms%мс",
                    java.util.Map.of("%sessions%", String.valueOf(result.sessionsWritten()),
                            "%violations%", String.valueOf(result.violationsWritten()),
                            "%ms%", String.valueOf(result.elapsedMs())));
            if (result.resumed()) {
                done = done.append(MessageUtil.getParsedComponent(sender, "history-migrate-resumed",
                        " &#CACABE(продолжено)"));
            }
            logBoth(sender, done);
            if (delete) {
                dropLegacy(source, sender);
            }
        } catch (BackendException e) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-failed",
                    "&#D080AC▍ &#CACABEМиграция не удалась: %error%",
                    java.util.Map.of("%error%", MessageUtil.miniMessageSafe(e.getMessage()))));
            LogUtil.error("Миграция легасу не удалась (/thunder history migrate)", e);
        }
    }

    private LegacyMigrator.Result runLegacy(
            DataStoreLifecycle lifecycle, V0Sources.V0Source source, Sender sender) throws BackendException {
        V0Reader reader =
                new V0Reader(
                        source.jdbcUrl(), source.username(), source.password());
        // Legacy migration only targets SQLite today — V0Reader understands
        // the old grim_history_* schema and writes through SqliteBackend's
        // bulk-import path. /thunder history copy is the general-purpose
        // cross-backend hammer once more targets exist.
        SqliteBackend v1 = lifecycle.sqliteBackendForCommands();
        if (v1 == null) {
                throw new BackendException(
                        "Для миграции нужен SQLite: переключи категорию в database.yml или используй /thunder history copy");
        }
        CheckRegistry registry = lifecycle.checkRegistryForCommands();
        long gapMs = lifecycle.config().session().gapMs();
        LegacyMigrator migrator =
                new LegacyMigrator(
                        reader, v1, registry,
                        ClientVersionResolver::legacyStringToPvn,
                        gapMs, Logger.getLogger("thunder-history-migrate"));
        return migrator.run(count -> {
            if (count > 0 && count % 5000 == 0) {
                logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-progress",
                        "&#D080AC▍ &#CACABE… смигрировано нарушений: %count%",
                        java.util.Map.of("%count%", String.valueOf(count))));
            }
        });
    }

    private void dropLegacy(V0Sources.V0Source source, Sender sender) {
        logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-dropping",
                "&#D080AC▍ &#CACABE--delete запрошен — удаление старых таблиц v0…"));
        String[] tables = {
                "grim_history_violations",
                "grim_history_check_names",
                "grim_history_servers",
                "grim_history_versions",
                "grim_history_client_brands",
                "grim_history_client_versions",
                "grim_history_server_versions",
        };
        try (Connection c = open(source); Statement s = c.createStatement()) {
            for (String t : tables) {
                try { s.executeUpdate("DROP TABLE IF EXISTS " + t); }
                catch (SQLException e) {
                    logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-drop-failed",
                            "&#D080AC▍ &#CACABE  удаление %table% не удалось: %error%",
                            java.util.Map.of("%table%", t, "%error%", MessageUtil.miniMessageSafe(e.getMessage()))));
                }
            }
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-dropped",
                    "&#D080AC▍ &#CACABEСтарые таблицы v0 удалены."));
        } catch (SQLException e) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-migrate-open-failed",
                    "&#D080AC▍ &#CACABEНе удалось открыть старый источник для --delete: %error%",
                    java.util.Map.of("%error%", MessageUtil.miniMessageSafe(e.getMessage()))));
        }
    }

    private static Connection open(V0Sources.V0Source source) throws SQLException {
        if (source.username() == null && source.password() == null) {
            return DriverManager.getConnection(source.jdbcUrl());
        }
        return DriverManager.getConnection(source.jdbcUrl(), source.username(), source.password());
    }

    private static void logBoth(Sender sender, Component msg) {
        sender.sendMessage(msg);
        // Console log in plain text so operators still see progress if the
        // command channel (RCON connection) closes mid-run.
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
