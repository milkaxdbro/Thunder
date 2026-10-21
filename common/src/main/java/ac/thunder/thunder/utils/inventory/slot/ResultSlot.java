package ac.thunder.thunder.utils.inventory.slot;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public void onTake(ThunderPlayer player, ItemStack itemStack) {
        // Resync the player's inventory
    }
}
