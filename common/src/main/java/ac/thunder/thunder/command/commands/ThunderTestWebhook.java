package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.LogUtil;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import ac.thunder.thunder.utils.data.webhook.discord.WebhookMessage;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class ThunderTestWebhook implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("testwebhook")
                        .permission("thunder.testwebhook")
                        .handler(this::handleTestWebhook)
        );
    }

    private void handleTestWebhook(@NotNull CommandContext<Sender> context) {
        if (ThunderAPI.INSTANCE.getDiscordManager().isDisabled()) {
            context.sender().sendMessage(MessageUtil.miniMessage(ThunderAPI.INSTANCE.getConfigManager().getWebhookNotEnabled()));
            return;
        }

        WebhookMessage webhookMessage = new WebhookMessage().content(ThunderAPI.INSTANCE.getConfigManager().getWebhookTestMessage());
        ThunderAPI.INSTANCE.getDiscordManager().sendWebhookMessage(webhookMessage).whenCompleteAsync(((successful, throwable) -> {
            if (successful == true) {
                context.sender().sendMessage(MessageUtil.miniMessage(ThunderAPI.INSTANCE.getConfigManager().getWebhookTestSucceeded()));
                return;
            }

            context.sender().sendMessage(MessageUtil.miniMessage(ThunderAPI.INSTANCE.getConfigManager().getWebhookTestFailed()));

            if (throwable != null) {
                LogUtil.error("Exception caught while sending a Discord webhook test alert", throwable);
            }
        }));
    }
}
