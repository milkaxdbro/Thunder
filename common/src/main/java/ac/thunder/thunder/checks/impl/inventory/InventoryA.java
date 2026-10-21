package ac.thunder.thunder.checks.impl.inventory;

import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.InventoryCheck;
import ac.thunder.thunder.player.ThunderPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;

@CheckData(name = "InventoryA", stableKey = "thunder.inventory.attack_open", setback = 3, description = "Атака с открытым инвентарем")
public class InventoryA extends InventoryCheck {
    public InventoryA(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            if (wrapper.getAction() != InteractAction.ATTACK) return;

            // Is not possible to attack while the inventory is open.
            if (player.hasInventoryOpen) {
                if (flag()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    if (!isNoSetbackPermission())
                        closeInventory();
                }
            } else {
                reward();
            }
        }
    }
}
