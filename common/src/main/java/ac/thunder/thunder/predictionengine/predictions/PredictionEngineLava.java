package ac.thunder.thunder.predictionengine.predictions;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.data.VectorData;
import ac.thunder.thunder.utils.enums.FluidTag;
import ac.thunder.thunder.utils.math.ThunderMath;
import ac.thunder.thunder.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {
    @Override
    public void addJumpsToPossibilities(ThunderPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.couldSkipTick && vector.isZeroPointZeroThree()) {
                double extraVelFromVertTickSkipUpwards = ThunderMath.clamp(player.actualMovement.getY(), vector.vector.clone().getY(), vector.vector.clone().getY() + 0.05f);
                existingVelocities.add(new VectorData(vector.vector.clone().setY(extraVelFromVertTickSkipUpwards), vector, VectorData.VectorType.Jump));
            } else {
                existingVelocities.add(new VectorData(vector.vector.clone().add(0, 0.04f, 0), vector, VectorData.VectorType.Jump));
            }

            if (player.getFluidHeight(FluidTag.LAVA) <= 0.4D && player.lastOnGround && !player.onGround) {
                Vector3dm withJump = vector.vector.clone();
                super.doJump(player, withJump);
                existingVelocities.add(new VectorData(withJump, vector, VectorData.VectorType.Jump));
            }
        }
    }
}
