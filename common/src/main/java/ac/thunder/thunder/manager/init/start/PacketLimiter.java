package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.player.ThunderPlayer;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(ThunderAPI.INSTANCE.getThunderPlugin(), () -> {
            for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
