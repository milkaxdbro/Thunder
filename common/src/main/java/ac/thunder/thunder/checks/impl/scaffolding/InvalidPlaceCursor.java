package ac.thunder.thunder.checks.impl.scaffolding;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.BlockPlaceCheck;
import ac.thunder.thunder.checks.type.BlockPlaceListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "InvalidPlaceCursor", stableKey = "thunder.scaffolding.invalid_place_a", description = "Неверная позиция курсора")
public class InvalidPlaceCursor extends BlockPlaceCheck implements BlockPlaceListener {
    public InvalidPlaceCursor(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;
        if (!Float.isFinite(cursor.x) || !Float.isFinite(cursor.y) || !Float.isFinite(cursor.z)) {
            if (flag() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
