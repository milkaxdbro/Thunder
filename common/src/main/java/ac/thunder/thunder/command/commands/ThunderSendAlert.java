package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public class ThunderSendAlert implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("sendalert")
                        .permission("thunder.sendalert")
                        .required("message", StringParser.greedyStringParser())
                        .handler(this::handleSendAlert)
        );
    }

    private void handleSendAlert(@NotNull CommandContext<Sender> context) {
        String string = context.get("message");
        string = MessageUtil.replacePlaceholders((Sender) null, string);
        Component message = MessageUtil.miniMessage(string);
        ThunderAPI.INSTANCE.getAlertManager().sendAlert(message, null);
    }
}
