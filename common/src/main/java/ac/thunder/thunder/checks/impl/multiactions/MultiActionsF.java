package ac.thunder.thunder.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.BlockBreakListener;
import ac.thunder.thunder.checks.type.BlockPlaceCheck;
import ac.thunder.thunder.checks.type.BlockPlaceListener;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockBreak;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiActionsF", stableKey = "thunder.multiactions.block_and_entity_interact", description = "Блок и сущность за один тик", experimental = true)
public class MultiActionsF extends BlockPlaceCheck implements PacketReceiveListener, PostPredictionListener, BlockPlaceListener, BlockBreakListener {
    // Shape index == ACTION_* constant value.
    private static final Verbose V = Verbose.of("action=place").or("action=entity").or("action=dig");

    private static final int ACTION_PLACE = 0;
    private static final int ACTION_ENTITY = 1;
    private static final int ACTION_DIG = 2;

    private final List<FlagData> flags = new ArrayList<>();
    private boolean entity, block;

    public MultiActionsF(ThunderPlayer player) {
        super(player);
    }

    private Verbose.Writer writeAction(int action) {
        return V.write(verbose(), action);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        block = true;
        if (entity) {
            if (!player.canSkipTicks()) {
                if (flag(writeAction(ACTION_PLACE)) && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.add(new FlagData(ACTION_PLACE));
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.ATTACK
                || event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            entity = true;
            if (block) {
                if (!player.canSkipTicks()) {
                    if (flag(writeAction(ACTION_ENTITY)) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(ACTION_ENTITY));
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            block = entity = false;
        }
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING || blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            block = true;
            if (entity) {
                if (!player.canSkipTicks()) {
                    if (flag(writeAction(ACTION_DIG)) && shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                } else {
                    flags.add(new FlagData(ACTION_DIG));
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flag(writeAction(data.action()));
            }
        }

        flags.clear();
    }

    private record FlagData(int action) {}
}
