package ac.thunder.thunder.checks.type;

import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;

public interface PostPredictionListener {
    void onPredictionComplete(PredictionComplete predictionComplete);
}
