package ac.thunder.thunder.platform.fabric.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = ac.thunder.thunder.platform.fabric.inject.FabricServerPlayerHandle.class, prefix = "thunder$"))
abstract class FabricOfficialServerPlayerMixin extends Player {

    public FabricOfficialServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    public void thunder$resyncSharedFlags() {
        this.getEntityData().getItem(DATA_SHARED_FLAGS_ID).setDirty(true);
        this.getEntityData().isDirty = true;
    }

    public boolean thunder$isDead() {
        return this.isDeadOrDying();
    }

    public void thunder$sendSystemText(Object nativeComponent) {
        ((ServerPlayer) (Object) this).sendSystemMessage((Component) nativeComponent, false);
    }

    public boolean thunder$isDisconnected() {
        return ((ServerPlayer) (Object) this).hasDisconnected();
    }

    public String thunder$usernameString() {
        return this.getName().getString();
    }

    public void thunder$broadcastInventoryChanges() {
        this.containerMenu.broadcastChanges();
    }

    public double thunder$posX() {
        return this.getX();
    }

    public double thunder$posY() {
        return this.getY();
    }

    public double thunder$posZ() {
        return this.getZ();
    }

    public UUID thunder$uuid() {
        return this.getUUID();
    }

    public Object thunder$vehicleEntity() {
        return this.getVehicle();
    }

    public Object thunder$gameMode() {
        return ((ServerPlayer) (Object) this).gameMode.getGameModeForPlayer();
    }

    public Object thunder$heldItemStack() {
        return this.inventory.getSelectedItem();
    }

    public Object thunder$inventoryItemAt(int slot) {
        return this.inventory.getItem(slot);
    }

    public Object thunder$usedItemHand() {
        return this.getUsedItemHand();
    }

    public int thunder$inventorySlotCount() {
        return this.inventory.getContainerSize();
    }

    // NOTE: isUsingItem()/stopUsingItem() are intentionally NOT bodied here. On the mojmap
    // runtime the vanilla ServerPlayer methods of the same name satisfy the injected interface
    // directly; a thunder$ body would graft a same-named method and self-recurse (StackOverflow).
    // The intermediary mixin DOES body them (vanilla is method_6115/method_6021 there, so no clash).
}
