package ac.thunder.thunder.platform.fabric.mixins;

import ac.thunder.thunder.platform.api.world.PlatformChunk;
import ac.thunder.thunder.platform.api.world.PlatformWorld;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.utils.world.FabricIntermediaryLevelChunkUtil;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(Level.class)
@Implements(@Interface(iface = PlatformWorld.class, prefix = "thunder$"))
abstract class FabricIntermediaryLevelMixin implements LevelAccessor {

    @Shadow
    public abstract ResourceKey<Level> dimension();

    public boolean thunder$isChunkLoaded(int chunkX, int chunkZ) {
        return FabricIntermediaryLevelChunkUtil.hasChunkAt((Level) (Object) this, chunkX, chunkZ);
    }

    public WrappedBlockState thunder$getBlockAt(int x, int y, int z) {
        return WrappedBlockState.getByGlobalId(
                Block.getId(getBlockState(new BlockPos(x, y, z)))
        );
    }

    public String thunder$getName() {
        return this.dimension().location().toString();
    }

    public @Nullable UUID thunder$getUID() {
        return null;
    }

    public PlatformChunk thunder$getChunkAt(int currChunkX, int currChunkZ) {
        return (PlatformChunk) getChunk(currChunkX, currChunkZ);
    }

    public boolean thunder$isLoaded() {
        return ThunderFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getLevel(this.dimension()) != null;
    }
}
