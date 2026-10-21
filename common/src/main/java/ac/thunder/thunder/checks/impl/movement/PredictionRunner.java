package ac.thunder.thunder.checks.impl.movement;

import ac.thunder.thunder.checks.ThunderProcessor;
import ac.thunder.thunder.checks.type.PositionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PositionUpdate;

public class PredictionRunner extends ThunderProcessor implements PositionListener {

    public PredictionRunner(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (!player.inVehicle()) {
            player.movementCheckRunner.processAndCheckMovementPacket(positionUpdate);
        }
    }
}
