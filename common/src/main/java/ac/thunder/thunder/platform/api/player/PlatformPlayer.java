package ac.thunder.thunder.platform.api.player;

import ac.thunder.thunder.platform.api.entity.ThunderEntity;
import ac.thunder.thunder.platform.api.sender.Sender;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface PlatformPlayer extends ThunderEntity, OfflinePlatformPlayer {
    void kickPlayer(String textReason);

    /**
     * Immediate, main-thread part of the theatrical {@code [kick]} punishment:
     * levitation lift, ender-dragon sound, a few light particles and dropping
     * the inventory. Best-effort cosmetics — must never throw.
     * The delayed kick itself is scheduled by the caller.
     */
    default void punishEffects() {
    }

    void resyncSharedFlags();

    boolean hasPermission(String s);

    boolean hasPermission(String s, boolean defaultIfUnset);

    void sendMessage(String message);

    void sendMessage(Component message);

    void updateInventory();

    Vector3d getPosition();

    PlatformInventory getInventory();

    @Nullable ThunderEntity getVehicle();

    GameMode getGameMode();

    void setGameMode(GameMode gameMode);

    boolean isExternalPlayer();

    void sendPluginMessage(String channelName, byte[] byteArray);

    Sender getSender();

    /*
     * Replaces native player reference in PlatformPlayer implementation with a new object
     * Vanilla MC replaces ServerPlayerEntity references on respawn and dimension change
     */
    default void replaceNativePlayer(Object nativePlayerObject) {}

    BlockTranslator getBlockTranslator();
}
