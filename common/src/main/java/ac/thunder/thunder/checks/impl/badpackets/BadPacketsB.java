package ac.thunder.thunder.checks.impl.badpackets;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "BadPacketsB", stableKey = "thunder.badpackets.ignored_rotation", description = "Проигнорирован пакет установки ротации")
public class BadPacketsB extends Check {
    public BadPacketsB(final ThunderPlayer player) {
        super(player);
    }
}
