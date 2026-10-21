package ac.thunder.thunder;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.events.GrimReloadEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.thunder.thunder.manager.config.ConfigManagerFileImpl;
import ac.thunder.thunder.manager.init.start.StartableInitable;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import ac.thunder.thunder.utils.common.ConfigReloadObserver;
import ac.thunder.thunder.utils.common.PropertiesUtil;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// This is used for thunder's external API. It has its own class just for organization.
public class ThunderExternalAPI implements GrimAbstractAPI, ConfigReloadObserver, StartableInitable {

    // Holder class — ThunderExternalAPI is constructed inside ThunderAPI's ctor,
    // so a plain static-final would see a null ThunderAPI.INSTANCE. Holder
    // init runs on first fire, after ThunderAPI is fully built.
    private static final class Channels {
        static final GrimReloadEvent.Channel RELOAD = ThunderAPI.INSTANCE.getEventBus().get(GrimReloadEvent.class);
    }

    private final ThunderAPI api;
    @Getter
    private final Map<String, Function<GrimUser, String>> variableReplacements = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> staticReplacements = new ConcurrentHashMap<>();
    private final Map<String, Function<Object, Object>> functions = new ConcurrentHashMap<>();
    private final ConfigManagerFileImpl configManagerFile = new ConfigManagerFileImpl();
    private final String thunderVersion;
    private ConfigManager configManager = null;
    private boolean started = false;

    public ThunderExternalAPI(ThunderAPI api) {
        this.api = api;
        this.thunderVersion = resolveThunderVersion(api);
    }

    @Override
    public @NotNull EventBus getEventBus() {
        return api.getEventBus();
    }

    @Deprecated
    @Override
    public @Nullable GrimUser getGrimUser(Player player) {
        return getGrimUser(player.getUniqueId());
    }

    @Override
    public @Nullable GrimUser getGrimUser(UUID uuid) {
        return api.getPlayerDataManager().getPlayer(uuid);
    }

    @Override
    public void registerVariable(String string, Function<GrimUser, String> replacement) {
        if (replacement == null) {
            variableReplacements.remove(string);
        } else {
            variableReplacements.put(string, replacement);
        }
    }

    @Override
    public void registerVariable(String variable, String replacement) {
        if (replacement == null) {
            staticReplacements.remove(variable);
        } else {
            staticReplacements.put(variable, replacement);
        }
    }

    @Override
    public String getGrimVersion() {
        return thunderVersion;
    }

