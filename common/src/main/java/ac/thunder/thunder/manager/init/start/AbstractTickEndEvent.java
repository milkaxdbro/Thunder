package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.player.ThunderPlayer;

// Intended for future events we inject all platforms at the end of a tick
public abstract class AbstractTickEndEvent implements StartableInitable {

    @Override
    public void start() {

    }

    protected void onEndOfTick(ThunderPlayer player, boolean flush) {
        player.packetEntityReplication.onEndOfTickEvent(true, flush);
    }

    protected boolean shouldInjectEndTick() {
        return ThunderAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("Reach.enable-post-packet", false);
    }
}
