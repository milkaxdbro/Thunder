package ac.thunder.thunder.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.checks.type.PacketSendListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;

import java.util.LinkedList;

@CheckData(name = "BadPacketsO", stableKey = "thunder.badpackets.invalid_keepalive", description = "Ответ с keepalive-ID, который сервер не отправлял")
public class BadPacketsO extends Check implements PacketReceiveListener, PacketSendListener {
    private static final Verbose V = Verbose.of("id={slong}");

    private final LinkedList<Long> keepalives = new LinkedList<>();

    public BadPacketsO(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            keepalives.add(new WrapperPlayServerKeepAlive(event).getId());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            final long id = new WrapperPlayClientKeepAlive(event).getId();

            for (long keepalive : keepalives) {
                if (keepalive == id) {
                    // Found the ID, remove stuff until we get to it (to stop very slow memory leaks)
                    Long data;
                    do {
                        data = keepalives.poll();
                    } while (data != null && data != id);

                    return;
                }
            }

            if (flag(V.write(verbose()).slong(id)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
