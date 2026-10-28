package ac.thunder.thunder.platform.bukkit;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.ThunderExternalAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.thunder.thunder.command.CloudCommandService;
import ac.grim.grimac.internal.platform.bukkit.resolver.BukkitResolverRegistrar;
import ac.thunder.thunder.manager.init.Initable;
import ac.thunder.thunder.manager.init.start.ExemptOnlinePlayersOnReload;
import ac.thunder.thunder.manager.init.start.StartableInitable;
import ac.thunder.thunder.platform.api.Platform;
import ac.thunder.thunder.platform.api.PlatformLoader;
import ac.thunder.thunder.platform.api.PlatformServer;
import ac.thunder.thunder.platform.api.command.CommandService;
import ac.thunder.thunder.platform.api.manager.ItemResetHandler;
import ac.thunder.thunder.platform.api.manager.MessagePlaceHolderManager;
import ac.thunder.thunder.platform.api.manager.PlatformPluginManager;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.player.PlatformPlayerFactory;
import ac.thunder.thunder.platform.api.scheduler.PlatformScheduler;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.platform.api.sender.SenderFactory;
import ac.thunder.thunder.platform.bukkit.initables.BukkitBStats;
import ac.thunder.thunder.platform.bukkit.initables.BukkitEventManager;
import ac.thunder.thunder.platform.bukkit.initables.BukkitLuckPermsInitable;
import ac.thunder.thunder.platform.bukkit.initables.BukkitTickEndEvent;
import ac.thunder.thunder.platform.bukkit.manager.BukkitItemResetHandler;
import ac.thunder.thunder.platform.bukkit.manager.BukkitMessagePlaceHolderManager;
import ac.thunder.thunder.platform.bukkit.manager.BukkitCloudPlatformCommandArguments;
import ac.thunder.thunder.platform.bukkit.manager.BukkitPermissionRegistrationManager;
import ac.thunder.thunder.platform.bukkit.manager.BukkitPlatformPluginManager;
import ac.thunder.thunder.platform.bukkit.player.BukkitPlatformPlayerFactory;
import ac.thunder.thunder.platform.bukkit.scheduler.bukkit.BukkitPlatformScheduler;
import ac.thunder.thunder.platform.bukkit.scheduler.folia.FoliaPlatformScheduler;
import ac.thunder.thunder.platform.bukkit.sender.BukkitSenderFactory;
import ac.thunder.thunder.platform.bukkit.utils.placeholder.PlaceholderAPIExpansion;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class ThunderBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {
    public static ThunderBukkitLoaderPlugin LOADER;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(this::createScheduler);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.simple(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.simple(BukkitSenderFactory::new);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(BukkitItemResetHandler::new);
    private final LazyHolder<CommandService> commandService = LazyHolder.simple(this::createCommandService);
    private final CloudPlatformCommandArguments commandArguments = new BukkitCloudPlatformCommandArguments();

    @Getter private final PlatformPlayerFactory platformPlayerFactory = new BukkitPlatformPlayerFactory();
    @Getter private final PlatformPluginManager pluginManager = new BukkitPlatformPluginManager();
    @Getter private final GrimPlugin plugin;
    @Getter private final PlatformServer platformServer = new BukkitPlatformServer();
    @Getter private final MessagePlaceHolderManager messagePlaceHolderManager = new BukkitMessagePlaceHolderManager();
    @Getter private final BukkitPermissionRegistrationManager permissionManager = new BukkitPermissionRegistrationManager();

    public ThunderBukkitLoaderPlugin() {
        BukkitResolverRegistrar registrar = new BukkitResolverRegistrar();
        registrar.registerAll(ThunderAPI.INSTANCE.getExtensionManager());
        this.plugin = registrar.resolvePlugin(this);
    }

    @Override
    public void onLoad() {
        LOADER = this;
        printBanner();
        ThunderAPI.INSTANCE.load(this, this.getBukkitInitTasks());
    }

    private void printBanner() {
        final java.util.logging.Logger log = Bukkit.getLogger();
        for (final String line : BANNER) log.info(line);
        log.info("Версия: " + getDescription().getVersion());
        log.info("Поддержка версий Minecraft: 1.19 – 26.3 (Paper / Purpur)");
        log.info(" ");
    }

    /**
     * Startup banner. Kept as a plain string array so it prints even before the
     * plugin is fully loaded.
     */
    private static final String[] BANNER = {
            " ",
            "██████████  ██      ██  ██      ██  ██      ██  ████████    ██████████  ████████    ",
            "██████████  ██      ██  ██      ██  ██      ██  ████████    ██████████  ████████    ",
            "    ██      ██      ██  ██      ██  ████    ██  ██      ██  ██          ██      ██  ",
            "    ██      ██      ██  ██      ██  ████    ██  ██      ██  ██          ██      ██  ",
            "    ██      ██████████  ██      ██  ██  ██  ██  ██      ██  ████████    ████████    ",
            "    ██      ██████████  ██      ██  ██  ██  ██  ██      ██  ████████    ████████    ",
            "    ██      ██      ██  ██      ██  ██    ████  ██      ██  ██          ██    ██    ",
            "    ██      ██      ██  ██      ██  ██    ████  ██      ██  ██          ██    ██    ",
            "    ██      ██      ██    ██████    ██      ██  ████████    ██████████  ██      ██  ",
            "    ██      ██      ██    ██████    ██      ██  ████████    ██████████  ██      ██  ",
            " ",
            "Спасибо за использования ThunderAC! По всем вопросам @froriym81",
            " "
    };

    private Initable[] getBukkitInitTasks() {
        return new Initable[] {
                new ExemptOnlinePlayersOnReload(),
                new BukkitEventManager(),
                new BukkitTickEndEvent(),
                new BukkitBStats(),
                new BukkitLuckPermsInitable(),
                (StartableInitable) () -> {
                    if (BukkitMessagePlaceHolderManager.hasPlaceholderAPI) {
                        new PlaceholderAPIExpansion().register();
                    }
                }
        };
    }

    @Override
    public void onEnable() {
        ThunderAPI.INSTANCE.start();
    }

    @Override
    public void onDisable() {
        ThunderAPI.INSTANCE.stop();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler.get();
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents.get();
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public CommandService getCommandService() {
        return commandService.get();
    }

    @Override
    public SenderFactory<CommandSender> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    @SuppressWarnings("removal")
    public void registerAPIService() {
        final ThunderExternalAPI externalAPI = ThunderAPI.INSTANCE.getExternalAPI();
        final EventBus eventBus = externalAPI.getEventBus();
        final ac.grim.grimac.api.plugin.GrimPlugin plugin = ThunderAPI.INSTANCE.getThunderPlugin();

        // Bridge Thunder events → legacy Bukkit Event API so pre-1.3 plugins that
        // listened for ac.grim.grimac.api.events.* Bukkit events keep working.
        // Typed channel subscriptions here are plugin-bound so they go away if
        // Thunder itself is disabled.

        eventBus.get(ac.grim.grimac.api.event.events.GrimJoinEvent.class).onJoin(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimJoinEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimQuitEvent.class).onQuit(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimQuitEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimReloadEvent.class).onReload(plugin, (success) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimReloadEvent(success));
        });

        eventBus.subscribe(plugin, ac.grim.grimac.api.event.events.FlagEvent.class, event -> {
            ac.grim.grimac.api.events.FlagEvent bukkitEvent =
                    new ac.grim.grimac.api.events.FlagEvent(event.getUser(), event.getCheck(), event::getVerbose);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            event.setCancelled(event.isCancelled() || bukkitEvent.isCancelled());
        }, 0, false, ThunderBukkitLoaderPlugin.class);

        eventBus.get(ac.grim.grimac.api.event.events.CommandExecuteEvent.class).onCommandExecuteSupplier(plugin, (user, check, verbose, command, cancelled) -> {
            ac.grim.grimac.api.events.CommandExecuteEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CommandExecuteEvent(user, check, verbose, command);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        eventBus.get(ac.grim.grimac.api.event.events.CompletePredictionEvent.class).onCompletePrediction(plugin, (user, check, offset, cancelled) -> {
            // Legacy Bukkit event has a verbose field that the new channel event does not; pass empty.
            ac.grim.grimac.api.events.CompletePredictionEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CompletePredictionEvent(user, check, "", offset);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        GrimAPIProvider.init(externalAPI);
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, externalAPI, this, ServicePriority.Normal);
    }

    private PlatformScheduler createScheduler() {
        return ThunderAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler();
    }

    private CommandService createCommandService() {
        try {
            return new CloudCommandService(this::createCloudCommandManager, commandArguments);
        } catch (Throwable t) {
            LogUtil.warn("КРИТИЧНО: Не удалось инициализировать командный фреймворк. " +
                    "thunder продолжит работу без команд.", t);
            return () -> {};
        }
    }

    private CommandManager<Sender> createCloudCommandManager() {
        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {
                manager.registerBrigadier();
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } catch (Throwable t) {
                LogUtil.error("Не удалось зарегистрировать нативные подсказки Brigadier. Использую стандартные.", t);
            }
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
        return manager;
    }

    public BukkitSenderFactory getBukkitSenderFactory() {
        return senderFactory.get();
    }
}
