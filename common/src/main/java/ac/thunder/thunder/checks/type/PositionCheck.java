package ac.thunder.thunder.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.thunder.thunder.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
