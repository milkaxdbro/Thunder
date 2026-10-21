package ac.thunder.thunder.platform.fabric.mc1161;

import ac.thunder.thunder.platform.fabric.AbstractFabricPlatformServer;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.mc1161.entity.Fabric1161ThunderEntity;
import ac.thunder.thunder.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.thunder.thunder.platform.fabric.mc1161.player.Fabric1161PlatformPlayer;
import ac.thunder.thunder.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.thunder.thunder.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.thunder.thunder.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.thunder.thunder.platform.fabric.utils.message.IFabricMessageUtil;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class ThunderFabric1161LoaderPlugin extends ThunderFabricIntermediaryLoaderPlugin {

    public ThunderFabric1161LoaderPlugin() {
        this(
            new FabricPlatformPlayerFactory(
                Fabric1161PlatformPlayer::new,
                Fabric1161ThunderEntity::new,
                Fabric1161PlatformInventory::new
            ),
            new Fabric1140PlatformServer(),
            new Fabric1161MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected ThunderFabric1161LoaderPlugin(
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        super(ThunderFabricIntermediaryLoaderPlugin::createCommandArguments,
            playerFactory,
            platformServer,
            fabricMessageUtil,
            fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_16_1;
    }
}
