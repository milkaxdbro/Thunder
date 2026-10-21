package ac.thunder.thunder.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.impl.verbose.VerboseCodecs;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "PacketOrderO", stableKey = "thunder.packetorder.tick_end_order", description = "Пакеты до конца тика клиента", experimental = true)
public class PacketOrderO extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("type={packet}");

    public PacketOrderO(final ThunderPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.supportsEndTick();
    }

    private boolean flying;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            flying = false;
        }

        if (isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            flying = true;
            return;
        }

        if (flying && !isAsync(event.getPacketType()) && event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.inVehicle() && event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
                if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                    return;
                }
            }

            int packetId = VerboseCodecs.packet(event.getPacketType(), player.getClientVersion());
            flag(V.write(verbose()).sint(packetId));
        }
    }
}
