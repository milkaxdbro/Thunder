package ac.thunder.thunder.predictionengine.predictions.rideable;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.predictionengine.predictions.PredictionEngineWater;
import ac.thunder.thunder.predictionengine.predictions.input.Input;
import ac.thunder.thunder.utils.data.VectorData;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableWater extends PredictionEngineWater {
    protected final Input movementVector;

    @Override
    public void addJumpsToPossibilities(ThunderPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(ThunderPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }

}
