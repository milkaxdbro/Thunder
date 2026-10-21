package ac.thunder.thunder.utils.anticheat;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.player.ThunderPlayer;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Наказание уроном для подтверждённых читеров.
 *
 * <p>Игрок считается подтверждённым читером, пока суммарный уровень нарушений
 * высок ({@code min-total-violations}) либо одна проверка наврала очень много
 * ({@code min-single-violations}). Нарушения гаснут со временем (decay), так что
 * наказание снимается само, когда игрок перестаёт чудить.
 *
 * <p>Подтверждённому читеру почти полностью режется исходящий урон, а входящий
 * умножается. Всё выключается одним флагом {@code cheater-damage.enabled}.
 */
@UtilityClass
public class CheaterDamage {
    /** UUID -> был ли помечен читером (для лога переходов). */
    private final Map<UUID, Boolean> confirmedCache = new ConcurrentHashMap<>();

    public boolean isEnabled() {
        return config().getBooleanElse("cheater-damage.enabled", true);
    }

    public double outgoingFactor() {
        return config().getDoubleElse("cheater-damage.outgoing-factor", 0.05);
    }

    public double incomingFactor() {
        return config().getDoubleElse("cheater-damage.incoming-factor", 4.0);
    }

    public boolean isConfirmedCheater(@NotNull ThunderPlayer player) {
        if (!isEnabled() || player.disableThunder) {
            setConfirmed(player.getUniqueId(), player.getName(), false);
            return false;
        }

        double total = 0;
        double max = 0;
        for (AbstractCheck check : player.getChecks()) {
            if (check instanceof Check c) {
                total += c.violations;
                if (c.violations > max) max = c.violations;
            }
        }

        boolean confirmed = total >= minTotalViolations() || max >= minSingleViolations();
        setConfirmed(player.getUniqueId(), player.getName(), confirmed);
        return confirmed;
    }

    private double minTotalViolations() {
        return config().getDoubleElse("cheater-damage.min-total-violations", 100);
    }

    private double minSingleViolations() {
        return config().getDoubleElse("cheater-damage.min-single-violations", 50);
    }

    private void setConfirmed(UUID uuid, String name, boolean confirmed) {
        Boolean was = confirmedCache.put(uuid, confirmed);
        if (was != null && was != confirmed) {
            if (confirmed) {
                LogUtil.info("Читер подтверждён: " + name + " — урон урезан.");
            } else {
                LogUtil.info("Наказание уроном снято: " + name + ".");
            }
        }
        if (confirmedCache.size() > 512) {
            confirmedCache.keySet().removeIf(id ->
                    ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(id) == null);
        }
    }

    private @NotNull ConfigManager config() {
        return ThunderAPI.INSTANCE.getConfigManager().getConfig();
    }
}
