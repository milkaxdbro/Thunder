package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.api.Platform;
import ac.thunder.thunder.utils.anticheat.LogUtil;

public class TickRunner implements StartableInitable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (ThunderAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(ThunderAPI.INSTANCE.getThunderPlugin(), () -> {
                ThunderAPI.INSTANCE.getTickManager().tickSync();
                ThunderAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            ThunderAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(ThunderAPI.INSTANCE.getThunderPlugin(), () -> ThunderAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            ThunderAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(ThunderAPI.INSTANCE.getThunderPlugin(), () -> ThunderAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
