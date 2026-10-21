package ac.thunder.thunder.platform.fabric;

import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.SenderFactory;
import ac.thunder.thunder.platform.fabric.manager.FabricItemResetHandler;
import ac.thunder.thunder.platform.fabric.command.FabricPlayerSelectorParser;
import ac.thunder.thunder.platform.fabric.manager.FabricCloudPlatformCommandArguments;
import ac.thunder.thunder.platform.fabric.manager.FabricPermissionRegistrationManager;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.thunder.thunder.platform.fabric.scheduler.FabricPlatformScheduler;
import ac.thunder.thunder.platform.fabric.sender.AbstractFabricSenderFactory;
import ac.thunder.thunder.platform.fabric.sender.FabricIntermediarySenderFactory;
import me.lucko.fabric.api.permissions.v0.Permissions;
import ac.thunder.thunder.platform.fabric.utils.FabricIntermediaryPolymerHook;
import ac.thunder.thunder.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.thunder.thunder.platform.fabric.utils.message.IFabricMessageUtil;
import ac.thunder.thunder.utils.lazy.LazyHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public abstract class ThunderFabricIntermediaryLoaderPlugin extends AbstractThunderFabricLoaderPlugin<
        FabricPlatformPlayerFactory,
        AbstractFabricPlatformServer,
        FabricPlatformScheduler,
        FabricIntermediarySenderFactory,
        FabricItemResetHandler,
        CloudPlatformCommandArguments
        > {
    public static MinecraftServer FABRIC_SERVER;
    public static ThunderFabricIntermediaryLoaderPlugin LOADER;

    public ThunderFabricIntermediaryLoaderPlugin(
            LazyHolder<CloudPlatformCommandArguments> commandArguments,
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        super(
                LazyHolder.simple(FabricPlatformScheduler::new),
                LazyHolder.simple(FabricIntermediarySenderFactory::new),
                LazyHolder.simple(() -> new FabricItemResetHandler(fabricConversionUtil)),
                commandArguments,
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
                player -> FabricIntermediaryPolymerHook.createTranslator((ServerPlayer) player),
                fabricMessageUtil::textLiteral,
                platformServer::getProfileByName,
                fabricConversionUtil
        );
    }

    @Override
    public SenderFactory<CommandSourceStack> getSenderFactory() {
        return senderFactory.get();
    }

    public FabricIntermediarySenderFactory getFabricSenderFactory() {
        return senderFactory.get();
    }

    public static FabricCloudPlatformCommandArguments createCommandArguments() {
        return new FabricCloudPlatformCommandArguments(new FabricPlayerSelectorParser<>(
                selector -> LOADER.getFabricSenderFactory().wrap(selector.single().createCommandSourceStack()),
                selector -> selector.inputString()
        ));
    }

}
