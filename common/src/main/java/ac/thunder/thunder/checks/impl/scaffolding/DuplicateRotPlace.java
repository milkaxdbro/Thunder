package ac.thunder.thunder.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.impl.aim.processor.AimProcessor;
import ac.thunder.thunder.checks.type.BlockPlaceCheck;
import ac.thunder.thunder.checks.type.PostFlyingBlockPlaceListener;
import ac.thunder.thunder.checks.type.RotationListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;
import ac.thunder.thunder.utils.anticheat.update.RotationUpdate;

@CheckData(name = "DuplicateRotPlace", stableKey = "thunder.scaffolding.duplicate_rot_place", description = "Одинаковая дельта ротации при стройке", experimental = true)
public class DuplicateRotPlace extends BlockPlaceCheck implements RotationListener, PostFlyingBlockPlaceListener {
    private static final Verbose V = Verbose.of("x={f64} xdots={f64} y={f64}");

    private float deltaX, deltaY;
    private float lastPlacedDeltaX;
    private double lastPlacedDeltaDotsX;
    private double deltaDotsX;
    private boolean rotated = false;

    public DuplicateRotPlace(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        deltaX = rotationUpdate.deltaYawABS();
        deltaY = rotationUpdate.deltaPitchABS();
        deltaDotsX = player.checkManager.get(AimProcessor.class).deltaDotsYaw;
        rotated = true;
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (rotated && !player.inVehicle()) {
            if (deltaX > 2) {
                float xDiff = Math.abs(deltaX - lastPlacedDeltaX);
                double xDiffDots = Math.abs(deltaDotsX - lastPlacedDeltaDotsX);

                if (xDiff < 0.0001) {
                    flag(V.write(verbose()).f64(xDiff).f64(xDiffDots).f64(deltaY));
                } else {
                    reward();
                }
            } else {
                reward();
            }
            this.lastPlacedDeltaX = deltaX;
            this.lastPlacedDeltaDotsX = deltaDotsX;
            rotated = false;
        }
    }
}
