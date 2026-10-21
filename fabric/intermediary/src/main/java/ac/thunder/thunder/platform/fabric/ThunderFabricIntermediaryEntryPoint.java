package ac.thunder.thunder.platform.fabric;

import ac.thunder.thunder.platform.fabric.inject.FabricMinecraftServerHandle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ThunderFabricIntermediaryEntryPoint extends AbstractThunderFabricEntryPoint<ThunderFabricIntermediaryLoaderPlugin> {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerEvents::fireServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::fireServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(FabricServerEvents::fireEndTick);
        initialize(
                "thunderMainLoad",
                ThunderFabricIntermediaryLoaderPlugin.class,
                false
        );
    }

    @Override
    protected void setPlatformLoader(ThunderFabricIntermediaryLoaderPlugin platformLoader) {
        ThunderFabricIntermediaryLoaderPlugin.LOADER = platformLoader;
    }

    @Override
    protected void setNativeServer(FabricMinecraftServerHandle server) {
        ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER = (MinecraftServer) server;
    }
}
