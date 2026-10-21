package ac.thunder.thunder.platform.bukkit.initables;

import ac.thunder.thunder.manager.init.OptionalReflectiveInitable;
import org.bukkit.Bukkit;

public final class BukkitLuckPermsInitable extends OptionalReflectiveInitable {
    private static final String HANDLER_CLASS =
            "ac.thunder.thunder.platform.bukkit.initables.BukkitLuckPermsHandler";

    public BukkitLuckPermsInitable() {
        super(HANDLER_CLASS, "Error when initializing LuckPerms hook");
    }

    @Override
    protected boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }
}
