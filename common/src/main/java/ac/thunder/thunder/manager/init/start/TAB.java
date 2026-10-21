package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class TAB implements StartableInitable {

    @Override
    public void start() {
        if (ThunderAPI.INSTANCE.getPluginManager().getPlugin("TAB") == null) return;
        if (!ViaVersionUtil.isAvailable) return;
        // I don't know when team limits were changed, 1.13 is reasonable enough
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13))
            return;

        LogUtil.warn("Thunder has detected that you have installed TAB with ViaVersion.");
        LogUtil.warn("Please note that currently, TAB is incompatible as it sends illegal packets to players using versions newer than your server version.");
        LogUtil.warn("You may be able to remedy this by setting `compensate-for-packetevents-bug` to true in the TAB config.");
    }
}
