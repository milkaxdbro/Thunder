package ac.thunder.thunder.platform.fabric.mixins;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.fabric.inject.FabricServerPlayerHandle;
import ac.thunder.thunder.platform.fabric.player.FabricPlatformPlayerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = ac.thunder.thunder.platform.fabric.inject.FabricServerPlayerHandle.class, prefix = "thunder$"))
abstract class FabricIntermediaryServerPlayerMixin {

    public boolean thunder$isSneaking() {
        return ((ServerPlayer) (Object) this).isShiftKeyDown();
    }

    public void thunder$setSneaking(boolean sneaking) {
        ((ServerPlayer) (Object) this).setShiftKeyDown(sneaking);
    }

    public boolean thunder$isDead() {
        return ((ServerPlayer) (Object) this).isDeadOrDying();
    }

    public void thunder$sendSystemText(Object nativeComponent) {
        ((ServerPlayer) (Object) this).displayClientMessage((Component) nativeComponent, false);
    }

    public boolean thunder$isDisconnected() {
        return ((ServerPlayer) (Object) this).hasDisconnected();
    }

    public String thunder$usernameString() {
        return ((ServerPlayer) (Object) this).getName().getString();
    }

    public void thunder$broadcastInventoryChanges() {
        ((ServerPlayer) (Object) this).containerMenu.broadcastChanges();
    }

    public void thunder$stopUsingItem() {
        ((ServerPlayer) (Object) this).stopUsingItem();
    }

    public boolean thunder$isUsingItem() {
        return ((ServerPlayer) (Object) this).isUsingItem();
    }

    public double thunder$posX() {
        return ((ServerPlayer) (Object) this).getX();
    }

    public double thunder$posY() {
        return ((ServerPlayer) (Object) this).getY();
    }

    public double thunder$posZ() {
        return ((ServerPlayer) (Object) this).getZ();
    }

    public UUID thunder$uuid() {
        return ((ServerPlayer) (Object) this).getUUID();
    }

    public Object thunder$vehicleEntity() {
        return ((ServerPlayer) (Object) this).getVehicle();
    }

    public Object thunder$gameMode() {
        return ((ServerPlayer) (Object) this).gameMode.getGameModeForPlayer();
    }

    public Object thunder$heldItemStack() {
        return ((ServerPlayer) (Object) this).inventory.getSelected();
    }

    public Object thunder$inventoryItemAt(int slot) {
        return ((ServerPlayer) (Object) this).inventory.getItem(slot);
    }

    public Object thunder$usedItemHand() {
        return ((ServerPlayer) (Object) this).getUsedItemHand();
    }

    public int thunder$inventorySlotCount() {
        return ((ServerPlayer) (Object) this).inventory.getContainerSize();
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ((FabricPlatformPlayerFactory) ThunderAPI.INSTANCE.getPlatformPlayerFactory())
                .replaceNativePlayer(oldPlayer.getUUID(), (FabricServerPlayerHandle) (Object) this);
    }
}
