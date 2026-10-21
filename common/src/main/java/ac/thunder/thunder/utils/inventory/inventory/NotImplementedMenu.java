package ac.thunder.thunder.utils.inventory.inventory;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.inventory.Inventory;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

public class NotImplementedMenu extends AbstractContainerMenu {
    public NotImplementedMenu(ThunderPlayer player, Inventory playerInventory) {
        super(player, playerInventory);
        player.inventory.isPacketInventoryActive = false;
        player.inventory.needResend = true;
    }

    @Override
    public void doClick(int button, int slotID, WrapperPlayClientClickWindow.WindowClickType clickType) {

    }
}
