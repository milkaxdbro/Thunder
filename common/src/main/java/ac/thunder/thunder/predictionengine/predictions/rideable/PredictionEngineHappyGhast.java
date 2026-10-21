package ac.thunder.thunder.predictionengine.predictions.rideable;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.predictionengine.predictions.PredictionEngineNormal;
import ac.thunder.thunder.predictionengine.predictions.input.Input;
import ac.thunder.thunder.utils.data.VectorData;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineHappyGhast extends PredictionEngineNormal {
    private final Input movementVector;
    private final double multiplier;

    @Override
    public void endOfTick(ThunderPlayer player, double delta) {
        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.setX(vector.vector.getX() * multiplier);
            vector.vector.setY(vector.vector.getY() * multiplier);
            vector.vector.setZ(vector.vector.getZ() * multiplier);
        }
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(ThunderPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }

}
