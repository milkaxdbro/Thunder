package ac.thunder.thunder.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.impl.verbose.VerboseCodecs;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.checks.type.PacketSendListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import it.unimi.dsi.fastutil.ints.IntArrayList;

@CheckData(name = "PacketOrderP", stableKey = "thunder.packetorder.transaction_response_order", description = "Чанки в неверном порядке транзакций", experimental = true)
public class PacketOrderP extends Check implements PacketReceiveListener, PacketSendListener {
    private static final Verbose V = Verbose.of("[invalid response|skipped response, type={packet}]");

    public PacketOrderP(final ThunderPlayer player) {
        super(player);
    }

    private byte trimTimer; // let the list shrink eventually
    private final IntArrayList transactions = new IntArrayList(0);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            if (!transactions.rem(player.getLastTransactionReceived())) {
                flag(V.write(verbose()).bool(true).sint(VerboseCodecs.PACKET_NONE));
            }
        } else if (!isAsync(event.getPacketType()) && !isTransaction(event.getPacketType())) {
            if (transactions.rem(player.getLastTransactionReceived())) {
                int packetId = VerboseCodecs.packet(event.getPacketType(), player.getClientVersion());
                flag(V.write(verbose()).bool(false).sint(packetId));
            }
        }
    }

    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_BATCH_END) {
            boolean sendingBundlePacket = player.packetStateData.sendingBundlePacket;
            if (!sendingBundlePacket) player.user.sendPacket(new WrapperPlayServerBundle());

            player.sendTransaction();
            int transaction = player.getLastTransactionSent();
            transactions.add(transaction);
            if (++trimTimer == 0) transactions.trim();
            player.addRealTimeTaskNext(() -> {
                if (transactions.rem(transaction)) {
                    flag(V.write(verbose()).bool(false).sint(VerboseCodecs.PACKET_TRANSACTION));
                }
            });

            if (!sendingBundlePacket) {
                event.getTasksAfterSend().add(() -> player.user.sendPacket(new WrapperPlayServerBundle()));
            }
        }
    }
}
