package ac.thunder.thunder.checks.impl.baritone;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.impl.aim.processor.AimProcessor;
import ac.thunder.thunder.checks.type.RotationListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.RotationUpdate;
import ac.thunder.thunder.utils.math.ThunderMath;

// This check has been patched by Baritone for a long time, and it also seems to false with cinematic camera now, so it is disabled.
@CheckData(name = "Baritone", stableKey = "thunder.baritone.baritone", description = "Поведение как у Baritone")
public class Baritone extends Check implements RotationListener {
    private static final Verbose V = Verbose.of("divisor={f64}");

    private int verbose;

    public Baritone(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final float deltaPitch = Math.abs(rotationUpdate.newPitch() - rotationUpdate.oldPitch());

        // Baritone works with small degrees, limit to 1 degree to pick up on baritone slightly moving aim to bypass anticheats
        if (rotationUpdate.deltaYaw() == 0 && deltaPitch > 0 && deltaPitch < 1 && Math.abs(rotationUpdate.newPitch()) != 90.0f) {
            AimProcessor processor = player.checkManager.get(AimProcessor.class);
            if (processor.divisorPitch < ThunderMath.MINIMUM_DIVISOR) {
                verbose++;
                if (verbose > 8) {
                    double divisor = AimProcessor.convertToSensitivity(processor.divisorYaw);
                    flag(V.write(verbose()).f64(divisor));
                }
            } else {
                verbose = 0;
            }
        }
    }
}
