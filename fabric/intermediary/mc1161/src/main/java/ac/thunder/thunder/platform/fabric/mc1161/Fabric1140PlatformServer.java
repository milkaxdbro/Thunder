package ac.thunder.thunder.platform.fabric.mc1161;

import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.platform.fabric.AbstractFabricPlatformServer;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.player.FabricOfflineProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public class Fabric1140PlatformServer extends AbstractFabricPlatformServer {

    @Override
    public int getOperatorPermissionLevel() {
        return ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getOperatorUserPermissionLevel();
    }

    @Override
    public boolean hasPermission(Sender sender, int level) {
        return ((CommandSourceStack) sender).hasPermission(level);
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = ThunderFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getCommands().performCommand(commandSource, command);
    }

    @Override
    public double getTPS() {
        return Math.min(1000.0 / ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getAverageTickTime(), 20.0);
    }

    @Override
    public @Nullable FabricOfflineProfile getProfileByName(String name) {
        GameProfile profile = ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getProfileCache().get(name);
        return profile != null ? new FabricOfflineProfile(profile.getId(), profile.getName()) : null;
    }
}
