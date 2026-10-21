package ac.thunder.thunder.checks.impl.timer;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.checks.type.PostPredictionListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "NegativeTimer", stableKey = "thunder.timer.negative", description = "Движение медленнее тикрейта", setback = -1, experimental = true)
public class NegativeTimer extends Timer implements PacketReceiveListener, PostPredictionListener {
    private static final Verbose V = Verbose.of("-{ulong}ms");

    public NegativeTimer(ThunderPlayer player) {
        super(player);
        timerBalanceRealTime = System.nanoTime() + clockDrift;
    }

    @Override
    public void onPrePredictionPacketReceive(PacketReceiveEvent event) {
        // NegativeTimer runs the inherited timer processing during normal packet dispatch.
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        super.onPrePredictionPacketReceive(event);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // We can't negative timer check a 1.9+ player who is standing still.
        if (player.uncertaintyHandler.lastPointThree.hasOccurredSince(2) || !predictionComplete.isChecked()) {
            timerBalanceRealTime = System.nanoTime() + clockDrift;
        }

        if (timerBalanceRealTime < lastMovementPlayerClock - clockDrift) {
            int lostMS = (int) ((System.nanoTime() - timerBalanceRealTime) / 1e6);
            flagWithSetback(V.write(verbose()).ulong(lostMS));
            timerBalanceRealTime += 50e6;
        }
    }

    @Override
    public void doCheck(final PacketReceiveEvent event) {
        // We don't know if the player is ticking stable, therefore we must wait until prediction
        // determines this.  Do nothing here!
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        clockDrift = (long) (config.getDoubleElse(getConfigName() + ".drift", 1200.0) * 1e6);
    }
}
