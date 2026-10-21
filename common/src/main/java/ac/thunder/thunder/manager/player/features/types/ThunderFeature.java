package ac.thunder.thunder.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.thunder.thunder.player.ThunderPlayer;

public interface ThunderFeature {
    String getName();

    void setState(ThunderPlayer player, ConfigManager config, FeatureState state);

    boolean isEnabled(ThunderPlayer player);

    boolean isEnabledInConfig(ThunderPlayer player, ConfigManager config);
}
