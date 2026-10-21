package ac.thunder.thunder.manager.tick.impl;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.manager.tick.Tickable;
import ac.thunder.thunder.player.ThunderPlayer;

public class ClearRecentlyUpdatedBlocks implements Tickable {

    private static final int maxTickAge = 2;

    @Override
    public void tick() {
        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.blockHistory.cleanup(ThunderAPI.INSTANCE.getTickManager().currentTick - maxTickAge);
        }
    }
}
