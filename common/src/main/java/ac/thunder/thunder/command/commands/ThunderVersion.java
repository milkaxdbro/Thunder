package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import ac.thunder.thunder.utils.common.arguments.CommonThunderArguments;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ThunderVersion implements BuildableCommand {

    private static final AtomicReference<Component> updateMessage = new AtomicReference<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.of(CommonThunderArguments.URL_TIMEOUT.value(), ChronoUnit.MILLIS))
            .build();
    private static long lastCheck;

    public static void checkForUpdatesAsync(Sender sender) {
        String current = ThunderAPI.INSTANCE.getExternalAPI().getGrimVersion();
        sender.sendMessage(MessageUtil.getParsedComponent(sender, "version-current",
                "&#D080AC▍ &#CACABEВерсия Thunder: %version%",
                java.util.Map.of("%version%", MessageUtil.miniMessageSafe(current))));
        // use cached message if last check was less than 1 minute ago
        final long now = System.currentTimeMillis();
        if (now - lastCheck < 60000) {
            Component message = updateMessage.get();
            if (message != null) sender.sendMessage(message);
            return;
        }
        lastCheck = now;
        ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(ThunderAPI.INSTANCE.getThunderPlugin(), () -> checkForUpdates(sender));
    }

    // Using UserAgent format recommended by https://docs.modrinth.com/api/
    @SuppressWarnings("deprecation")
    private static void checkForUpdates(Sender sender) {
        try {
            //
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CommonThunderArguments.API_URL.value() + "updates"))
                    .GET()
                    .header("User-Agent", "Thunder/" + ThunderAPI.INSTANCE.getExternalAPI().getGrimVersion())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.of(CommonThunderArguments.URL_TIMEOUT.value(), ChronoUnit.MILLIS))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            final int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                Component msg = updateMessage.get();
                sender.sendMessage(Objects.requireNonNullElseGet(msg, () -> MessageUtil.getParsedComponent(sender, "version-check-failed",
                        "&#D080AC▍ &#CACABEНе удалось проверить последнюю версию Thunder. Сервер обновлений ответил кодом: %code%",
                        java.util.Map.of("%code%", String.valueOf(statusCode)))));
                return;
            }
            // Using old JsonParser method, as old versions of Gson don't include the static one
            JsonObject object = new JsonParser().parse(response.body()).getAsJsonObject();
            String downloadPage = getJsonString(object, "download_page", "Unknown");
            String latest = getJsonString(object, "latest_version", "Unknown");
            @Nullable String warning = getJsonString(object, "warning", null);
            // allow status to be overridden if provided
            Status status;
            if (object.has("status")) {
                status = Status.getStatus(object.get("status").getAsString());
            } else {
                status = Status.SemVer.getVersionStatus(ThunderAPI.INSTANCE.getExternalAPI().getGrimVersion(), latest);
            }
            //
            Component msg = switch (status) {
                case AHEAD ->
                        MessageUtil.getParsedComponent(sender, "version-dev",
                                "&#D080AC▍ &#CACABEТы используешь dev-версию Thunder");
                case UPDATED ->
                        MessageUtil.getParsedComponent(sender, "version-latest",
                                "&#D080AC▍ &#CACABEУ тебя последняя версия Thunder");
                case OUTDATED ->
                        MessageUtil.getParsedComponent(sender, "version-outdated",
                                "&#D080AC▍ &#CACABEНайдена новая версия Thunder! Версия %latest% доступна для скачивания здесь: <click:open_url:%page%>&#D080AC&n%page%</click>",
                                java.util.Map.of("%latest%", MessageUtil.miniMessageSafe(latest),
                                        "%page%", downloadPage));
                case UNKNOWN ->
                        MessageUtil.getParsedComponent(sender, "version-unknown",
                                "&#D080AC▍ &#CACABEТы используешь неизвестную версию Thunder.");
            };
            // in case of a critical exploit that requires attention, allow us to provide a warning
            if (warning != null && !warning.isBlank()) {
                msg = msg.append(Component.text().append(Component.text(warning).color(NamedTextColor.RED)).build());
            }
            updateMessage.set(msg);
            sender.sendMessage(msg);
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "version-check-error",
                    "&#D080AC▍ &#CACABEНе удалось проверить последнюю версию."));
            LogUtil.error("Не удалось проверить последнюю версию thunder.", e);
        }
    }

    private static String getJsonString(JsonObject object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("version")
                        .permission("thunder.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        checkForUpdatesAsync(sender);
    }


    @AllArgsConstructor
    private enum Status {
        AHEAD("ahead"),
        UPDATED("updated"),
        OUTDATED("outdated"),
        UNKNOWN("unknown");

        private final String id;

        public static Status getStatus(String id) {
            for (Status status : Status.values()) {
                if (status.id.equals(id)) return status;
            }
            return UNKNOWN;
        }

        private static class SemVer {

            public static Status getVersionStatus(String current, String latest) {
                try {
                    var cmp = compareSemver(current, latest);
                    if (cmp == 0) {
                        return Status.UPDATED;
                    }
                    if (cmp < 0) {
                        return Status.OUTDATED;
                    }
                    return Status.AHEAD;
                } catch (Exception ignored) {}
                return Status.UNKNOWN;
            }

            public static String normalizeCoreVersion(String version) {
                String trimmed = version.trim();
                String[] dashParts = trimmed.split("-");
                String[] plusParts = dashParts[0].split("\\+");
                return plusParts[0];
            }

            public static int[] parseVersion(String version) {
                String core = normalizeCoreVersion(version);
                if (core.isEmpty()) return null;
                String[] parts = core.split("\\.");
                if (parts.length < 1) return null;

                int major = parseInt(parts[0]);
                int minor = parts.length > 1 ? parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? parseInt(parts[2]) : 0;

                if (major < 0 || minor < 0 || patch < 0) {
                    return null;
                }

                return new int[] { major, minor, patch };
            }

            private static int parseInt(String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }

            public static int compareSemver(String a, String b) {
                int[] pa = parseVersion(a);
                int[] pb = parseVersion(b);
                if (pa == null || pb == null) return 0;

                for (int i = 0; i < 3; i++) {
                    if (pa[i] < pb[i]) return -1;
                    if (pa[i] > pb[i]) return 1;
                }
                return 0;
            }
        }
    }
}
