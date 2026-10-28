package ac.thunder.thunder.checks.impl.combat;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.ArrayDeque;

/**
 * Catching "ElytraAura / ElytraTarget" — continuous attacks while gliding.
 * A vanilla client practically cannot hit a specific entity 6 times in 4 seconds while gliding (the elytra
 * requires constantly tracking the direction of flight, and each hit changes the trajectory).
 * Riptide tridents (SPIN_ATTACK pose) are excluded - it is a legit attack mechanic in a dash.
 */
@CheckData(name = "ElytraTarget", stableKey = "thunder.combat.elytra_target", description = "Атака сущностей во время полёта на элитре", experimental = true)
public class ElytraTarget extends Check implements PacketReceiveListener {
    private static final Verbose V =
            Verbose.of("target={sint}, hits={sint}, window={ulong}ms");

    // window and threshold: bursts of hits while gliding that vanilla can't produce
    private static final long WINDOW_MS = 4000L;
    private static final int HITS_REQUIRED = 6;

    private final ArrayDeque<Long> glidingAttacks = new ArrayDeque<>();

    public ElytraTarget(final ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final int entityId;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
            entityId = packet.getEntityId();
        } else {
            return;
        }

        final long now = System.currentTimeMillis();
        while (!glidingAttacks.isEmpty() && now - glidingAttacks.peekFirst() > WINDOW_MS) {
            glidingAttacks.pollFirst();
        }

        // Riptide tridents are a legitimate dash attack, never flag them
        if (player.isRiptidePose) return;
        // Attacking yourself is SelfInteract's job
        if (entityId == player.entityID) return;
        // Unknown (untracked) entity - can't confirm it's a real hit on something alive
        PacketEntity target = player.compensatedEntities.getEntity(entityId);
        if (target == null) return;

        if (player.isGliding) {
            glidingAttacks.addLast(now);

            if (glidingAttacks.size() >= HITS_REQUIRED) {
                glidingAttacks.clear(); // require a new burst to flag again
                if (flag(V.write(verbose()).sint(entityId).sint(HITS_REQUIRED).ulong(WINDOW_MS))
                        && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
