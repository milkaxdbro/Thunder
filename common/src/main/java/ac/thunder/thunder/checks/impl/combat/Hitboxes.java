package ac.thunder.thunder.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.checks.type.PacketCheck;
import ac.thunder.thunder.checks.type.PacketReceiveListener;
import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.collisions.datatypes.SimpleCollisionBox;
import ac.thunder.thunder.utils.data.packetentity.PacketEntity;
import ac.thunder.thunder.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.thunder.thunder.utils.enums.Pose;
import ac.thunder.thunder.utils.math.Vector3dm;
import ac.thunder.thunder.utils.nmsutil.GetBoundingBox;
import ac.thunder.thunder.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Проверяет, что удар атакующего действительно мог попасть в ванильный хитбокс цели.
 *
 * <p>Для цели строится строгий ванильный бокс (для игроков — по текущей позе:
 * стоя 0.6x1.8, крадучись 0.6x1.5, вплавь/глайд 0.6x0.6 — с учётом scale),
 * вокруг компенсированной (интерполированной) позиции сущности. Бокс расширяется
 * на неопределённость движения ({@code movementThreshold}) и на задержку сети
 * атакующего ({@code ping * ping-factor}, с потолком), затем луч взгляда
 * атакующего проверяется со всех возможных позиций глаз (текущая и прошлая)
 * и ротаций (текущая и прошлая). Промах мимо расширенного бокса считается
 * подозрительным только после {@code required-misses} попаданий подряд.
 *
 * <p>Удары вне дистанции взаимодействия — зона ответственности Reach, здесь
 * они игнорируются.
 */
@CheckData(name = "Hitboxes", stableKey = "thunder.combat.hitboxes", description = "Попадание вне ванильного хитбокса", setback = 10)
public class Hitboxes extends Check implements PacketCheck, PacketReceiveListener {
    private static final Verbose V = Verbose.of("miss={f64:%.3f} blocks, ping={uint}ms");

