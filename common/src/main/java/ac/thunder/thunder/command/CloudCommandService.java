package ac.thunder.thunder.command;

import ac.thunder.thunder.command.commands.*;
import ac.thunder.thunder.command.handler.ThunderCommandFailureHandler;
import ac.thunder.thunder.platform.api.command.CommandService;
import ac.thunder.thunder.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.thunder.thunder.platform.api.sender.Sender;
import ac.thunder.thunder.utils.anticheat.MessageUtil;
import io.leangen.geantyref.TypeToken;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementApplicable.RequirementApplicableFactory;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;

import java.util.Locale;
import java.util.function.Supplier;

public class CloudCommandService implements CommandService {

    public static final CloudKey<Requirements<Sender, SenderRequirement>> REQUIREMENT_KEY
            = CloudKey.of("requirements", new TypeToken<>() {});

    public static final RequirementApplicableFactory<Sender, SenderRequirement> REQUIREMENT_FACTORY
            = RequirementApplicable.factory(REQUIREMENT_KEY);

    private boolean commandsRegistered = false;

    private final Supplier<CommandManager<Sender>> commandManagerSupplier;
    private final CloudPlatformCommandArguments commandArguments;

    public CloudCommandService(Supplier<CommandManager<Sender>> commandManagerSupplier, CloudPlatformCommandArguments commandArguments) {
        this.commandManagerSupplier = commandManagerSupplier;
        this.commandArguments = commandArguments;
    }

    public void registerCommands() {
        if (commandsRegistered) return;
        CommandManager<Sender> commandManager = commandManagerSupplier.get();
        new ThunderPerf().register(commandManager, commandArguments);
        new ThunderDebug().register(commandManager, commandArguments);
        new ThunderAlerts().register(commandManager, commandArguments);
        new ThunderProfile().register(commandManager, commandArguments);
        new ThunderSendAlert().register(commandManager, commandArguments);
        new ThunderHelp().register(commandManager, commandArguments);
        new ThunderHistory().register(commandManager, commandArguments);
        new ThunderHistoryMigrate().register(commandManager, commandArguments);
        new ThunderHistoryCopy().register(commandManager, commandArguments);
        new ThunderReload().register(commandManager, commandArguments);
        new ThunderSpectate().register(commandManager, commandArguments);
        new ThunderStopSpectating().register(commandManager, commandArguments);
        new ThunderLog().register(commandManager, commandArguments);
        new ThunderVerbose().register(commandManager, commandArguments);
        new ThunderVersion().register(commandManager, commandArguments);
        new ThunderDump().register(commandManager, commandArguments);
        new ThunderBrands().register(commandManager, commandArguments);
        new ThunderList().register(commandManager, commandArguments);
        new ThunderTestWebhook().register(commandManager, commandArguments);
        new ThunderSpec().register(commandManager, commandArguments);

        final RequirementPostprocessor<Sender, SenderRequirement>
                senderRequirementPostprocessor = RequirementPostprocessor.of(
                REQUIREMENT_KEY,
                new ThunderCommandFailureHandler()
        );
        commandManager.registerCommandPostProcessor(senderRequirementPostprocessor);
        registerInvalidSyntaxHandler(commandManager);
        commandsRegistered = true;
    }

    private void registerInvalidSyntaxHandler(CommandManager<Sender> commandManager) {
        commandManager.exceptionController().registerHandler(InvalidSyntaxException.class, context -> {
            Sender sender = context.context().sender();
            if (isHistoryInput(context.context().rawInput().input())) {
                MessageUtil.sendConfigLines(sender, "syntax-history-help", java.util.List.of(
                        "&#D080AC▍ &#CACABEНеверный синтаксис history!",
                        "&#D080AC&n▍&r &#D080AC/thunder history <игрок> &#CACABE— список сессий",
                        "&#D080AC&n▍&r &#D080AC/thunder history <игрок> session latest &#CACABE— детали последней сессии",
                        "&#D080AC&n▍&r &#CACABEПример: &#D080AC/thunder history Steve session latest -d -v",
                        "&#D080AC▍ &#CACABEПодсказки: жми TAB"));
                return;
            }
            MessageUtil.sendConfigLines(sender, "syntax-help", java.util.List.of(
                    "&#D080AC▍ &#CACABEНеверный синтаксис!",
                    "&#D080AC&n▍&r &#CACABEОсновные команды:",
                    "&#D080AC&n▍&r &#D080AC/thunder profile <игрок> &#CACABE— инфо об игроке",
                    "&#D080AC&n▍&r &#D080AC/thunder history <игрок> &#CACABE— история нарушений",
                    "&#D080AC&n▍&r &#D080AC/thunder alerts &#CACABE— оповещения вкл/выкл",
                    "&#D080AC&n▍&r &#D080AC/thunder spectate <игрок> &#CACABE— &#D080AC&nследить&r&#CACABE за игроком",
                    "&#D080AC&n▍&r &#D080AC/thunder help &#CACABE— все команды",
                    "&#D080AC▍ &#CACABEПодсказки: жми TAB в чате"));
        });
        commandManager.exceptionController().registerHandler(NoSuchCommandException.class, context ->
                MessageUtil.sendConfigLines(context.context().sender(), "syntax-help", java.util.List.of(
                        "&#D080AC▍ &#CACABEНеизвестная команда!")));
    }

    private static boolean isHistoryInput(String rawInput) {
        String input = rawInput.strip();
        if (input.startsWith("/")) input = input.substring(1).strip();
        String[] tokens = input.toLowerCase(Locale.ROOT).split("\\s+");
        return tokens.length >= 2
                && (tokens[0].equals("thunder") || tokens[0].equals("th"))
                && (tokens[1].equals("history") || tokens[1].equals("hist"));
    }
}
