package ac.thunder.thunder.platform.bukkit.initables;

import ac.thunder.thunder.manager.init.start.StartableInitable;
import ac.thunder.thunder.platform.bukkit.ThunderBukkitLoaderPlugin;
import ac.thunder.thunder.utils.anticheat.Constants;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements StartableInitable {
    @Override
    public void start() {
        try {
            new Metrics(ThunderBukkitLoaderPlugin.LOADER, Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }
}
