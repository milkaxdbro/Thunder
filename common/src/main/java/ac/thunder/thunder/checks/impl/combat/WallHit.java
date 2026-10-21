package ac.thunder.thunder.checks.impl.combat;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketCheck;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "WallHit", stableKey = "thunder.combat.wall_hit", configName = "WallHit", setback = 20)
public class WallHit extends Check implements PacketCheck {
    public WallHit(ThunderPlayer player) {
        super(player);
    }
}
