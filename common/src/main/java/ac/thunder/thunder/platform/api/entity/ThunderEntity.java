package ac.thunder.thunder.platform.api.entity;

import ac.grim.grimac.api.GrimIdentity;
import ac.thunder.thunder.platform.api.world.PlatformWorld;
import ac.thunder.thunder.utils.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ThunderEntity extends GrimIdentity {
    /**
     * Eject any passenger.
     *
     * @return True if there was a passenger.
     */
    boolean eject();

    CompletableFuture<Boolean> teleportAsync(Location location);

    @NotNull
    Object getNative();

    boolean isDead();

    PlatformWorld getWorld();

    Location getLocation();

    double distanceSquared(double x, double y, double z);
}
