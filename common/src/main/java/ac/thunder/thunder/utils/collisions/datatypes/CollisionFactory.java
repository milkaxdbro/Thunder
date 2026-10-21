package ac.thunder.thunder.utils.collisions.datatypes;

import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public interface CollisionFactory {
    CollisionBox fetch(ThunderPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
