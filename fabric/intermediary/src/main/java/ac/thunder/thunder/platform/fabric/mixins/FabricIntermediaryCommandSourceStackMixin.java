package ac.thunder.thunder.platform.fabric.mixins;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.platform.fabric.ThunderFabricIntermediaryLoaderPlugin;
import ac.thunder.thunder.platform.fabric.sender.FabricIntermediarySenderFactory;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(CommandSourceStack.class)
@Implements(@Interface(iface = Sender.class, prefix = "thunder$"))
abstract class FabricIntermediaryCommandSourceStackMixin {

    @Unique
    private CommandSourceStack thunder$self() {
        return (CommandSourceStack) (Object) this;
    }

    @Unique
    private FabricIntermediarySenderFactory thunder$factory() {
        return ThunderFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory();
    }

    public UUID thunder$getUniqueId() {
        return thunder$factory().getUniqueId(thunder$self());
    }

    public String thunder$getName() {
        return thunder$factory().getName(thunder$self());
    }

    public void thunder$sendMessage(String message) {
        thunder$factory().sendMessage(thunder$self(), message);
    }

    public void thunder$sendMessage(Component message) {
        thunder$factory().sendMessage(thunder$self(), message);
    }

    public boolean thunder$hasPermission(String permission) {
        return thunder$factory().hasPermission(thunder$self(), permission);
    }

    public boolean thunder$hasPermission(String permission, boolean defaultIfUnset) {
        return thunder$factory().hasPermission(thunder$self(), permission, defaultIfUnset);
    }

    public void thunder$performCommand(String commandLine) {
        thunder$factory().performCommand(thunder$self(), commandLine);
    }

    public boolean thunder$isConsole() {
        return thunder$factory().isConsole(thunder$self());
    }

    public Object thunder$getNativeSender() {
        return thunder$self();
    }

    public @Nullable PlatformPlayer thunder$getPlatformPlayer() {
        return ThunderAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(thunder$getUniqueId());
    }
}