    private static String resolveThunderVersion(ThunderAPI api) {
        try {
            Properties properties = PropertiesUtil.readProperties(ThunderExternalAPI.class, "thunder.properties");
            String buildVersion = properties.getProperty("build.version");
            if (buildVersion != null && !buildVersion.isBlank() && !buildVersion.startsWith("${")) {
                return buildVersion;
            }
        } catch (RuntimeException ignored) {
        }

        try {
            return api.getThunderPlugin().getDescription().getVersion();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    @Override
    public void registerFunction(String key, Function<Object, Object> function) {
        if (function == null) {
            functions.remove(key);
        } else {
            functions.put(key, function);
        }
    }

    @Override
    public Function<Object, Object> getFunction(String key) {
        return functions.get(key);
    }

    @Override
    public AlertManager getAlertManager() {
        return ThunderAPI.INSTANCE.getAlertManager();
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public int getCurrentTick() {
        return ThunderAPI.INSTANCE.getTickManager().currentTick;
    }

    @Override
    public @NotNull GrimPlugin getGrimPlugin(@NotNull Object o) {
        return this.api.getExtensionManager().getPlugin(o);
    }

    @Override
    public @NotNull BackendRegistry getBackendRegistry() {
        return api.getBackendRegistry();
    }

    // on load, load the config & register the service
    public void load() {
        reload(configManagerFile);
        api.getLoader().registerAPIService();
    }

    // handles any config loading that's needed to be done after load
    @Override
    public void start() {
        started = true;
        try {
            ThunderAPI.INSTANCE.getConfigManager().start();
        } catch (Exception e) {
            LogUtil.error("Не удалось запустить менеджер конфигов.", e);
        }
    }

    @Override
    public void reload(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(ThunderAPI.INSTANCE.getThunderPlugin(),
                    () -> successfulReload(config));
        } else {
            successfulReload(config);
        }
    }

    @Override
    public CompletableFuture<Boolean> reloadAsync(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(ThunderAPI.INSTANCE.getThunderPlugin(),
                    () -> future.complete(successfulReload(config)));
            return future;
        }
        return CompletableFuture.completedFuture(successfulReload(config));
    }

    private boolean successfulReload(ConfigManager config) {
        try {
            config.reload();
            ThunderAPI.INSTANCE.getConfigManager().load(config);
            if (started) ThunderAPI.INSTANCE.getConfigManager().start();
            onReload(config);
            if (started)
                ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(ThunderAPI.INSTANCE.getThunderPlugin(),
                        () -> Channels.RELOAD.fire(true));
            return true;
        } catch (Exception e) {
            LogUtil.error("Не удалось перезагрузить конфиг.", e);
        }
        if (started)
            ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(ThunderAPI.INSTANCE.getThunderPlugin(),
                    () -> Channels.RELOAD.fire(false));
        return false;
    }

    @Override
    public void onReload(ConfigManager newConfig) {
        if (newConfig == null) {
            LogUtil.warn("Менеджер конфигов не задан. Использую файловый по умолчанию.");
            configManager = configManagerFile;
        } else {
            configManager = newConfig;
        }
        // Update variables
        updateVariables();
        // Restart
        ThunderAPI.INSTANCE.getAlertManager().reload(configManager);
        ThunderAPI.INSTANCE.getDiscordManager().reload();
        ThunderAPI.INSTANCE.getSpectateManager().reload();
        // First-load guard: load() calls reload() before start() runs, so this fires once with started=false before the datastore exists. Subsequent /thunder reload calls see started=true and proceed (including disabled→enabled flips — DataStoreLifecycle.reload() re-evaluates builder.enabled() each time).
        if (!started) return;
        // Hot-reload picks up backend swaps + routing + connection-pool edits without a server restart. Drains in-flight writes for shutdown-drain-timeout-ms then drops; brief mid-reload unavailability is the tradeoff.
        if (ThunderAPI.INSTANCE.getDataStoreLifecycle() != null) {
            ThunderAPI.INSTANCE.getDataStoreLifecycle().reload();
        }
        // Reload checks for all players
        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.runSafely(() -> player.reload(configManager));
        }
    }

    private void updateVariables() {
        variableReplacements.putIfAbsent("%player%", user -> MessageUtil.toSmallCaps(user.getName()));
        variableReplacements.putIfAbsent("%uuid%", user -> user.getUniqueId().toString());
        variableReplacements.putIfAbsent("%ping%", user -> user.getTransactionPing() + "");
        variableReplacements.putIfAbsent("%brand%", user -> MessageUtil.toSmallCaps(user.getBrand()));
        variableReplacements.putIfAbsent("%h_sensitivity%", user -> ((int) Math.round(user.getHorizontalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%v_sensitivity%", user -> ((int) Math.round(user.getVerticalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%fast_math%", user -> MessageUtil.toSmallCaps(!user.isVanillaMath() ? "да" : "нет"));
        variableReplacements.putIfAbsent("%tps%", user -> String.format("%.2f", ThunderAPI.INSTANCE.getPlatformServer().getTPS()));
        variableReplacements.putIfAbsent("%version%", user -> MessageUtil.toSmallCaps(user.getVersionName()));
        // static variables
        staticReplacements.put("%prefix%", MessageUtil.translateAlternateColorCodes('&', ThunderAPI.INSTANCE.getConfigManager().getPrefix()));
        staticReplacements.putIfAbsent("%thunder_version%", getGrimVersion());
    }
}
