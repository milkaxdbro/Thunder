package ac.thunder.thunder.checks.impl.aim;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.RotationListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimDuplicateLook", stableKey = "thunder.aim.duplicate_look", description = "Дувлирующее обновление взгляда без его смены")
public class AimDuplicateLook extends Check implements RotationListener {
    private boolean exempt;

    public AimDuplicateLook(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate || player.compensatedEntities.self.getRiding() != null) {
            exempt = true;
            return;
        }

        if (exempt) { // Exempt for a tick on teleport
            exempt = false;
            return;
        }

        if (rotationUpdate.oldYaw() == rotationUpdate.newYaw() && rotationUpdate.oldPitch() == rotationUpdate.newPitch()) {
            flag();
        }
    }
}
