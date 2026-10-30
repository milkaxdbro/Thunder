package ac.thunder.thunder.platform.bukkit.initables;

import ac.thunder.thunder.manager.init.start.StartableInitable;
import ac.thunder.thunder.platform.bukkit.ThunderBukkitLoaderPlugin;
import ac.thunder.thunder.platform.bukkit.events.CheaterDamageListener;
import ac.thunder.thunder.platform.bukkit.events.KickPickupLock;
import ac.thunder.thunder.platform.bukkit.events.PistonEvent;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Регистрация bukkit-событий... (PistonEvent, CheaterDamageListener, KickPickupLock)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), ThunderBukkitLoaderPlugin.LOADER);
        Bukkit.getPluginManager().registerEvents(new CheaterDamageListener(), ThunderBukkitLoaderPlugin.LOADER);
        Bukkit.getPluginManager().registerEvents(new KickPickupLock(), ThunderBukkitLoaderPlugin.LOADER);
    }
}
