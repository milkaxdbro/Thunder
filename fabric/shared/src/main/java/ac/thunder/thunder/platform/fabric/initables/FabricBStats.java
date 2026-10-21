package ac.thunder.thunder.platform.fabric.initables;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.manager.init.start.StartableInitable;
import ac.thunder.thunder.manager.init.stop.StoppableInitable;
import ac.thunder.thunder.platform.fabric.utils.metrics.MetricsFabric;
import ac.thunder.thunder.utils.anticheat.Constants;

public class FabricBStats implements StartableInitable, StoppableInitable {

    private MetricsFabric metricsFabric;

    @Override
    public void start() {
        try {
            metricsFabric = new MetricsFabric(ThunderAPI.INSTANCE.getThunderPlugin(), Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }

    @Override
    public void stop() {
        if (metricsFabric != null)
            metricsFabric.shutdown();
    }
}
