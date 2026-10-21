package ac.thunder.thunder.platform.fabric.mc1194;

import ac.thunder.thunder.platform.fabric.AbstractFabricPlatformServer;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.mc1171.ThunderFabric1170LoaderPlugin;
import ac.thunder.thunder.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.thunder.thunder.platform.fabric.mc1194.convert.Fabric1190MessageUtil;
import ac.thunder.thunder.platform.fabric.mc1194.entity.Fabric1194ThunderEntity;
import ac.thunder.thunder.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.thunder.thunder.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.thunder.thunder.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.thunder.thunder.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.thunder.thunder.platform.fabric.utils.message.IFabricMessageUtil;
import ac.thunder.thunder.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class ThunderFabric1190LoaderPlugin extends ThunderFabric1170LoaderPlugin {

    public ThunderFabric1190LoaderPlugin() {
        this(
            ThunderFabricIntermediaryLoaderPlugin::createCommandArguments,
            new FabricPlatformPlayerFactory(
                    Fabric1170PlatformPlayer::new,
                    Fabric1194ThunderEntity::new,
                    PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_19_2)
                            ? Fabric1193PlatformInventory::new : Fabric1161PlatformInventory::new
            ),
            new Fabric1190PlatformServer(),
            new Fabric1190MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected ThunderFabric1190LoaderPlugin(
            LazyHolder<CloudPlatformCommandArguments> commandArguments,
            FabricPlatformPlayerFactory platformPlayerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil) {
        super(commandArguments, platformPlayerFactory, platformServer, fabricMessageUtil, fabricConversionUtil);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_19_4;
    }
}
