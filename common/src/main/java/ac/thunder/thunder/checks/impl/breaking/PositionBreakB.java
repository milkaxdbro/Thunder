package ac.thunder.thunder.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.impl.verbose.VerboseCodecs;
import ac.thunder.thunder.checks.type.BlockBreakListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "PositionBreakB", stableKey = "thunder.breaking.position_break_b", description = "Отмена ломания с неверной гранью")
public class PositionBreakB extends Check implements BlockBreakListener {
    private static final Verbose V = Verbose.of("lastFace={face}, action={digging}");

    private final boolean allowLegacyFace = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10);
    private BlockFace lastFace;

    public PositionBreakB(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            flag(V.write(verbose())
                    .uint(VerboseCodecs.enumId(lastFace))
                    .uint(VerboseCodecs.enumId(blockBreak.action)));
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            // as of https://github.com/ViaVersion/ViaRewind/commit/e7b0606e187afbccf98ef7c88d3f3af27fe11da3,
            // ViaRewind maps face 255 for 1.7 clients to 0. Let's allow both, just to be safe
            lastFace = blockBreak.faceId == 0 || allowLegacyFace && blockBreak.faceId == 255 ? null : blockBreak.face;
        }
    }
}
