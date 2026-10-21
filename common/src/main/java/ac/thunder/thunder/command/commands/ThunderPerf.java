package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.predictionengine.MovementCheckRunner;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class ThunderPerf {

    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> thunderCommand = commandManager.commandBuilder("thunder", "th");

        Command.Builder<Sender> configuredBuilder = thunderCommand
                .literal("perf", "performance")
                .permission("thunder.performance")
                .handler(this::handlePerformance);

        commandManager.command(configuredBuilder);
    }

    private void handlePerformance(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        double millis = MovementCheckRunner.predictionNanos / 1000000;
        double longMillis = MovementCheckRunner.longPredictionNanos / 1000000;

        Component message1 = MessageUtil.getParsedComponent(sender, "perf-line",
                "&#D080AC▍ &#CACABEМиллисекунд на предсказание (сред. %avg%): %value%",
                java.util.Map.of("%avg%", "500", "%value%", Double.toString(millis)));

        Component message2 = MessageUtil.getParsedComponent(sender, "perf-line",
                "&#D080AC▍ &#CACABEМиллисекунд на предсказание (сред. %avg%): %value%",
                java.util.Map.of("%avg%", "20k", "%value%", Double.toString(longMillis)));

        sender.sendMessage(message1);
        sender.sendMessage(message2);
    }
}
