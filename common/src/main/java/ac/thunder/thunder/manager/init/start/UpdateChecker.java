package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.command.commands.ThunderVersion;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (ThunderAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            ThunderVersion.checkForUpdatesAsync(ThunderAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
