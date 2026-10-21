package ac.thunder.thunder.utils.data.packetentity;

import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.UUID;

public class PacketEntitySkeleton extends PacketEntity {
    public boolean isWitherSkeleton;

    public PacketEntitySkeleton(ThunderPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public EntityType getType() {
        return isWitherSkeleton ? EntityTypes.WITHER_SKELETON : super.getType();
    }
}
