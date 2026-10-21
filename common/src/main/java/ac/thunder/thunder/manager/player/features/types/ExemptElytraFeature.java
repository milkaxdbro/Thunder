package ac.thunder.thunder.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.thunder.thunder.player.ThunderPlayer;

public class ExemptElytraFeature implements ThunderFeature {

    @Override
    public String getName() {
        return "ExemptElytra";
    }

    @Override
    public void setState(ThunderPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExemptElytra(true);
            case DISABLED -> player.setExemptElytra(false);
            default -> player.setExemptElytra(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(ThunderPlayer player) {
        return player.isExemptElytra();
    }

    @Override
    public boolean isEnabledInConfig(ThunderPlayer player, ConfigManager config) {
        return config.getBooleanElse("exempt-elytra", false);
    }

}
