package ac.thunder.thunder.checks.impl.inventory;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.InventoryCheck;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.update.BlockPlace;

@CheckData(name = "InventoryC", stableKey = "thunder.inventory.place_open", setback = 3, description = "Установка блока с открытым инвентарем")
public class InventoryC extends InventoryCheck {

    public InventoryC(ThunderPlayer player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
        // It is not possible to place a block while the inventory is open
        if (player.hasInventoryOpen) {
            if (flag()) {
                if (shouldModifyPackets()) {
                    place.resync();
                }
                if (!isNoSetbackPermission()) {
                    closeInventory();
                }
            }
        } else {
            reward();
        }
    }
}
