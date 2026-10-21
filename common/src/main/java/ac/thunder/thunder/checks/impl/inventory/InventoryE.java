package ac.thunder.thunder.checks.impl.inventory;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.InventoryCheck;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "InventoryE", stableKey = "thunder.inventory.held_item_change_open", setback = 3, description = "Смена слота с открытым инвентарем")
public class InventoryE extends InventoryCheck {
    private long lastTransaction = Long.MAX_VALUE; // Impossible transaction ID

    public InventoryE(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            // It is not possible to change hotbar slots with held item change while the inventory is open
            // A container click packet would be sent instead
            if (player.hasInventoryOpen) {
                if (this.lastTransaction < player.lastTransactionReceived.get()
                        && flag()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        player.inventory.needResend = true;
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

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.HELD_ITEM_CHANGE) {
            this.lastTransaction = player.lastTransactionSent.get();
        }
    }
}
