package ac.thunder.thunder.checks.type;

import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketSendListener {
    void onPacketSend(PacketSendEvent event);
}
