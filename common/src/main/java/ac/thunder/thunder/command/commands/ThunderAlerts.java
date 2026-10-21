package ac.thunder.thunder.command.commands;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.BuildableCommand;
import ac.thunder.thunder.manager.AlertManagerImpl;
import ac.thunder.thunder.manager.datastore.PlayerToggleStore;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.player.PlatformPlayer;
import ac.thunder.thunder.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ThunderAlerts implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("alerts", Description.of("Вкл/выкл оповещения"))
                        .permission("thunder.alerts")
                        .handler(this::handleAlerts)
        );
    }

    private void handleAlerts(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer player = Objects.requireNonNull(context.sender().getPlatformPlayer(), "player");
            AlertManagerImpl am = ThunderAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasAlertsEnabled(player);
            am.setAlertsEnabled(player, newState, false);
            ThunderAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore()
                    .applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_ALERTS, newState);
        } else if (sender.isConsole()) {
            ThunderAPI.INSTANCE.getAlertManager().toggleConsoleAlerts();
        }
    }
}
