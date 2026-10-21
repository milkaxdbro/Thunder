package ac.thunder.thunder.platform.fabric;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.fabric.initables.FabricBStats;
import ac.thunder.thunder.platform.fabric.initables.FabricLuckPermsInitable;
import ac.thunder.thunder.platform.fabric.initables.FabricTickEndEvent;
import ac.thunder.thunder.platform.fabric.inject.FabricMinecraftServerHandle;
import ac.thunder.thunder.platform.fabric.scheduler.FabricPlatformScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;
import java.util.Objects;

public abstract class AbstractThunderFabricEntryPoint<P extends AbstractThunderFabricLoaderPlugin<?, ?, ?, ?, ?, ?>>
        implements PreLaunchEntrypoint, ModInitializer {
    private static volatile FabricMinecraftServerHandle server;

    @Override
    public void onPreLaunch() {
    }

    public static FabricMinecraftServerHandle server() {
        return Objects.requireNonNull(server, "server");
    }

    public static FabricMinecraftServerHandle serverOrNull() {
        return server;
    }

    protected void initialize(
            String entryPointName,
            Class<P> pluginClass,
            boolean allowMissingEntryPoint
    ) {
        List<P> entryPoints = FabricLoader.getInstance().getEntrypoints(entryPointName, pluginClass);
        entryPoints.sort((a, b) -> b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());

        if (entryPoints.isEmpty()) {
            if (allowMissingEntryPoint) return;
            throw new IllegalStateException("No Fabric platform entrypoint found for " + entryPointName);
        }

        P platformLoader = entryPoints.get(0);
        setPlatformLoader(platformLoader);

        ThunderAPI.INSTANCE.load(
                platformLoader,
                new FabricBStats(),
                new FabricTickEndEvent(),
                new FabricLuckPermsInitable()
        );

        ThunderAPI.INSTANCE.getCommandService().registerCommands();

        FabricServerEvents.onServerStarting(nativeServer -> {
            setNativeServer(nativeServer);
            server = nativeServer;
            ThunderAPI.INSTANCE.start();
        });

        FabricServerEvents.onServerStopping(nativeServer -> {
            ThunderAPI.INSTANCE.stop();
            ((FabricPlatformScheduler) platformLoader.getScheduler()).shutdown();
        });
    }

    protected abstract void setPlatformLoader(P platformLoader);

    protected abstract void setNativeServer(FabricMinecraftServerHandle server);
}
