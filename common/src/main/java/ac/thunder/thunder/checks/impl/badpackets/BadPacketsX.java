package ac.thunder.thunder.checks.impl.badpackets;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "BadPacketsX", stableKey = "thunder.badpackets.extra_input_actions", description = "Дувлирующийся sneak/спринт до следующего движения", experimental = true)
public class BadPacketsX extends Check implements PacketReceiveListener, PostPredictionListener {
    private boolean sprint;
    private boolean sneak;
    private int flags;

    public BadPacketsX(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (flags > 0) {
                setbackIfAboveSetbackVL();
            }

            flags = 0;
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (; flags > 0; flags--) {
                flagWithSetback();
            }
        }

        flags = 0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            sprint = sneak = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SNEAKING, STOP_SNEAKING -> {
                    if (sneak) {
                        if (player.canSkipTicks() || flag()) {
                            flags++;
                        }
                    }

                    sneak = true;
                }
                case START_SPRINTING, STOP_SPRINTING -> {
                    if (player.inVehicle()) {
                        return;
                    }

                    if (sprint) {
                        if (player.canSkipTicks() || flag()) {
                            flags++;
                        }
                    }

                    sprint = true;
                }
            }
        }
    }
}
