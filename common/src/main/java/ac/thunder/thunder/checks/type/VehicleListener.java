package ac.thunder.thunder.checks.type;

import ac.thunder.thunder.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleListener {
    void process(final VehiclePositionUpdate vehicleUpdate);
}
