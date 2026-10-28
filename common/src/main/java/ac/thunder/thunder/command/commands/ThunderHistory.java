package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.storage.backend.Backend;
import ac.grim.grimac.api.storage.category.Categories;
import ac.grim.grimac.api.storage.check.CheckCatalogRepairResult;
import ac.grim.grimac.api.storage.check.CheckCatalogRow;
import ac.grim.grimac.api.storage.history.HistoryService;
import ac.grim.grimac.api.storage.history.SessionDetail;
import ac.grim.grimac.api.storage.history.SessionSummary;
import ac.grim.grimac.api.storage.history.ViolationEntry;
import ac.grim.grimac.api.storage.identity.NameResolver;
import ac.grim.grimac.api.storage.model.PlayerIdentity;
import ac.grim.grimac.api.storage.query.Cursor;
import ac.grim.grimac.api.storage.query.Page;
import ac.grim.grimac.api.storage.query.Queries;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.command.render.HistoryComponentRenderer;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.thunder.thunder.manager.datastore.DataStoreLifecycle;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <pre>
 *   /thunder history &lt;target&gt;                                 → list, page 1
 *   /thunder history &lt;target&gt; page &lt;P&gt;                          → list, page P
 *   /thunder history &lt;target&gt; session                          → help menu
 *   /thunder history &lt;target&gt; session &lt;N|latest&gt; [-d] [-v]     → detail
 *   /thunder history &lt;target&gt; session &lt;N|latest&gt; page &lt;P&gt; [-d] [-v]
 *   /thunder history player &lt;target&gt; ...                       → disambiguated form
 *   /thunder history repair check-ids                           → in-place repair
 * </pre>
 * {@code latest} / {@code last} / {@code l} alias the most-recent session.
 * Flags: {@code -d} raw rows, {@code -v} inline verbose, {@code --name} /
 * {@code --match} / {@code --grep} regex filters (AND-composed).
 */
public class ThunderHistory implements BuildableCommand {

    private static final AtomicBoolean REPAIR_RUNNING = new AtomicBoolean();

