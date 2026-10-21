package ac.thunder.thunder.platform.fabric.player;

import ac.thunder.thunder.platform.api.player.OfflinePlatformPlayer;
import ac.thunder.thunder.platform.fabric.AbstractThunderFabricEntryPoint;
import ac.thunder.thunder.platform.fabric.inject.FabricMinecraftServerHandle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class FabricOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final @NotNull UUID uniqueId;
    private final @NotNull String name;

    @Override
    public boolean isOnline() {
        FabricMinecraftServerHandle server = AbstractThunderFabricEntryPoint.serverOrNull();
        return server != null && server.isPlayerOnline(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflinePlatformPlayer player && this.getUniqueId().equals(player.getUniqueId());
    }
}
