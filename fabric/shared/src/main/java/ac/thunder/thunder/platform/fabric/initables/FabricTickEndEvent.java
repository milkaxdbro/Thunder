package ac.thunder.thunder.platform.fabric.initables;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.manager.init.start.AbstractTickEndEvent;
import ac.thunder.thunder.platform.fabric.FabricServerEvents;
import ac.thunder.thunder.player.ThunderPlayer;

public class FabricTickEndEvent extends AbstractTickEndEvent {

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }

        FabricServerEvents.onEndTick(server -> tickAllPlayers());
    }

    private void tickAllPlayers() {
        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableThunder) continue;
            super.onEndOfTick(player, true);
        }
    }
}
