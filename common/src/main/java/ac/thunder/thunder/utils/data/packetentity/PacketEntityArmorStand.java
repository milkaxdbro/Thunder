package ac.thunder.thunder.utils.data.packetentity;

import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityArmorStand extends PacketEntity {

    public boolean isMarker = false;

    public PacketEntityArmorStand(ThunderPlayer player, UUID uuid, EntityType type, double x, double y, double z, int extraData) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public boolean canHit() {
        return !isMarker && super.canHit();
    }
}
