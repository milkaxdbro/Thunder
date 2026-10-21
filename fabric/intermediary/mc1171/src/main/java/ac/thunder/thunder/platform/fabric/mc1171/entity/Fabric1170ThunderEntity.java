package ac.thunder.thunder.platform.fabric.mc1171.entity;

import ac.thunder.thunder.platform.fabric.mc1161.entity.Fabric1161ThunderEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Fabric1170ThunderEntity extends Fabric1161ThunderEntity {

    public Fabric1170ThunderEntity(Entity entity) {
        super(entity);
    }

    @Override
    public boolean isDead() {
        return this.entity instanceof LivingEntity living ? living.isDeadOrDying() : this.entity.isRemoved();
    }
}
