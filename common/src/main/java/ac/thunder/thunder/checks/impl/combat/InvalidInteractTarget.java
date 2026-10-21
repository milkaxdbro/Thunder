package ac.thunder.thunder.checks.impl.combat;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "InvalidInteractTarget", stableKey = "thunder.badpackets.invalid_entity_target", description = "Взаимодействие с несуществующей сущностью", experimental = true)
public class InvalidInteractTarget extends Check {
    public InvalidInteractTarget(ThunderPlayer player) {
        super(player);
    }
}
