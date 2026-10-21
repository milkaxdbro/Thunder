package ac.thunder.thunder.checks.impl.vehicle;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PreViaPacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "VehicleB", stableKey = "thunder.vehicle.spoofed_vehicle", description = "Яковы в транспорте, но без него")
public class VehicleB extends Check implements PreViaPacketReceiveListener {
    public VehicleB(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE && !player.inVehicle()
                && flag() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