    private static final int MAX_SUGGESTIONS = 30;
    private static final int MAX_PLAYER_SUGGESTIONS = 25;
    private static final String LATEST_ALIAS = "latest";

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        SuggestionProvider<Sender> listPageNumberSuggestions = listPageSuggestions();
        SuggestionProvider<Sender> sessionOrdinalSuggestions = sessionSuggestions();
        SuggestionProvider<Sender> violationPageSuggestions = violationPageSuggestions();
        SuggestionProvider<Sender> targetSuggestions = targetSuggestions(arguments);

        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("history", "hist")
                        .literal("repair")
                        .literal("check-ids")
                        .permission("thunder.history.repair")
                        .handler(this::handleRepairCheckIds)
        );

        // Bare + 'player'-prefixed forms; the prefix is the escape hatch
        // for players whose name collides with a sibling literal at the
        // same tree depth (today: 'repair', 'player').
        registerHistoryViewBranches(commandManager, false,
                targetSuggestions, listPageNumberSuggestions,
                sessionOrdinalSuggestions, violationPageSuggestions);
        registerHistoryViewBranches(commandManager, true,
                targetSuggestions, listPageNumberSuggestions,
                sessionOrdinalSuggestions, violationPageSuggestions);
    }

    /** Registers the bare or {@code player}-prefixed view branches. */
    private void registerHistoryViewBranches(
            CommandManager<Sender> commandManager,
            boolean withPlayerLiteral,
            SuggestionProvider<Sender> targetSuggestions,
            SuggestionProvider<Sender> listPageNumberSuggestions,
            SuggestionProvider<Sender> sessionOrdinalSuggestions,
            SuggestionProvider<Sender> violationPageSuggestions) {
        // Fresh builder per branch — reusing one cross-pollinates siblings.
        java.util.function.Supplier<Command.Builder<Sender>> base = () -> {
            Command.Builder<Sender> b = commandManager.commandBuilder("thunder", "th")
                    .literal("history", "hist")
                    .permission("thunder.history");
            if (withPlayerLiteral) b = b.literal("player");
            return b.required("target", StringParser.stringParser(), targetSuggestions);
        };

        // handleSessionHelp echoes whichever prefix was used.
        final boolean viaPlayer = withPlayerLiteral;

        // List, page 1
        commandManager.command(
                applyFilterFlags(commandManager, base.get())
                        .handler(this::handleListPage1)
        );
        // List, page N
        commandManager.command(
                applyFilterFlags(commandManager, base.get()
                        .literal("page")
                        .required("page_number", IntegerParser.integerParser(1), listPageNumberSuggestions))
                        .handler(this::handleListPageN)
        );
        // Help branch on bare 'session' — Cloud would otherwise reject with
        // a parse error that reads like a syntax mistake, not a hint.
        commandManager.command(
                base.get().literal("session")
                        .handler(ctx -> handleSessionHelp(ctx, viaPlayer))
        );
        // 'session' literal at the same tree slot as 'page'; session-ordinal is
        // String so it accepts 'latest' / 'last' / 'l' alongside integers.
        commandManager.command(
                applyFilterFlags(commandManager, base.get()
                        .literal("session")
                        .required("session", StringParser.stringParser(), sessionOrdinalSuggestions)
                        .flag(commandManager.flagBuilder("detailed").withAliases("d")
                                .withDescription(Description.of("Каждое нарушение отдельной строкой, без группировки по времени.")))
                        .flag(commandManager.flagBuilder("verbose").withAliases("v")
                                .withDescription(Description.of("Подробный текст в строке (есть и в ховере)."))))
                        .handler(this::handleDetailDefaultPage)
        );
        // Detail, violation page N
        commandManager.command(
                applyFilterFlags(commandManager, base.get()
                        .literal("session")
                        .required("session", StringParser.stringParser(), sessionOrdinalSuggestions)
                        .literal("page")
                        .required("page_number", IntegerParser.integerParser(1), violationPageSuggestions)
                        .flag(commandManager.flagBuilder("detailed").withAliases("d")
                                .withDescription(Description.of("Каждое нарушение отдельной строкой, без группировки по времени.")))
                        .flag(commandManager.flagBuilder("verbose").withAliases("v")
                                .withDescription(Description.of("Подробный текст в строке (есть и в ховере)."))))
                        .handler(this::handleDetailPageN)
        );
    }

    /** Attach the {@code --name} / {@code --match} / {@code --grep} regex flags. */
    private static Command.Builder<Sender> applyFilterFlags(
            CommandManager<Sender> commandManager,
            Command.Builder<Sender> b) {
        return b
                .flag(commandManager.flagBuilder("name")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Фильтр нарушений по названию проверки.")))
                .flag(commandManager.flagBuilder("match")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Фильтр нарушений по подробному тексту.")))
                .flag(commandManager.flagBuilder("grep")
                        .withComponent(StringParser.stringParser())
                        .withDescription(Description.of("Фильтр по названию ИЛИ тексту.")));
    }

    private void handleListPage1(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) ->
                renderList(sender, lifecycle, history, uuid, displayName, 1, filter));
    }

    private void handleListPageN(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        int page = context.<Integer>get("page_number");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) ->
                renderList(sender, lifecycle, history, uuid, displayName, Math.max(1, page), filter));
    }

    private void handleDetailDefaultPage(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        String sessionRaw = context.get("session");
        boolean detailed = context.flags().hasFlag("detailed");
        boolean verbose = context.flags().hasFlag("verbose");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) -> {
            Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, history);
            if (ordinal == null) {
                return List.of(message("thunder-history-session-not-found",
                        "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                        Map.of("player", displayName, "ordinal", sessionRaw)));
            }
            return renderDetail(sender, lifecycle, history, uuid, displayName,
                    ordinal, detailed, verbose, /*pageArg*/ null, filter);
        });
    }

    private void handleDetailPageN(CommandContext<Sender> context) {
        Sender sender = context.sender();
        String target = context.get("target");
        String sessionRaw = context.get("session");
        int page = context.<Integer>get("page_number");
        boolean detailed = context.flags().hasFlag("detailed");
        boolean verbose = context.flags().hasFlag("verbose");
        Predicate<ViolationEntry> filter = parseFilterFromContext(sender, context);
        if (filter == FILTER_ERROR) return;
        runWithPrelude(sender, target, (uuid, displayName, lifecycle, history) -> {
            Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, history);
            if (ordinal == null) {
                return List.of(message("thunder-history-session-not-found",
                        "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                        Map.of("player", displayName, "ordinal", sessionRaw)));
            }
            return renderDetail(sender, lifecycle, history, uuid, displayName,
                    ordinal, detailed, verbose, Math.max(1, page), filter);
        });
    }

    /** Prints usage for {@code /thunder history <target> session} without an ordinal. */
    private void handleSessionHelp(CommandContext<Sender> ctx, boolean viaPlayer) {
        Sender sender = ctx.sender();
        String target = ctx.get("target");
        // Echo the exact prefix the operator used so the printed examples
        // dispatch on the same branch — a target that needed the 'player'
        // escape hatch (e.g. someone named 'repair') would route through
        // the wrong literal if the help printed the bare form.
        String addressPrefix = "/thunder history " + (viaPlayer ? "player " : "");
        java.util.Map<String, String> extra = java.util.Map.of(
                "%address%", addressPrefix,
                "%target%", MessageUtil.toSmallCaps(MessageUtil.miniMessageSafe(target)));
        MessageUtil.sendConfigLines(sender, "history-session-help", java.util.List.of(
                "&#D080AC&n▍&r &#D080AC%address%&#D080AC&n%target%&r&#CACABE session <N | latest> — показать нарушения конкретной сессии.",
                "&#D080AC&n▍&r &#CACABE  N — номер сессии (1 — самая старая). 'latest' / 'last' / 'l' — последняя.",
                "&#D080AC&n▍&r &#CACABEДополнительно:",
                "&#D080AC&n▍&r &#D080AC  page <P>&#CACABE                  листать нарушения сессии",
                "&#D080AC&n▍&r &#D080AC  --detailed / -d&#CACABE           сырые строки нарушений (без группировки по времени)",
                "&#D080AC&n▍&r &#D080AC  --verbose / -v&#CACABE            подробный текст в строке (и в ховере)",
                "&#D080AC&n▍&r &#D080AC  --name <regex>&#CACABE            фильтр по названию проверки",
                "&#D080AC&n▍&r &#D080AC  --match <regex>&#CACABE           фильтр по подробному тексту",
                "&#D080AC&n▍&r &#D080AC  --grep <regex>&#CACABE            фильтр по названию или тексту (И-композиция)",
                "&#D080AC&n▍&r &#CACABEПримеры:",
                "&#D080AC&n▍&r &#CACABE  %address%&#D080AC&n%target%&r&#CACABE session latest -d -v",
                "&#D080AC&n▍&r &#CACABE  %address%&#D080AC&n%target%&r&#CACABE session 1 --grep reach"), extra);
    }

    /** Sentinel: invalid regex, sender already messaged. Distinguished from null ("no filter") with {@code ==}. */
    private static final Predicate<ViolationEntry> FILTER_ERROR = v -> false;

    /**
     * Build the violation predicate from {@code --name} (display name),
     * {@code --match} (verbose), {@code --grep} (either). Case-insensitive,
     * AND-composed. Null = no filter; {@link #FILTER_ERROR} = bad regex,
     * sender already messaged.
     */
    private static @Nullable Predicate<ViolationEntry> parseFilterFromContext(Sender sender, CommandContext<Sender> ctx) {
        Pattern namePat;
        Pattern verbosePat;
        Pattern grepPat;
        try {
            namePat = compileFlag(ctx, "name");
            verbosePat = compileFlag(ctx, "match");
            grepPat = compileFlag(ctx, "grep");
        } catch (BadRegexException e) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-invalid-regex",
                    "&#D080AC▍ Неверный regex для --%flag%: %detail%",
                    java.util.Map.of("%flag%", e.flag, "%detail%", MessageUtil.miniMessageSafe(e.detail))));
            return FILTER_ERROR;
        }
        if (namePat == null && verbosePat == null && grepPat == null) return null;
        final Pattern n = namePat;
        final Pattern m = verbosePat;
        final Pattern g = grepPat;
        return v -> {
            if (n != null && !n.matcher(v.displayName()).find()) return false;
            if (m != null) {
                String vb = v.verbose();
                if (vb == null || !m.matcher(vb).find()) return false;
            }
            if (g != null) {
                if (!g.matcher(v.displayName()).find()) {
                    String vb = v.verbose();
                    if (vb == null || !g.matcher(vb).find()) return false;
                }
            }
            return true;
        };
    }

    private static @Nullable Pattern compileFlag(CommandContext<Sender> ctx, String flag) {
        String raw = ctx.flags().get(flag);
        if (raw == null) return null;
        try {
            return Pattern.compile(raw, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new BadRegexException(flag, e.getDescription());
        }
    }

    private static final class BadRegexException extends RuntimeException {
        final String flag;
        final String detail;
        BadRegexException(String flag, String detail) {
            super(detail);
            this.flag = flag;
            this.detail = detail;
        }
    }

    /** Resolves a positive integer, {@code latest}/{@code last}/{@code l}, or null. */
    private static @Nullable Integer resolveSessionOrdinal(String raw, UUID uuid, HistoryService history)
            throws Exception {
        if (raw == null) return null;
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.equals(LATEST_ALIAS) || trimmed.equals("last") || trimmed.equals("l")) {
            long total = history.countSessions(uuid).toCompletableFuture().get(5, TimeUnit.SECONDS);
            return total >= 1 ? (int) Math.min(Integer.MAX_VALUE, total) : null;
        }
        try {
            int n = Integer.parseInt(trimmed);
            return n >= 1 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FunctionalInterface
    private interface HistoryAction {
        List<Component> run(UUID uuid, String displayName, DataStoreLifecycle lifecycle, HistoryService history) throws Exception;
    }

    private void runWithPrelude(Sender sender, String target, HistoryAction action) {
        DataStoreLifecycle lifecycle = ThunderAPI.INSTANCE.getDataStoreLifecycle();
        // enabled=false when database.yml turns the feature off; loaded=false
        // when start() caught an init failure. Distinguish in the message so
        // the operator knows whether to flip config or check the log.
        if (lifecycle == null || !lifecycle.isEnabled()) {
            sender.sendMessage(message("thunder-history-disabled",
                    "%prefix% &cHistory subsystem is disabled!", Map.of()));
            return;
        }
        if (!lifecycle.isLoaded()) {
            sender.sendMessage(message("thunder-history-load-failure",
                    "%prefix% &cHistory subsystem failed to load! Check server console for errors.", Map.of()));
            return;
        }

        UUID onlineUuid = onlineUuid(target);
        ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(
                ThunderAPI.INSTANCE.getThunderPlugin(),
                () -> runWithPreludeAsync(sender, target, onlineUuid, lifecycle, action));
    }

    private void runWithPreludeAsync(Sender sender, String target, @Nullable UUID onlineUuid,
                                     DataStoreLifecycle lifecycle, HistoryAction action) {
        try {
            HistoryService history = lifecycle.historyService();
            if (history == null) {
                sendHistoryMessage(sender, message("thunder-history-disabled",
                        "%prefix% &cHistory subsystem is disabled!", Map.of()));
                return;
            }
            UUID targetUuid = resolveUuid(target, lifecycle, onlineUuid);
            if (targetUuid == null) {
                sendHistoryMessage(sender, message("thunder-history-unknown-player",
                        "%prefix% &cUnknown player: &f%player%", Map.of("player", target)));
                return;
            }
            sendHistoryMessages(sender, action.run(targetUuid, target, lifecycle, history));
        } catch (Exception e) {
            // Some exception types carry a null message; fall back to the
            // class name so operators still see *something* useful.
            sendHistoryMessage(sender, message("thunder-history-failed",
                    "%prefix% &cFailed to load history: &7%error%",
                    Map.of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
        }
    }

    private List<Component> renderList(Sender sender, DataStoreLifecycle lifecycle, HistoryService history,
                                       UUID uuid, String displayName, int page,
                                       @Nullable Predicate<ViolationEntry> filter) throws Exception {
        int entriesPerPage = lifecycle.config().history().entriesPerPage();
        long totalSessions = history.countSessions(uuid).toCompletableFuture().get(5, TimeUnit.SECONDS);
        int maxPages = Math.max(1, (int) ((totalSessions + entriesPerPage - 1) / Math.max(1, entriesPerPage)));
        if (page > maxPages) page = maxPages;

        Cursor cursor = advanceToPage(history, uuid, entriesPerPage, page);
        Page<SessionSummary> result = history
                .listSessions(uuid, cursor, entriesPerPage)
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        // Filter active: keep only sessions with at least one matching
        // violation. Costs N+1 detail fetches per visible page — acceptable
        // at the page-size limit. Unfiltered path keeps the original single
        // listSessions query.
        if (filter != null) result = filterSessionsByDetail(history, uuid, result, filter);

        UUID ongoingSessionId = ongoingSessionIdFor(lifecycle, uuid);
        return HistoryComponentRenderer.renderSessionList(
                sender, uuid, displayName, page, maxPages, result, ongoingSessionId);
    }

    private static Page<SessionSummary> filterSessionsByDetail(HistoryService history, UUID uuid,
                                                               Page<SessionSummary> result,
                                                               Predicate<ViolationEntry> filter) {
        List<SessionSummary> kept = new ArrayList<>();
        for (SessionSummary s : result.items()) {
            try {
                SessionDetail d = history.getSessionDetail(uuid, s.sessionId())
                        .toCompletableFuture().get(2, TimeUnit.SECONDS);
                if (d == null) continue;
                if (d.violations().stream().anyMatch(filter)) kept.add(s);
            } catch (Exception ignored) {
                // Skip sessions whose detail fetch fails — under-show beats
                // blocking the whole page on one slow row.
            }
        }
        return new Page<>(kept, result.nextCursor());
    }

    private List<Component> renderDetail(Sender sender, DataStoreLifecycle lifecycle, HistoryService history,
                                         UUID uuid, String displayName, int sessionOrdinal,
                                         boolean detailed, boolean verbose, @Nullable Integer violationPage,
                                         @Nullable Predicate<ViolationEntry> filter) throws Exception {
        SessionDetail detail = null;
        if (history instanceof ac.grim.grimac.internal.storage.history.HistoryServiceImpl impl) {
            detail = impl.getSessionDetailByOrdinal(uuid, sessionOrdinal)
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        if (detail == null) {
            return List.of(message("thunder-history-session-not-found",
                    "%prefix% &cSession &f%ordinal%&c not found for &f%player%&c.",
                    Map.of(
                            "player", displayName,
                            "ordinal", Integer.toString(sessionOrdinal))));
        }
        if (filter != null) detail = HistoryComponentRenderer.applyFilter(detail, filter);
        int pageSize = lifecycle.config().history().entriesPerPage();
        UUID ongoingSessionId = ongoingSessionIdFor(lifecycle, uuid);
        boolean isOngoing = ongoingSessionId != null && ongoingSessionId.equals(detail.sessionId());
        return HistoryComponentRenderer.renderSessionDetail(
                sender, displayName, detail, detailed, verbose,
                violationPage, pageSize, isOngoing);
    }

    private void handleRepairCheckIds(CommandContext<Sender> context) {
        Sender sender = context.sender();
        DataStoreLifecycle lifecycle = ThunderAPI.INSTANCE.getDataStoreLifecycle();
        if (lifecycle == null || !lifecycle.isLoaded() || lifecycle.config() == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-repair-no-storage",
                    "&#D080AC▍ Хранилище истории выключено (см. database.yml)."));
            return;
        }

        String backendId = lifecycle.config().routing().get(Categories.VIOLATION);
        if (backendId == null || backendId.equalsIgnoreCase("none")) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-repair-no-route",
                    "&#D080AC▍ Бэкенд нарушений не подключён; чинить нечего."));
            return;
        }

        Backend backend = lifecycle.allBackendsForCommands().get(backendId);
        if (backend == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-repair-backend-inactive",
                    "&#D080AC▍ Бэкенд нарушений '%backend%' неактивен.",
                    java.util.Map.of("%backend%", backendId)));
            return;
        }

        if (!REPAIR_RUNNING.compareAndSet(false, true)) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "history-repair-running",
                    "&#D080AC▍ Починка check-id истории уже запущена."));
            return;
        }

        List<CheckDefinition> liveChecks = snapshotLiveCheckDefinitions();
        logBoth(sender, MessageUtil.getParsedComponent(sender, "history-repair-started",
                "&#D080AC▍ Починка check-id истории на бэкенде '%backend%' запущена в фоне...",
                java.util.Map.of("%backend%", backendId)));
        ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(
                ThunderAPI.INSTANCE.getThunderPlugin(),
                () -> runRepairAsync(sender, lifecycle, backend, liveChecks));
    }

    private static void runRepairAsync(
            Sender sender,
            DataStoreLifecycle lifecycle,
            Backend backend,
            List<CheckDefinition> liveChecks) {
        try {
            int prewarmed = prewarmCatalog(lifecycle, liveChecks);
            RepairPlan plan = buildRepairPlan(backend);
            CheckCatalogRepairResult result = backend.repairCheckCatalog(
                    plan.legacyToCatalogIds(), currentThunderVersion());
            runOnGlobalThread(() -> reportRepairComplete(sender, prewarmed, plan, result));
        } catch (Exception e) {
            runOnGlobalThread(() -> logBoth(sender, MessageUtil.getParsedComponent(sender, "history-repair-failed",
                    "&#D080AC▍ Починка не удалась: %error%",
                    java.util.Map.of("%error%", MessageUtil.miniMessageSafe(e.getMessage())))));
            LogUtil.error(MessageUtil.getLogText("log-repair-failed",
                    "Починка check-id не удалась (/thunder history repair check-ids)"), e);
        } finally {
            REPAIR_RUNNING.set(false);
        }
    }

    private static void reportRepairComplete(
            Sender sender,
            int prewarmed,
            RepairPlan plan,
            CheckCatalogRepairResult result) {
        logBoth(sender, MessageUtil.getParsedComponent(sender, "history-repair-complete",
                "&#D080AC▍ Починка завершена: прогрето определений: %prewarmed%, применено маппингов: %mapped%, переписано строк: %updated%, исправлено версий: %fixed%.",
                java.util.Map.of(
                        "%prewarmed%", Integer.toString(prewarmed),
                        "%mapped%", Integer.toString(result.mappingsApplied()),
                        "%updated%", Long.toString(result.violationsUpdated()),
                        "%fixed%", Long.toString(result.catalogVersionsUpdated()))));
        if (plan.ambiguousHashes() > 0 || plan.catalogIdCollisions() > 0) {
            logBoth(sender, MessageUtil.getParsedComponent(sender, "history-repair-skipped",
                    "&#D080AC▍ Пропущено неоднозначных хешей: %ambiguous%, коллизий id: %collisions%.",
                    java.util.Map.of(
                            "%ambiguous%", Integer.toString(plan.ambiguousHashes()),
                            "%collisions%", Integer.toString(plan.catalogIdCollisions()))));
        }
    }

    private static List<CheckDefinition> snapshotLiveCheckDefinitions() {
        List<CheckDefinition> definitions = new ArrayList<>();
        Set<String> seenStableKeys = new HashSet<>();
        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            for (AbstractCheck check : player.getChecks()) {
                String stableKey = check.getStableKey();
                if (stableKey == null || stableKey.isBlank()) continue;
                if (!seenStableKeys.add(stableKey)) continue;
                definitions.add(new CheckDefinition(stableKey, check.getCheckName(), check.getDescription()));
            }
        }
        return definitions;
    }

    private static int prewarmCatalog(DataStoreLifecycle lifecycle, List<CheckDefinition> definitions) {
        CheckRegistry registry = lifecycle.checkRegistryForCommands();
        if (registry == null) return 0;

        int prewarmed = 0;
        String version = currentThunderVersion();
        for (CheckDefinition definition : definitions) {
            registry.intern(definition.stableKey(), definition.display(), definition.description(), version);
            prewarmed++;
        }
        return prewarmed;
    }

    private static RepairPlan buildRepairPlan(Backend backend) {
        Set<Integer> catalogIds = new HashSet<>();
        Map<Integer, List<CheckCatalogRow>> byLegacyHash = new LinkedHashMap<>();
        for (CheckCatalogRow row : backend.checkCatalog().loadAll()) {
            catalogIds.add(row.checkId());
            byLegacyHash.computeIfAbsent(row.stableKey().hashCode(), k -> new ArrayList<>()).add(row);
        }

        Map<Integer, Integer> repairIds = new LinkedHashMap<>();
        int ambiguousHashes = 0;
        int catalogIdCollisions = 0;
        for (Map.Entry<Integer, List<CheckCatalogRow>> entry : byLegacyHash.entrySet()) {
            int legacyId = entry.getKey();
            List<CheckCatalogRow> owners = entry.getValue();
            if (owners.size() != 1) {
                ambiguousHashes += owners.size();
                continue;
            }
            CheckCatalogRow row = owners.get(0);
            if (legacyId == row.checkId()) continue;
            if (catalogIds.contains(legacyId)) {
                catalogIdCollisions++;
                continue;
            }
            repairIds.put(legacyId, row.checkId());
        }
        return new RepairPlan(repairIds, ambiguousHashes, catalogIdCollisions);
    }

    private static String currentThunderVersion() {
        return ThunderAPI.INSTANCE.getExternalAPI().getGrimVersion();
    }

    private record RepairPlan(
            Map<Integer, Integer> legacyToCatalogIds,
            int ambiguousHashes,
            int catalogIdCollisions) {}

    private record CheckDefinition(String stableKey, String display, String description) {}

    private static void sendHistoryMessage(Sender sender, Component message) {
        sendHistoryMessages(sender, List.of(message));
    }

    private static void sendHistoryMessages(Sender sender, List<Component> messages) {
        if (messages.isEmpty()) return;
        runOnGlobalThread(() -> {
            for (Component message : messages) {
                sender.sendMessage(message);
            }
        });
    }

    private static void runOnGlobalThread(Runnable task) {
        ThunderAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(
                ThunderAPI.INSTANCE.getThunderPlugin(), task);
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
        if (c instanceof TextComponent tc) sb.append(tc.content());
        for (Component child : c.children()) flatten(child, sb);
    }

    private Cursor advanceToPage(HistoryService history, UUID uuid, int pageSize, int page) throws Exception {
        if (page <= 1) return null;
        Cursor cursor = null;
        for (int i = 1; i < page; i++) {
            Page<SessionSummary> r = history.listSessions(uuid, cursor, pageSize)
                    .toCompletableFuture().get(5, TimeUnit.SECONDS);
            cursor = r.nextCursor();
            if (cursor == null) break;
        }
        return cursor;
    }

    private static @Nullable UUID ongoingSessionIdFor(DataStoreLifecycle lifecycle, UUID player) {
        return lifecycle.sessionTracker().currentSessionId(player);
    }

    // ---- suggestion providers ----

    /**
     * Online players + offline name-prefix matches from the datastore
     * (sorted by {@code last_seen} desc). Empty prefix skips the offline
     * lookup. Capped at {@link #MAX_PLAYER_SUGGESTIONS}.
     */
    private static SuggestionProvider<Sender> targetSuggestions(CloudPlatformCommandArguments arguments) {
        SuggestionProvider<Sender> onlineProvider = arguments.onlinePlayerSuggestions();
        return SuggestionProvider.blocking((ctx, in) -> {
            String partial = in.remainingInput();
            String partialLower = partial == null ? "" : partial.toLowerCase(Locale.ROOT);

            List<Suggestion> onlineSuggestions;
            try {
                Iterable<? extends Suggestion> onlineIt = onlineProvider.suggestionsFuture(ctx, in)
                        .toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
                onlineSuggestions = new ArrayList<>();
                onlineIt.forEach(onlineSuggestions::add);
            } catch (Exception e) {
                onlineSuggestions = List.of();
            }

            if (partialLower.isEmpty()) return onlineSuggestions;

            Set<String> seen = new HashSet<>();
            List<Suggestion> out = new ArrayList<>();
            for (Suggestion s : onlineSuggestions) {
                if (seen.add(s.suggestion().toLowerCase(Locale.ROOT))) {
                    out.add(s);
                    if (out.size() >= MAX_PLAYER_SUGGESTIONS) return out;
                }
            }

            DataStoreLifecycle dsl = ThunderAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.dataStore() == null) return out;
            try {
                Page<PlayerIdentity> page = dsl.dataStore().query(
                                Categories.PLAYER_IDENTITY,
                                Queries.listPlayersByNamePrefix(partialLower, MAX_PLAYER_SUGGESTIONS))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                for (PlayerIdentity id : page.items()) {
                    if (id.currentName() == null) continue;
                    if (seen.add(id.currentName().toLowerCase(Locale.ROOT))) {
                        out.add(Suggestion.suggestion(id.currentName()));
                        if (out.size() >= MAX_PLAYER_SUGGESTIONS) return out;
                    }
                }
            } catch (Exception e) {
                // Datastore unavailable or timed out — online-only fallback already populated.
            }
            return out;
        });
    }

    /** Suggest 1..maxPages for the list-pagination page number. */
    private static SuggestionProvider<Sender> listPageSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of();
            DataStoreLifecycle dsl = ThunderAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) return List.of();
            try {
                long total = dsl.historyService().countSessions(uuid)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                int entriesPerPage = dsl.config().history().entriesPerPage();
                int maxPages = Math.max(1, (int) ((total + entriesPerPage - 1) / Math.max(1, entriesPerPage)));
                return rangeSuggestions(1, Math.min(maxPages, MAX_SUGGESTIONS));
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    /** Suggest 1..totalSessions (capped) plus "latest". */
    private static SuggestionProvider<Sender> sessionSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of(Suggestion.suggestion(LATEST_ALIAS));
            DataStoreLifecycle dsl = ThunderAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) {
                return List.of(Suggestion.suggestion(LATEST_ALIAS));
            }
            try {
                long total = dsl.historyService().countSessions(uuid)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                int max = (int) Math.min(total, MAX_SUGGESTIONS);
                List<Suggestion> out = new ArrayList<>(max + 1);
                out.add(Suggestion.suggestion(LATEST_ALIAS));
                for (int i = 1; i <= max; i++) out.add(Suggestion.suggestion(Integer.toString(i)));
                return out;
            } catch (Exception e) {
                return List.of(Suggestion.suggestion(LATEST_ALIAS));
            }
        });
    }

    /**
     * Suggest 1..violationPages for the violation-page argument inside a session.
     * Relies on the already-parsed {@code session} argument in the context to
     * find the matching sessionId and count its violations.
     */
    private static SuggestionProvider<Sender> violationPageSuggestions() {
        return SuggestionProvider.blocking((ctx, in) -> {
            UUID uuid = resolveTargetUuid(ctx);
            if (uuid == null) return List.of();
            DataStoreLifecycle dsl = ThunderAPI.INSTANCE.getDataStoreLifecycle();
            if (dsl == null || !dsl.isLoaded() || dsl.historyService() == null) return List.of();
            String sessionRaw = ctx.getOrDefault("session", null);
            if (sessionRaw == null) return List.of();
            try {
                Integer ordinal = resolveSessionOrdinal(sessionRaw, uuid, dsl.historyService());
                if (ordinal == null) return List.of();
                SessionDetail detail;
                if (!(dsl.historyService() instanceof ac.grim.grimac.internal.storage.history.HistoryServiceImpl impl)) {
                    return List.of();
                }
                detail = impl.getSessionDetailByOrdinal(uuid, ordinal)
                        .toCompletableFuture().get(1, TimeUnit.SECONDS);
                if (detail == null) return List.of();
                int entriesPerPage = dsl.config().history().entriesPerPage();
                // Page unit depends on --detailed; without that info here, suggest
                // the larger of the two so we never under-offer. Detailed mode
                // paginates violations (larger count); grouped paginates buckets.
                int rows = Math.max(detail.violations().size(), detail.buckets().size());
                int maxPages = Math.max(1, (rows + entriesPerPage - 1) / Math.max(1, entriesPerPage));
                return rangeSuggestions(1, Math.min(maxPages, MAX_SUGGESTIONS));
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    private static @Nullable UUID resolveTargetUuid(CommandContext<Sender> ctx) {
        String target = ctx.getOrDefault("target", null);
        if (target == null) return null;
        return resolveUuid(target, ThunderAPI.INSTANCE.getDataStoreLifecycle(), null);
    }

    /**
     * Resolve history targets without platform offline-player lookups. Those
     * can synchronously hit profile storage and miss names known only through a
     * shared history backend.
     */
    private static @Nullable UUID resolveUuid(String name, @Nullable DataStoreLifecycle lifecycle) {
        return resolveUuid(name, lifecycle, onlineUuid(name));
    }

    private static @Nullable UUID resolveUuid(String name, @Nullable DataStoreLifecycle lifecycle, @Nullable UUID onlineUuid) {
        if (onlineUuid != null) return onlineUuid;
        if (lifecycle != null && lifecycle.isLoaded()) {
            NameResolver resolver = lifecycle.nameResolver();
            if (resolver != null) {
                try {
                    Optional<UUID> hit = resolver.resolveByName(name)
                            .toCompletableFuture().get(2, TimeUnit.SECONDS);
                    if (hit.isPresent()) return hit.get();
                } catch (Exception ignored) {
                }
            }
        }

        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @Nullable UUID onlineUuid(String name) {
        PlatformPlayer online = ThunderAPI.INSTANCE.getPlatformPlayerFactory().getFromName(name);
        return online == null ? null : online.getUniqueId();
    }

    private static List<Suggestion> rangeSuggestions(int fromInclusive, int toInclusive) {
        if (toInclusive < fromInclusive) return List.of();
        List<Suggestion> out = new ArrayList<>(toInclusive - fromInclusive + 1);
        for (int i = fromInclusive; i <= toInclusive; i++) out.add(Suggestion.suggestion(Integer.toString(i)));
        return out;
    }

    private Component message(String key, String fallback, Map<String, String> vars) {
        String raw = ThunderAPI.INSTANCE.getConfigManager().getConfig().getStringElse(key, fallback);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            raw = raw.replace("%" + e.getKey() + "%", e.getValue());
        }
        return MessageUtil.miniMessage(raw);
    }
}
