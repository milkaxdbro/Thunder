package ac.thunder.thunder.platform.fabric.utils.thread;

import ac.thunder.thunder.ThunderAPI;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FabricFutureUtil {
    public static <U> CompletableFuture<U> supplySync(Supplier<U> entityTeleportSupplier) {
        CompletableFuture<U> ret = new CompletableFuture<>();
        ThunderAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(ThunderAPI.INSTANCE.getThunderPlugin(),
                () -> ret.complete(entityTeleportSupplier.get()));
        return ret;
    }
}
