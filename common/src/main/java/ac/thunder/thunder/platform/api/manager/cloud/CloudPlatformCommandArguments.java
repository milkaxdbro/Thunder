package ac.thunder.thunder.platform.api.manager.cloud;

import ac.thunder.thunder.platform.api.command.PlayerSelector;
import ac.thunder.thunder.platform.api.sender.Sender;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.SuggestionProvider;

public interface CloudPlatformCommandArguments {
    ParserDescriptor<Sender, PlayerSelector> singlePlayerSelectorParser();

    SuggestionProvider<Sender> onlinePlayerSuggestions();
}
