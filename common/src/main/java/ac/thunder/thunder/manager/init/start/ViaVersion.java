package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.viaversion.viaversion.api.Via;

public class ViaVersion implements StartableInitable {

    @Override
    public void start() {
        if (!ViaVersionUtil.isAvailable) return;

        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();

        if (Via.getConfig().getValues().containsKey("fix-1_21-placement-rotation") && Via.getConfig().fix1_21PlacementRotation() && serverVersion.isOlderThan(ServerVersion.V_1_21)) {
            LogUtil.error("Thunder has detected that you are using ViaVersion with the `fix-1_21-placement-rotation` option enabled.");
            LogUtil.error("This option is known to cause issues with Thunder and may result in false positives and bypasses.");
            LogUtil.error("Please disable this option in your ViaVersion configuration to prevent these issues.");
        }

        if (ThunderAPI.INSTANCE.getPluginManager().getPlugin("ViaBackwards") != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            LogUtil.warn("Thunder has detected that you have installed ViaBackwards on a 1.21.2+ server.");
            LogUtil.warn("This setup is currently unsupported and you will experience issues with older clients using vehicles.");
        }
    }
}
