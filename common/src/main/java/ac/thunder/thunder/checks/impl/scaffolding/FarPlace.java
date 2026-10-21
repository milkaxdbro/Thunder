package ac.thunder.thunder.checks.impl.scaffolding;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.BlockPlaceCheck;
import ac.thunder.thunder.checks.type.BlockPlaceListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;
import ac.thunder.thunder.utils.collisions.datatypes.SimpleCollisionBox;
import ac.thunder.thunder.utils.math.Vector3dm;
import ac.thunder.thunder.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "FarPlace", stableKey = "thunder.scaffolding.far_place", description = "Стройка со слишком большой дистанции")
public class FarPlace extends BlockPlaceCheck implements BlockPlaceListener {
    public FarPlace(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!player.cameraEntity.isSelf() || player.inVehicle()) return;

        Vector3i blockPos = place.position;

        if (place.material == StateTypes.SCAFFOLDING) return;

        double min = Double.MAX_VALUE;
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (double d : possibleEyeHeights) {
            SimpleCollisionBox box = new SimpleCollisionBox(blockPos);
            Vector3dm best = VectorUtils.cutBoxToVector(player.x, player.y + d, player.z, box);
            min = Math.min(min, best.distanceSquared(player.x, player.y + d, player.z));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double threshold = player.getMovementThreshold();
        maxReach += Math.hypot(threshold, threshold);

        if (min > maxReach * maxReach) { // fail
            if (flag() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
