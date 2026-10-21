package ac.thunder.thunder.platform.fabric.utils.message;

import ac.thunder.thunder.platform.api.sender.Sender;

public interface IFabricMessageUtil {
    Object textLiteral(String message);

    void sendMessage(Sender target, Object message, boolean overlay);
}
