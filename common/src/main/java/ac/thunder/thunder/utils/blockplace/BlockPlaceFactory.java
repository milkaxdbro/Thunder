package ac.thunder.thunder.utils.blockplace;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(ThunderPlayer player, BlockPlace place);
}
