package ac.thunder.thunder.checks.debug;

import ac.thunder.thunder.checks.ThunderProcessor;
import ac.thunder.thunder.player.ThunderPlayer;

public abstract class AbstractDebugHandler extends ThunderProcessor {
    public AbstractDebugHandler(ThunderPlayer player) {
        super(player);
    }

    public abstract boolean toggleListener(ThunderPlayer player);

    public abstract boolean toggleConsoleOutput();
}
