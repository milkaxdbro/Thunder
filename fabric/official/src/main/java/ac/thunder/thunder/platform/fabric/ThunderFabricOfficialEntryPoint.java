package ac.thunder.thunder.platform.fabric;

import ac.thunder.thunder.platform.fabric.inject.FabricMinecraftServerHandle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ThunderFabricOfficialEntryPoint extends AbstractThunderFabricEntryPoint<ThunderFabricOfficialLoaderPlugin> {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerEvents::fireServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::fireServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(FabricServerEvents::fireEndTick);
        initialize(
                "thunder26MainLoad",
                ThunderFabricOfficialLoaderPlugin.class,
                true
        );
    }

    @Override
    protected void setPlatformLoader(ThunderFabricOfficialLoaderPlugin platformLoader) {
        ThunderFabricOfficialLoaderPlugin.LOADER = platformLoader;
    }

    @Override
    protected void setNativeServer(FabricMinecraftServerHandle server) {
        ThunderFabricOfficialLoaderPlugin.FABRIC_SERVER = (MinecraftServer) (Object) server;
    }
}
