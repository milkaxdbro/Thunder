package ac.thunder.thunder.checks.impl.sprint;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintC", stableKey = "thunder.sprint.using_item", description = "Спринт при использовании предмета", setback = 5, experimental = true)
public class SprintC extends Check implements PostPredictionListener {
    private boolean flaggedLastTick = false;

    public SprintC(ThunderPlayer player) {
        super(player);
    }


    @Override
    public boolean isApplicable() {
        // https://bugs.mojang.com/browse/MC-152728
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_2) || player.getClientVersion() == ClientVersion.V_1_21_4;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.packetStateData.isSlowedByUsingItem()) {
            if (!player.wasTouchingWater || player.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {
                flaggedLastTick = false;
                return;
            }

            if (player.isSprinting) {
                if (flaggedLastTick) flagWithSetback();
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
    }
}
