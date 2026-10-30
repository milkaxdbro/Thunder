package ac.thunder.thunder.platform.bukkit.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Блокировка подбора предметов наказанным читером.
 *
 * <p>Театральный {@code [kick]} сначала выбрасывает весь инвентарь игрока на
 * землю и только через ~3 секунды кикает его. В этом окне читер, оставаясь на
 * сервере, успевает подобрать свои же ресурсы обратно. Этот лок отменяет
 * любой подбор предметов наказанным игроком до самого кика (и ещё немного
 * после него — на случай мгновенного релога; живые игроки подбирают как
 * обычно).
 */
public class KickPickupLock implements Listener {

    /**
     * Сколько миллисекунд лок остаётся включённым с момента наказания.
     * Окно кика — 60 тиков (3 с), берём вдвое больше как запас на релог.
     */
    private static final long LOCK_DURATION_MS = 10_000L;

    private static final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<>();

    public static void lockForKick(UUID uuid) {
        lockedUntil.put(uuid, System.currentTimeMillis() + LOCK_DURATION_MS);
        // точечная уборка просроченных локов
        if (lockedUntil.size() > 64) {
            lockedUntil.values().removeIf(until -> until < System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Long until = lockedUntil.get(player.getUniqueId());
        if (until == null) return;
        if (until < System.currentTimeMillis()) {
            lockedUntil.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
    }
}
