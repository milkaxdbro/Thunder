package ac.thunder.thunder.checks.impl.sprint;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import ac.thunder.thunder.utils.enums.Pose;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.Collections;

@CheckData(name = "SprintB", stableKey = "thunder.sprint.sneaking", description = "Спринт крадучись или ползком", setback = 5, experimental = true)
public class SprintB extends Check implements PostPredictionListener {
    public SprintB(ThunderPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        // https://bugs.mojang.com/browse/MC-152728
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_2) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_4);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.isSlowMovement && player.sneakingSpeedMultiplier < 0.8f && predictionComplete.isChecked()) {
            ClientVersion version = player.getClientVersion();

            // https://github.com/GrimAnticheat/Grim/issues/1932
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14) && player.wasFlying && player.lastPose == Pose.FALL_FLYING && !player.isGliding) {
                return;
            }

            // https://github.com/GrimAnticheat/Grim/issues/1948
            if (version.isNewerThanOrEquals(ClientVersion.V_1_21_4) && (Collections.max(player.uncertaintyHandler.pistonX) != 0
                    || Collections.max(player.uncertaintyHandler.pistonY) != 0
                    || Collections.max(player.uncertaintyHandler.pistonZ) != 0)) {
                return;
            }

            if (!player.wasTouchingWater || version.isOlderThan(ClientVersion.V_1_13)) {
                return;
            }

            if (player.isSprinting) {
                flagWithSetback();
            } else reward();
        }
    }
}
