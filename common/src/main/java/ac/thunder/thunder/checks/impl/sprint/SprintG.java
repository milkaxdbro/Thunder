package ac.thunder.thunder.checks.impl.sprint;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintG", stableKey = "thunder.sprint.water", description = "Спринт в воде", experimental = true)
public class SprintG extends Check implements PostPredictionListener {
    public SprintG(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.wasTouchingWater && (player.wasWasTouchingWater || player.getClientVersion() == ClientVersion.V_1_21_4)
                && !player.wasEyeInWater && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)
                && player.wasLastPredictionCompleteChecked && predictionComplete.isChecked()
                && !EntityTypes.isTypeInstanceOf(player.getVehicleType(), EntityTypes.CAMEL)
                && !player.isSwimming) {
            if (player.isSprinting) {
                flagWithSetback();
            } else {
                reward();
            }
        }
    }
}
