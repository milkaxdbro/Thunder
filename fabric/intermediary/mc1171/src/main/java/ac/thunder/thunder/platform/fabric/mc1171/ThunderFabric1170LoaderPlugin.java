package ac.thunder.thunder.platform.fabric.mc1171;

import ac.thunder.thunder.platform.fabric.AbstractFabricPlatformServer;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.thunder.thunder.platform.fabric.mc1161.Fabric1140PlatformServer;
import ac.thunder.thunder.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.thunder.thunder.platform.fabric.mc1171.entity.Fabric1170ThunderEntity;
import ac.thunder.thunder.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.thunder.thunder.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.thunder.thunder.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.thunder.thunder.platform.fabric.utils.message.IFabricMessageUtil;
import ac.thunder.thunder.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class ThunderFabric1170LoaderPlugin extends ThunderFabricIntermediaryLoaderPlugin {

    public ThunderFabric1170LoaderPlugin() {
        this(ThunderFabricIntermediaryLoaderPlugin::createCommandArguments,
                new FabricPlatformPlayerFactory(
                        Fabric1170PlatformPlayer::new,
                        Fabric1170ThunderEntity::new,
                        Fabric1161PlatformInventory::new
                ),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_17)
                        ? new Fabric1171PlatformServer() : new Fabric1140PlatformServer(),
                new Fabric1161MessageUtil(),
                new Fabric1140ConversionUtil()
        );
    }

    protected ThunderFabric1170LoaderPlugin(LazyHolder<CloudPlatformCommandArguments> commandArguments,
                                           FabricPlatformPlayerFactory playerFactory,
                                           AbstractFabricPlatformServer platformServer,
                                           IFabricMessageUtil fabricMessageUtil,
                                           IFabricConversionUtil fabricConversionUtil) {
        super(
                commandArguments,
                playerFactory,
                platformServer,
                fabricMessageUtil,
                fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_17_1;
    }
}
