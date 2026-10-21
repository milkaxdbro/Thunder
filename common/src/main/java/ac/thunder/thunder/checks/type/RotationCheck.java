package ac.thunder.thunder.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.thunder.thunder.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends AbstractCheck {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
