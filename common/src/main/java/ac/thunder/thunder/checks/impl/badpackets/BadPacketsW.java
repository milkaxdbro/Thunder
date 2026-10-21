package ac.thunder.thunder.checks.impl.badpackets;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "BadPacketsW", stableKey = "thunder.badpackets.invalid_entity_target", description = "Взаимодействие с несуществующей сущностью", experimental = true)
public class BadPacketsW extends Check {
    public BadPacketsW(ThunderPlayer player) {
        super(player);
    }
}
