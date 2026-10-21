package ac.thunder.thunder.platform.fabric.mc1205;

import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.mc1194.Fabric1190PlatformServer;
import net.minecraft.commands.CommandSourceStack;

public class Fabric1203PlatformServer extends Fabric1190PlatformServer {

    @Override
    public double getTPS() {
        return Math.min(1000.0 / ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getCurrentSmoothedTickTime(), ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.tickRateManager().tickrate());
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = ThunderFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(commandSource, command);
    }
}
