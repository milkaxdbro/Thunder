package ac.thunder.thunder.platform.fabric.player;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.api.entity.ThunderEntity;
import ac.thunder.thunder.platform.api.player.BlockTranslator;
import ac.thunder.thunder.platform.api.player.PlatformInventory;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.fabric.FabricPlatformServices;
import ac.thunder.thunder.platform.fabric.entity.AbstractFabricThunderEntity;
import ac.thunder.thunder.platform.fabric.inject.FabricServerPlayerHandle;
import ac.thunder.thunder.utils.common.arguments.CommonThunderArguments;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFabricPlatformPlayer<T> extends AbstractFabricThunderEntity<T> implements PlatformPlayer {
    protected final AbstractFabricPlatformInventory inventory;
    private final @Nullable User user;
    private final BlockTranslator blockTranslator;

    public AbstractFabricPlatformPlayer(T player) {
        super(player);
        this.inventory = FabricPlatformServices.createInventory(this);
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value()) {
            Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(handle().uuid());
            this.user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        } else {
            this.user = null;
        }
        this.blockTranslator = FabricPlatformServices.createBlockTranslator(player);
    }

    protected T serverPlayer() {
        return this.entity;
    }

    protected FabricServerPlayerHandle handle() {
        return (FabricServerPlayerHandle) this.entity;
    }

    @Override
    public void resyncSharedFlags() {
        handle().resyncSharedFlags();
    }

    @Override
    public boolean hasPermission(String permission) {
        return getSender().hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, boolean defaultIfUnset) {
        return getSender().hasPermission(permission, defaultIfUnset);
    }

    @Override
    public void sendMessage(String message) {
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            handle().sendSystemText(FabricPlatformServices.textLiteral(message));
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            Object nativeText = FabricPlatformServices.conversionUtil().toNativeText(message);
            handle().sendSystemText(nativeText);
        }
    }

    @Override
    public boolean isOnline() {
        return !handle().isDisconnected();
    }

    @Override
    public void punishEffects() {
        // Theatrical [kick] without any NMS calls so it works on every
        // supported version: levitation + dragon growl + two small one-shot
        // particle bursts (FPS-friendly), then every stack is re-spawned as
        // a dropped item entity and the inventory is cleared.
        // Best-effort cosmetics — failures must not break the punishment
        // flow, the kick itself is scheduled by the caller.
        String name;
        try {
            name = handle().usernameString();
        } catch (Throwable ignored) {
            return;
        }
        if (name == null || name.isEmpty()) return;
        dispatchKickCommand("effect give " + name + " minecraft:levitation 3 1");
        dispatchKickCommand("execute as " + name + " at @s run playsound minecraft:entity.ender_dragon.growl master @s ~ ~ ~ 1 1");
        dispatchKickCommand("execute as " + name + " at @s run particle minecraft:dragon_breath ~ ~1 ~ 0.4 0.5 0.4 0.02 12 force");
        dispatchKickCommand("execute as " + name + " at @s run particle minecraft:soul_fire_flame ~ ~1 ~ 0.4 0.5 0.4 0.02 10 force");
        try {
            for (ItemStack stack : getInventory().getContents()) {
                if (stack == null || stack.isEmpty()) continue;
                String id;
                try {
                    id = String.valueOf(stack.getType().getName());
                } catch (Throwable ignored) {
                    continue;
                }
                if (id == null || id.isEmpty()) continue;
                int left = stack.getAmount();
                while (left > 0) {
                    int n = Math.min(left, 64);
                    left -= n;
                    dispatchKickCommand("execute as " + name + " at @s run summon minecraft:item ~ ~1 ~ {Item:{id:\"" + id + "\",Count:" + n + "b}}");
                }
            }
        } catch (Throwable ignored) {
        }
        dispatchKickCommand("clear " + name);
    }

    private static void dispatchKickCommand(String command) {
        try {
            ThunderAPI.INSTANCE.getPlatformServer().dispatchCommand(
                    ThunderAPI.INSTANCE.getPlatformServer().getConsoleSender(), command);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getName() {
        return handle().usernameString();
    }

    @Override
    public void updateInventory() {
        handle().broadcastInventoryChanges();
    }

    @Override
    public Vector3d getPosition() {
        FabricServerPlayerHandle handle = handle();
        return new Vector3d(handle.posX(), handle.posY(), handle.posZ());
    }

    @Override
    public PlatformInventory getInventory() {
        return inventory;
    }

    @Override
    public ThunderEntity getVehicle() {
        Object vehicle = handle().vehicleEntity();
        return vehicle != null ? FabricPlatformServices.createEntity(vehicle) : null;
    }

    @Override
    public GameMode getGameMode() {
        return FabricPlatformServices.conversionUtil().fromNativeGameMode(handle().gameMode());
    }

    @Override
    public java.util.UUID getUniqueId() {
        return handle().uuid();
    }

    @Override
    public boolean isExternalPlayer() {
        return false;
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return blockTranslator;
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(handle().uuid());
        User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        if (user != null) {
            user.sendPacket(new WrapperPlayServerPluginMessage(pluginMessageChannel(channelName), byteArray));
        }
    }

    private static String pluginMessageChannel(String channelName) {
        return channelName.equals("BungeeCord") ? "bungeecord:main" : channelName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceNativePlayer(Object nativePlayerObject) {
        setNativeEntity((T) nativePlayerObject);
    }

    @Override
    public boolean isDead() {
        return handle().isDead();
    }
}
