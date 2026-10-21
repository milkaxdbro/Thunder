package ac.thunder.thunder.checks.impl.inventory;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.InventoryCheck;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "InventoryB", stableKey = "thunder.inventory.dig_open", setback = 3, description = "Копание с открытым инвентарем")
public class InventoryB extends InventoryCheck {
    public InventoryB(ThunderPlayer player) {
        super(player);
    }

    public void handle(PacketReceiveEvent event, WrapperPlayClientPlayerDigging wrapper) {
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        // Is not possible to start digging a block while the inventory is open.
        if (player.hasInventoryOpen) {
            if (flag()) {
                // Cancel the packet
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
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
