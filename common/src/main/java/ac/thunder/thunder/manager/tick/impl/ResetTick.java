package ac.thunder.thunder.manager.tick.impl;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.manager.tick.Tickable;
import ac.thunder.thunder.player.ThunderPlayer;

public class ResetTick implements Tickable {
    @Override
    public void tick() {
        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.packetEntityReplication.tickStartTick();
        }
    }
}
