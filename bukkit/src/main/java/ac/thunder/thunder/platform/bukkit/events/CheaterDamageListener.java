package ac.thunder.thunder.platform.bukkit.events;

import ac.thunder.thunder.ThunderAPI;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.anticheat.CheaterDamage;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Наказание уроном: подтверждённый читер почти не наносит урона,
 * но получает многократно увеличенный.
 */
public class CheaterDamageListener implements Listener {

    // Жертва — читер: ВЕСЬ входящий урон умножается (мобы, падение, огонь, игроки)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!CheaterDamage.isEnabled()) return;

        if (event.getEntity() instanceof Player victim) {
            ThunderPlayer thunderVictim = getThunderPlayer(victim.getUniqueId());
            if (thunderVictim != null && CheaterDamage.isConfirmedCheater(thunderVictim)) {
                event.setDamage(event.getDamage() * CheaterDamage.incomingFactor());
            }
        }
    }

    // Атакующий — читер: исходящий урон режется
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!CheaterDamage.isEnabled()) return;

        Player attacker = resolveAttacker(event);
        if (attacker != null) {
            ThunderPlayer thunderAttacker = getThunderPlayer(attacker.getUniqueId());
            if (thunderAttacker != null && CheaterDamage.isConfirmedCheater(thunderAttacker)) {
                event.setDamage(event.getDamage() * CheaterDamage.outgoingFactor());
            }
        }
    }

    private @Nullable ThunderPlayer getThunderPlayer(UUID uuid) {
        try {
            return ThunderAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    private @Nullable Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
