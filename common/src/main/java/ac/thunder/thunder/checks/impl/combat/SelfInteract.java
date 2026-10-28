package ac.thunder.thunder.checks.impl.combat;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PreViaPacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity;

@CheckData(name = "SelfInteract", stableKey = "thunder.badpackets.self_hit", description = "Взаимодействие с собой")
public class SelfInteract extends Check implements PreViaPacketReceiveListener {
    public SelfInteract(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            onInteract(event, packet.getEntityId(), false);
        }

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId(), false);
        }

        if (event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            WrapperPlayClientSpectateEntity packet = new WrapperPlayClientSpectateEntity(event);
            onInteract(event, packet.getEntityId(), true);
        }
    }

    private void onInteract(PacketReceiveEvent event, int entityId, boolean spectate) {
        // Interacting/attacking yourself is impossible in vanilla (Instant ban)
        if (player.cameraEntity.isSelf() && entityId == player.entityID
                && flag() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        // While spectating an entity, the vanilla client cannot attack or interact with the
        // spectated entity (it can only send SPECTATE_ENTITY). Don't cancel in case of a
        // lagged camera transition.
        if (!spectate && !player.cameraEntity.isSelf()
                && player.cameraEntity.getPossibilities().stream()
                        .anyMatch(e -> player.compensatedEntities.getPacketEntityID(e) == entityId)
                && flag()) {
            // no packet cancel
        }
    }
}
