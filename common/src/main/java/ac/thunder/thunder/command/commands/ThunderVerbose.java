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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ThunderVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("thunder", "th")
                        .literal("verbose")
                        .permission("thunder.verbose")
                        .handler(this::handleVerbose)
        );
    }

    private void handleVerbose(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer player = Objects.requireNonNull(context.sender().getPlatformPlayer(), "player");
            AlertManagerImpl am = ThunderAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasVerboseEnabled(player);
            am.setVerboseEnabled(player, newState, false);
            PlayerToggleStore toggles = ThunderAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore();
            toggles.applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_VERBOSE, newState);
            // setVerboseEnabled(true) cascades to setAlertsEnabled(true) in AlertManager
            // — mirror that into the toggle store so the persisted alerts row tracks the
            // implied state, otherwise a verbose-on staff member would re-toggle alerts
            // off on next reconnect when persisted alerts is still false.
            if (newState) toggles.applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_ALERTS, true);
        } else if (sender.isConsole()) {
            ThunderAPI.INSTANCE.getAlertManager().toggleConsoleVerbose();
        }
    }
}
