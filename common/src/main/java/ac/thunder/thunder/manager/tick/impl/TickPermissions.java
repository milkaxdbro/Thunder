package ac.thunder.thunder.manager.tick.impl;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.manager.config.BaseConfigManager;
import ac.thunder.thunder.manager.tick.Tickable;
import ac.thunder.thunder.player.ThunderPlayer;

public class TickPermissions implements Tickable {

    @Override
    public void tick() {
        BaseConfigManager config = ThunderAPI.INSTANCE.getConfigManager();
        int interval = config.getUpdatePermissionTicks();
        if (interval <= 0 || ThunderAPI.INSTANCE.getTickManager().currentTick % interval != 0) return;

        for (ThunderPlayer player : ThunderAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.updatePermissions();
        }
    }
}