    private static final List<EntityType> BLACKLISTED = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);

    // Шаг и предел измерения величины промаха расширением бокса
    private static final double MISS_STEP = 0.05;
    private static final double MISS_CAP = 1.0;
    // Запас дальности луча сверх дистанции взаимодействия (как в Reach)
    private static final double EXTRA_RAY_DISTANCE = 3.0;

    private double threshold = 0.05;
    private int requiredMisses = 2;
    private double pingFactor = 0.006;
    private double maxPingLeniency = 0.5;
    private double cancelThreshold = 0.3;

    private int missStreak = 0;

    public Hitboxes(ThunderPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableThunder) return;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            handleAttack(event, new WrapperPlayClientAttack(event).getEntityId());
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(event, packet.getEntityId());
            }
        }
    }

    private void handleAttack(PacketReceiveEvent event, int entityId) {
        // Не даём обходить проверку телепортом во время сетвэка
        if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        if (entity == null || entity instanceof PacketEntityEnderDragonPart) return;
        if (entity.isDead) return;
        if (BLACKLISTED.contains(entity.getType())) return;
        // Модели этих сущностей рассинхронизируются на старых клиентах (как в Reach)
        if (entity.getType() == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_9))
            return;
        if (entity.getType() == EntityTypes.HAPPY_GHAST && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6))
            return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        if (player.inVehicle()) return;
        if (entity.riding != null) return;
        if (!entity.isLivingEntity && entity.getType() != EntityTypes.END_CRYSTAL) return;

        double range = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        // Строгий ванильный бокс цели + компенсация движения и пинга
        SimpleCollisionBox targetBox = getVanillaTargetBox(entity);
        targetBox.expand(getLeniency(entity));

        // Луч из лювой возможной позиции глаз/ротации: хоть одно попадание = легит
        if (rayHitsBox(targetBox, range)) {
            missStreak = 0;
            return;
        }

        // Цель вне дистанции взаимодействия — это территория Reach, не наша
        if (ReachUtils.getMinReachToBox(player, targetBox) > range) {
            missStreak = 0;
            return;
        }

        double miss = measureMiss(targetBox, range);
        if (miss <= threshold) {
            missStreak = 0;
            return;
        }

        missStreak++;
        int pingMs = Math.max(0, player.getTransactionPing());

        // Отменяем явные промахи сразу, не дожидаясь серии
        if (miss > cancelThreshold && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (missStreak >= requiredMisses) {
            missStreak = 0;
            flagWithSetback(V.write(verbose()).f64(miss).uint(pingMs));
        }
    }

    /**
     * Строит ванильный хитбокс цели вокруг её компенсированной позиции.
     * Для игроков бокс собирается строго по позе (width/height позы + scale),
     * для остальных сущностей верётся интерполированный компенсированный бокс.
     */
    private SimpleCollisionBox getVanillaTargetBox(PacketEntity entity) {
        if (entity.getType() == EntityTypes.END_CRYSTAL) {
            Vector3d pos = entity.trackedServerPosition.getPos();
            return new SimpleCollisionBox(pos.subtract(1, 0, 1), pos.add(1, 2, 1));
        }

        if (entity.getType() == EntityTypes.PLAYER) {
            SimpleCollisionBox position = entity.getPossibleLocationBoxes();
            double centerX = (position.minX + position.maxX) / 2.0;
            double centerZ = (position.minZ + position.maxZ) / 2.0;
            float width = entity.currentPose.width;
            float height = entity.currentPose.height;
            if (entity.transitionalPose != null) {
                width = Math.max(width, entity.transitionalPose.width);
                height = Math.max(height, entity.transitionalPose.height);
            }
            float scale = (float) entity.getAttributeValue(Attributes.SCALE);
            return GetBoundingBox.getBoundingBoxFromPosAndSizeRaw(centerX, position.minY, centerZ, width * scale, height * scale);
        }

        return entity.getPossibleCollisionBoxes().copy();
    }

    /**
     * Суммарное расширение бокса против ложных срабатываний:
     * порог движения клиента + задержка сети атакующего (цель успевает
     * сместиться, пока пакеты идут туда-обратно) + ванильный запас 1.8.
     * Элитра/трезувец быстрые — для них отдельный запас.
     */
    private double getLeniency(PacketEntity entity) {
        int pingMs = Math.max(0, player.getTransactionPing());
        double leniency = player.getMovementThreshold() + Math.min(pingMs * pingFactor, maxPingLeniency);

        // Ванильный увеличенный хитбокс на 1.7/1.8 (как в Reach)
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            leniency += 0.1;
        }

        // Быстрые цели: за пинг смещаются сильнее обычного спринта
        if (entity.getType() == EntityTypes.PLAYER
                && (entity.currentPose == Pose.FALL_FLYING || entity.currentPose == Pose.SPIN_ATTACK)) {
            leniency += 1.0;
        }

        return leniency;
    }

    private boolean rayHitsBox(SimpleCollisionBox box, double range) {
        double distance = range + EXTRA_RAY_DISTANCE;
        double[][] bases = {{player.x, player.y, player.z}, {player.lastX, player.lastY, player.lastZ}};
        double[] eyes = player.getPossibleEyeHeights();
        Vector3dm[] looks = player.getPossibleLookVectors(false);

        for (double[] base : bases) {
            for (double eye : eyes) {
                Vector3d eyePos = new Vector3d(base[0], base[1] + eye, base[2]);
                if (ReachUtils.isVecInside(box, eyePos)) return true;
                for (Vector3dm look : looks) {
                    Vector3d end = eyePos.add(look.getX() * distance, look.getY() * distance, look.getZ() * distance);
                    if (ReachUtils.calculateIntercept(box, eyePos, end).first() != null) return true;
                }
            }
        }
        return false;
    }

    /**
     * Величина промаха: минимальное дополнительное расширение бокса,
     * при котором хоть один луч начинает попадать.
     */
    private double measureMiss(SimpleCollisionBox box, double range) {
        for (double extra = MISS_STEP; extra <= MISS_CAP; extra += MISS_STEP) {
            if (rayHitsBox(box.copy().expand(extra), range)) return extra;
        }
        return MISS_CAP;
    }

    @Override
    public void onReload(ConfigManager config) {
        this.threshold = config.getDoubleElse("Hitboxes.threshold", 0.05);
        this.requiredMisses = Math.max(1, config.getIntElse("Hitboxes.required-misses", 2));
        this.pingFactor = config.getDoubleElse("Hitboxes.ping-factor", 0.006);
        this.maxPingLeniency = config.getDoubleElse("Hitboxes.max-ping-leniency", 0.5);
        this.cancelThreshold = config.getDoubleElse("Hitboxes.cancel-threshold", 0.3);
    }
}
