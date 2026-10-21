package ac.thunder.thunder.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.math.ThunderMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsV", stableKey = "thunder.badpackets.slow_move", description = "Слишком маленькое смещение", experimental = true)
public class BadPacketsV extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("delta={f64}");

    private int noReminderTicks;

    public BadPacketsV(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.canSkipTicks() && isTickPacket(event.getPacketType())) {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                int positionAtLeastEveryNTicks = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;

                if (noReminderTicks < positionAtLeastEveryNTicks && !player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(1)) {
                    final Vector3d position = new WrapperPlayClientPlayerFlying(event).getLocation().getPosition();
                    final double deltaSq = ThunderMath.square(player.lastX - position.x)
                            + ThunderMath.square(player.lastY - position.y)
                            + ThunderMath.square(player.lastZ - position.z);
                    if (deltaSq <= player.getMovementThreshold() * player.getMovementThreshold()) {
                        double delta = Math.sqrt(deltaSq);
                        flag(V.write(verbose()).f64(delta));
                    }
                }

                noReminderTicks = 0;
            } else {
                noReminderTicks++;
            }
        }
    }
}
