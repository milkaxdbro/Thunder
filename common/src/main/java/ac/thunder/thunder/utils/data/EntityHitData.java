package ac.thunder.thunder.utils.data;

import ac.thunder.thunder.utils.data.packetentity.PacketEntity;
import ac.thunder.thunder.utils.math.Vector3dm;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class EntityHitData extends HitData {
    private final PacketEntity entity;

    public EntityHitData(PacketEntity packetEntity) {
        this(packetEntity, new Vector3dm(packetEntity.trackedServerPosition.getPos().x,
                packetEntity.trackedServerPosition.getPos().y,
                packetEntity.trackedServerPosition.getPos().z));
    }

    public EntityHitData(PacketEntity packetEntity, Vector3dm intersectionPoint) {
        super(intersectionPoint);  // Use actual intersection point
        this.entity = packetEntity;
    }
}
