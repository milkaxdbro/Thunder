package ac.thunder.thunder.checks.impl.crash;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "CrashE", stableKey = "thunder.crash.low_view_distance", description = "Дальность прорисовки ниже минимума")
public class CrashE extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("distance={sint}");

    public CrashE(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            int viewDistance = wrapper.getViewDistance();
            if (viewDistance < 2) {
                flag(V.write(verbose()).sint(viewDistance));
                wrapper.setViewDistance(2);
            }
        }
    }

}
