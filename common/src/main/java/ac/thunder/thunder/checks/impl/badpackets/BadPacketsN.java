package ac.thunder.thunder.checks.impl.badpackets;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "BadPacketsN", stableKey = "thunder.badpackets.invalid_teleport", description = "Не принят обязательный телепорт сервера")
public class BadPacketsN extends Check {
    public BadPacketsN(final ThunderPlayer player) {
        super(player);
    }
}
