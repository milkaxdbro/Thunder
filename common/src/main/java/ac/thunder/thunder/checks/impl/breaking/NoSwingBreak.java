package ac.thunder.thunder.checks.impl.breaking;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.BlockBreakListener;
import ac.thunder.thunder.checks.type.PreViaPacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "NoSwingBreak", stableKey = "thunder.breaking.no_swing_break", description = "Ломание блока без взмаха", experimental = true)
public class NoSwingBreak extends Check implements BlockBreakListener, PreViaPacketReceiveListener {
    private boolean sentAnimation;
    private boolean sentBreak;
    private boolean exempt;

    public NoSwingBreak(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.CANCELLED_DIGGING) {
            sentBreak = true;
        }
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (isAnimation(event.getPacketType())) {
            sentAnimation = true;
        }

        if (isTickPacket(event.getPacketType())) {
            if (sentBreak && !sentAnimation) {
                // Exempt for a tick on teleport - the client can skip the animation packet
                // right after a teleport while breaking (netcode desync)
                if (player.packetStateData.lastPacketWasTeleport) {
                    exempt = true;
                } else if (exempt) {
                    exempt = false;
                } else {
                    flag();
                }
            }

            sentAnimation = sentBreak = false;
        }
    }
}
