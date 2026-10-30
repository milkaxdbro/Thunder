package ac.thunder.thunder.platform.bukkit.player;

import ac.thunder.thunder.platform.api.entity.ThunderEntity;
import ac.thunder.thunder.platform.api.player.BlockTranslator;
import ac.thunder.thunder.platform.api.player.PlatformInventory;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.platform.bukkit.ThunderBukkitLoaderPlugin;
import ac.thunder.thunder.platform.bukkit.entity.BukkitThunderEntity;
import ac.thunder.thunder.platform.bukkit.events.KickPickupLock;
import ac.thunder.thunder.platform.bukkit.utils.anticheat.MultiLibUtil;
import ac.thunder.thunder.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.thunder.thunder.platform.bukkit.utils.reflection.PaperUtils;
import ac.thunder.thunder.utils.common.arguments.CommonThunderArguments;
import ac.thunder.thunder.utils.math.Location;
import ac.thunder.thunder.utils.reflection.ReflectionUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BukkitPlatformPlayer extends BukkitThunderEntity implements PlatformPlayer {

    private static final BukkitAudiences audiences = BukkitAudiences.create(ThunderBukkitLoaderPlugin.LOADER);
    private static final Consumer<@NotNull Player> resyncSharedFlags;

    @Getter
    private final Player bukkitPlayer;
    @Getter
    private final PlatformInventory inventory;

    private final @Nullable User user;

    public BukkitPlatformPlayer(@NotNull Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
        this.inventory = new BukkitPlatformInventory(bukkitPlayer);
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value()) {
            this.user = PacketEvents.getAPI().getPlayerManager().getUser(bukkitPlayer);
        } else {
            this.user = null;
        }
    }

    @Override
    public void kickPlayer(String textReason) {
        bukkitPlayer.kickPlayer(textReason);
    }

    @Override
    public void punishEffects() {
        // Theatrical [kick]: lift on levitation, dragon growl, two small
        // one-shot particle bursts (FPS-friendly), drop the whole inventory
        // on the ground. Best-effort cosmetics — failures must not break
        // the punishment flow, the kick itself is scheduled by the caller.
        // The punished player must not pick the dropped items back up during
        // the ~3s window before the actual kick.
        KickPickupLock.lockForKick(bukkitPlayer.getUniqueId());
        try {
            bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1, false, false));
        } catch (Throwable ignored) {
            // pre-1.9 servers have no levitation — skip the lift
        }
        try {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        } catch (Throwable ignored) {
        }
        try {
            World world = bukkitPlayer.getWorld();
            org.bukkit.Location at = bukkitPlayer.getLocation().add(0.0, 1.0, 0.0);
            world.spawnParticle(Particle.DRAGON_BREATH, at, 12, 0.4, 0.5, 0.4, 0.02);
        } catch (Throwable ignored) {
        }
        try {
            World world = bukkitPlayer.getWorld();
            org.bukkit.Location at = bukkitPlayer.getLocation().add(0.0, 1.0, 0.0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 10, 0.4, 0.5, 0.4, 0.02);
        } catch (Throwable ignored) {
            // pre-1.16 servers have no soul fire flame — dragon breath above is enough
        }
        try {
            PlayerInventory inv = bukkitPlayer.getInventory();
            java.util.Set<ItemStack> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            java.util.List<ItemStack> all = new java.util.ArrayList<>();
            ItemStack[] contents = inv.getContents();
            if (contents != null) java.util.Collections.addAll(all, contents);
            ItemStack[] armor = inv.getArmorContents();
            if (armor != null) java.util.Collections.addAll(all, armor);
            try {
                all.add(inv.getItemInOffHand());
            } catch (Throwable ignored) {
                // pre-1.9 servers have no off hand
            }
            org.bukkit.Location ground = bukkitPlayer.getLocation();
            World world = bukkitPlayer.getWorld();
            for (ItemStack item : all) {
                if (item != null && item.getType() != Material.AIR && seen.add(item)) {
                    world.dropItemNaturally(ground, item);
                }
            }
            inv.clear();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean hasPermission(String s) {
        return bukkitPlayer.hasPermission(s);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return this.bukkitPlayer.hasPermission(new Permission(s, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    public void resyncSharedFlags() {
        resyncSharedFlags.accept(bukkitPlayer);
    }

    @Override
    public void sendMessage(String message) {
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            bukkitPlayer.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonThunderArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else if (Audience.class.isInstance(bukkitPlayer)) {
            Audience.class.cast(bukkitPlayer).sendMessage(message);
        } else {
            audiences.player(bukkitPlayer).sendMessage(message);
        }
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void updateInventory() {
        bukkitPlayer.updateInventory();
    }

    @Override
    public Vector3d getPosition() {
        if (CAN_USE_DIRECT_GETTERS) {
            return new Vector3d(this.bukkitPlayer.getX(), this.bukkitPlayer.getY(), this.bukkitPlayer.getZ());
        } else {
            org.bukkit.Location location = this.bukkitPlayer.getLocation();
            return new Vector3d(location.getX(), location.getY(), location.getZ());
        }
    }

    @Override
    public @Nullable ThunderEntity getVehicle() {
        return bukkitPlayer.getVehicle() == null ? null : new BukkitThunderEntity(bukkitPlayer.getVehicle());
    }

    @Override
    public GameMode getGameMode() {
        return SpigotConversionUtil.fromBukkitGameMode(bukkitPlayer.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        bukkitPlayer.setGameMode(SpigotConversionUtil.toBukkitGameMode(gameMode));
    }

    public World getBukkitWorld() {
        return bukkitPlayer.getWorld();
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean eject() {
        return bukkitPlayer.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        this.bukkitPlayer.sendPluginMessage(ThunderBukkitLoaderPlugin.LOADER, channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return ThunderBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(this.bukkitPlayer);
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return BlockTranslator.IDENTITY;
    }

    @Override
    @NotNull
    public Player getNative() {
        return this.bukkitPlayer;
    }

    static {
        final ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        try {
            final String nmsPackage;

            Class<?> CraftEntity = ReflectionUtils.getClass("org.bukkit.craftbukkit.entity.CraftEntity");
            if (CraftEntity != null) {
                nmsPackage = null;
            } else {
                nmsPackage = Bukkit.getServer().getClass().getPackageName().split("\\.")[3];
                CraftEntity = Class.forName("org.bukkit.craftbukkit." + nmsPackage + ".entity.CraftEntity");
            }

            final Method getHandle = CraftEntity.getMethod("getHandle");
            final Class<?> Entity = getHandle.getReturnType();

            final Method getDataWatcher = Entity.getMethod(nmsPackage != null ? switch (nmsPackage) {
                case "v1_8_R3", "v1_9_R1", "v1_9_R2", "v1_10_R1",
                     "v1_11_R1", "v1_12_R1", "v1_13_R1", "v1_13_R2",
                     "v1_14_R1", "v1_15_R1", "v1_16_R1", "v1_16_R2",
                     "v1_16_R3", "v1_17_R1" -> "getDataWatcher";
                case "v1_18_R1", "v1_18_R2", "v1_19_R1" -> "ai";
                case "v1_19_R2", "v1_20_R2" -> "al";
                case "v1_19_R3", "v1_20_R1" -> "aj";
                case "v1_20_R3" -> "an";
                case "v1_20_R4" -> "ap";
                case "v1_21_R1", "v1_21_R4" -> "ar";
                case "v1_21_R2", "v1_21_R3", "v1_21_R5" -> "au";
                case "v1_21_R6" -> "aC";
                case "v1_21_R7" -> "aD";
                default -> throw new IllegalStateException("You are using an unsupported server version: " + nmsPackage + "/" + version.getReleaseName());
            } : "getEntityData");

            final Class<?> DataWatcher = getDataWatcher.getReturnType();

            final Method markDirty;
            final Object sharedFlagsId;

            if (version.isOlderThan(ServerVersion.V_1_9)) {
                markDirty = DataWatcher.getMethod("update", int.class);
                sharedFlagsId = 0;
            } else {
                Field field = Entity.getDeclaredField(nmsPackage != null ? switch (nmsPackage) {
                    case "v1_9_R1" -> "ax";
                    case "v1_9_R2" -> "ay";
                    case "v1_10_R1", "v1_18_R1" -> "aa";
                    case "v1_11_R1", "v1_12_R1", "v1_17_R1",
                         "v1_18_R2", "v1_19_R1", "v1_19_R2" -> "Z";
                    case "v1_13_R1", "v1_13_R2" -> "ac";
                    case "v1_14_R1" -> "W";
                    case "v1_15_R1", "v1_16_R1" -> "T";
                    case "v1_16_R2", "v1_16_R3" -> "S";
                    case "v1_19_R3", "v1_20_R1" -> "an";
                    case "v1_20_R2", "v1_20_R3" -> "ao";
                    case "v1_20_R4", "v1_21_R1" -> "ap";
                    case "v1_21_R2", "v1_21_R3", "v1_21_R4" -> "am";
                    case "v1_21_R5" -> "az";
                    case "v1_21_R6", "v1_21_R7" -> "aA";
                    default -> throw new IllegalStateException("You are using an unsupported server version: " + nmsPackage + "/" + version.getReleaseName());
                } : "DATA_SHARED_FLAGS_ID");
                field.setAccessible(true);

                markDirty = DataWatcher.getMethod("markDirty", field.getType());
                sharedFlagsId = field.get(null);
            }

            resyncSharedFlags = player -> {
                try {
                    Object handle = getHandle.invoke(player);
                    Object dataWatcher = getDataWatcher.invoke(handle);
                    markDirty.invoke(dataWatcher, sharedFlagsId);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Throwable t) {
            throw t instanceof RuntimeException e ? e : new RuntimeException(t);
        }
    }
}
