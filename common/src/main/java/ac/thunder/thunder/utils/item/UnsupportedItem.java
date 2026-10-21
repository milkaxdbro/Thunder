package ac.thunder.thunder.utils.item;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class UnsupportedItem extends ItemBehaviour {

    public static final UnsupportedItem INSTANCE = new UnsupportedItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, ThunderPlayer player, InteractionHand hand) {
        return false;
    }

}
