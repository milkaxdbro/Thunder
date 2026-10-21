package ac.thunder.thunder.checks.impl.combat;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketCheck;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "EntityPierce", stableKey = "thunder.combat.entity_pierce", configName = "EntityPierce", setback = 30)
public class EntityPierce extends Check implements PacketCheck {
    public EntityPierce(ThunderPlayer player) {
        super(player);
    }
}
