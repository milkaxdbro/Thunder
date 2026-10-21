package ac.thunder.thunder.checks.impl.vehicle;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "VehicleC", stableKey = "thunder.vehicle.vehicle_control", description = "Транспорт не совпал с предсказанием")
public class VehicleC extends Check {
    public VehicleC(ThunderPlayer player) {
        super(player);
    }
}
