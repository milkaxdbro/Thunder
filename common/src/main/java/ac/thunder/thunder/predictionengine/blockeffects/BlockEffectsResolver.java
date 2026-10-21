package ac.thunder.thunder.predictionengine.blockeffects;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.math.Vector3dm;

import java.util.List;

public interface BlockEffectsResolver {

    void applyEffectsFromBlocks(ThunderPlayer player, Vector3dm clientVelocity, boolean onlyApplyVelocity, List<ThunderPlayer.Movement> movements);

}
