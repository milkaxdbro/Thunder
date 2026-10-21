package ac.thunder.thunder.platform.bukkit.utils.placeholder;

import ac.thunder.thunder.ThunderAPI;
import ac.grim.grimac.api.GrimUser;
import ac.thunder.thunder.player.ThunderPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "thunder";
    }

    public @NotNull String getAuthor() {
        return String.join(", ", ThunderAPI.INSTANCE.getThunderPlugin().getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return ThunderAPI.INSTANCE.getExternalAPI().getGrimVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        Set<String> staticReplacements = ThunderAPI.INSTANCE.getExternalAPI().getStaticReplacements().keySet();
        Set<String> variableReplacements = ThunderAPI.INSTANCE.getExternalAPI().getVariableReplacements().keySet();
        ArrayList<String> placeholders = new ArrayList<>(staticReplacements.size() + variableReplacements.size());
        for (String s : staticReplacements) {
            placeholders.add(s.equals("%thunder_version%") ? s : "%thunder_" + s.replace("%", "") + "%");
        }
        for (String s : variableReplacements) {
            placeholders.add(s.equals("%player%") ? "%thunder_player%" : "%thunder_player_" + s.replace("%", "") + "%");
        }
        return placeholders;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        for (Map.Entry<String, String> entry : ThunderAPI.INSTANCE.getExternalAPI().getStaticReplacements().entrySet()) {
            String key = entry.getKey().equals("%thunder_version%")
                    ? "version"
                    : entry.getKey().replace("%", "");
            if (params.equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        if (offlinePlayer instanceof Player player) {
            ThunderPlayer thunderPlayer = ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId());
            if (thunderPlayer == null) return null;

            for (Map.Entry<String, Function<GrimUser, String>> entry : ThunderAPI.INSTANCE.getExternalAPI().getVariableReplacements().entrySet()) {
                String key = entry.getKey().equals("%player%")
                        ? "player"
                        : "player_" + entry.getKey().replace("%", "");
                if (params.equalsIgnoreCase(key)) {
                    return entry.getValue().apply(thunderPlayer);
                }
            }
        }

        return null;
    }
}
