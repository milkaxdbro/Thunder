package ac.thunder.thunder.checks.impl.movement;

import ac.thunder.thunder.checks.ThunderProcessor;
import ac.thunder.thunder.checks.type.VehicleListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PositionUpdate;
import ac.thunder.thunder.utils.anticheat.update.VehiclePositionUpdate;

public class VehiclePredictionRunner extends ThunderProcessor implements VehicleListener {
    public VehiclePredictionRunner(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void process(final VehiclePositionUpdate vehicleUpdate) {
        // Vehicle onGround = false always
        // We don't do vehicle setbacks because vehicle netcode sucks.
        player.movementCheckRunner.processAndCheckMovementPacket(new PositionUpdate(vehicleUpdate.from(), vehicleUpdate.to(), false, null, null, vehicleUpdate.isTeleport()));
    }
}
