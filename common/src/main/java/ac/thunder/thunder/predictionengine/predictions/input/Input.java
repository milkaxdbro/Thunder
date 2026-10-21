package ac.thunder.thunder.predictionengine.predictions.input;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public interface Input {

    Vector3dm vector();

    Input normalize(ThunderPlayer player);

    static Input createInput(ThunderPlayer player, float sideways, float vertical, float forward) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new DoubleInput(sideways, vertical, forward);
        } else {
            return new FloatInput(sideways, vertical, forward);
        }
    }

}
