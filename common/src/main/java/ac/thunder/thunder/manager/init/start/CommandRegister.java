package ac.thunder.thunder.manager.init.start;

import ac.thunder.thunder.platform.api.command.CommandService;
import ac.thunder.thunder.utils.anticheat.LogUtil;

public record CommandRegister(CommandService service) implements StartableInitable {

    @Override
    public void start() {
        try {
            if (service != null) {
                service.registerCommands();
            }
        } catch (RuntimeException t) {
            // This is the ultimate safety net. If command registration fails, Thunder keeps running.
            LogUtil.error("Failed to register commands! Thunder will run without command support.", t);
        }
    }
}
