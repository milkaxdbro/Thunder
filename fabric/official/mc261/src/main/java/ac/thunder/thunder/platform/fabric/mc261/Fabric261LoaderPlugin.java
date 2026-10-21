package ac.thunder.thunder.platform.fabric.mc261;

import ac.thunder.thunder.platform.fabric.ThunderFabricOfficialLoaderPlugin;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class Fabric261LoaderPlugin extends ThunderFabricOfficialLoaderPlugin {

    public Fabric261LoaderPlugin() {
        super(
                new FabricPlatformPlayerFactory(
                        Fabric261PlatformPlayer::new,
                        Fabric261ThunderEntity::new,
                        Fabric261PlatformInventory::new
                ),
                new Fabric261PlatformServer(),
                new Fabric261MessageUtil(),
                new Fabric261ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
