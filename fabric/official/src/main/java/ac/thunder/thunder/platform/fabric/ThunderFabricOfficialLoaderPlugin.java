package ac.thunder.thunder.platform.fabric;

import ac.thunder.thunder.platform.fabric.manager.FabricItemResetHandler;
import ac.thunder.thunder.platform.fabric.manager.FabricCloudPlatformCommandArguments;
import ac.thunder.thunder.platform.fabric.manager.FabricPermissionRegistrationManager;
import ac.thunder.thunder.platform.fabric.command.FabricPlayerSelectorParser;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.thunder.thunder.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.thunder.thunder.platform.fabric.sender.AbstractFabricSenderFactory;
import ac.thunder.thunder.platform.fabric.sender.FabricOfficialSenderFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;
import ac.thunder.thunder.platform.fabric.utils.FabricOfficialPolymerHook;
import ac.thunder.thunder.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.thunder.thunder.platform.fabric.utils.message.IFabricMessageUtil;
import ac.thunder.thunder.utils.lazy.LazyHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public abstract class ThunderFabricOfficialLoaderPlugin extends AbstractThunderFabricLoaderPlugin<
        FabricPlatformPlayerFactory,
        AbstractFabricPlatformServer,
        FabricPlatformScheduler,
        FabricOfficialSenderFactory,
        FabricItemResetHandler,
        FabricCloudPlatformCommandArguments
        > {
    public static MinecraftServer FABRIC_SERVER;
    public static ThunderFabricOfficialLoaderPlugin LOADER;

    public ThunderFabricOfficialLoaderPlugin(
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        super(
                LazyHolder.simple(FabricPlatformScheduler::new),
                LazyHolder.simple(FabricOfficialSenderFactory::new),
                LazyHolder.simple(() -> new FabricItemResetHandler(fabricConversionUtil)),
                LazyHolder.simple(ThunderFabricOfficialLoaderPlugin::createCommandArguments),
                LazyHolder.simple(() -> new FabricPermissionRegistrationManager(
                        LOADER.getFabricSenderFactory(),
                        name -> {
                            if (AbstractFabricSenderFactory.HAS_PERMISSIONS_API) {
                                Permissions.check(FABRIC_SERVER.createCommandSourceStack(), name);
                            }
                        })),
                playerFactory,
                platformServer,
                fabricMessageUtil,
                fabricConversionUtil
        );
        FabricPlatformServices.configure(
                playerFactory::getPlatformInventory,
                playerFactory::getPlatformEntity,
                player -> FabricOfficialPolymerHook.createTranslator((ServerPlayer) player),
                fabricMessageUtil::textLiteral,
                platformServer::getProfileByName,
                fabricConversionUtil
        );
    }

    public FabricOfficialSenderFactory getFabricSenderFactory() {
        return senderFactory.get();
    }

    public static FabricCloudPlatformCommandArguments createCommandArguments() {
        return new FabricCloudPlatformCommandArguments(new FabricPlayerSelectorParser<>(
                selector -> LOADER.getFabricSenderFactory().wrap(selector.single().createCommandSourceStack()),
                selector -> selector.inputString()
        ));
    }

}
