package ac.thunder.thunder.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.BlockPlaceCheck;
import ac.thunder.thunder.checks.type.BlockPlaceListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceFace", stableKey = "thunder.scaffolding.invalid_place_b", description = "Невозможный id грани блока")
public class InvalidPlaceFace extends BlockPlaceCheck implements BlockPlaceListener {
    private static final Verbose V = Verbose.of("direction={sint}");

    public InvalidPlaceFace(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getFaceId() == 255 && PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        if (place.getFaceId() < 0 || place.getFaceId() > 5) {
            // ban
            int direction = place.getFaceId();
            if (flag(V.write(verbose()).sint(direction)) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
