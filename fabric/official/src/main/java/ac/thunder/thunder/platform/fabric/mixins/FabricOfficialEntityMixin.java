package ac.thunder.thunder.platform.fabric.mixins;

import ac.thunder.thunder.platform.fabric.inject.FabricEntityHandle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(Entity.class)
@Implements(@Interface(iface = FabricEntityHandle.class, prefix = "thunder$"))
abstract class FabricOfficialEntityMixin {

    public UUID thunder$fabricEntityUuid() {
        return ((Entity) (Object) this).getUUID();
    }

    public boolean thunder$fabricEjectPassengers() {
        Entity entity = (Entity) (Object) this;
        if (entity.isVehicle()) {
            entity.ejectPassengers();
            return true;
        }
        return false;
    }

    public Object thunder$fabricWorld() {
        return ((Entity) (Object) this).level;
    }

    public double thunder$fabricPosX() {
        return ((Entity) (Object) this).getX();
    }

    public double thunder$fabricPosY() {
        return ((Entity) (Object) this).getY();
    }

    public double thunder$fabricPosZ() {
        return ((Entity) (Object) this).getZ();
    }

    public float thunder$fabricYaw(float partialTick) {
        return ((Entity) (Object) this).getViewYRot(partialTick);
    }

    public float thunder$fabricPitch(float partialTick) {
        return ((Entity) (Object) this).getViewXRot(partialTick);
    }
}
