package ac.thunder.thunder.command.requirements;

import ac.thunder.thunder.command.SenderRequirement;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public enum PlayerSenderRequirement implements SenderRequirement {
    INSTANCE;

    @Override
    public @NotNull Component errorMessage(Sender sender) {
        return MessageUtil.getParsedComponent(sender, "run-as-player", "%prefix% &cThis command can only be used by players!");
    }

    @Override
    public boolean evaluateRequirement(@NotNull CommandContext<Sender> commandContext) {
        return commandContext.sender().isPlayer();
    }
}
