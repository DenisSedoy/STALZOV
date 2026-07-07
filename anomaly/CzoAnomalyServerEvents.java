package com.czo.anomaly;

import com.czo.CZO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stage 8F anomaly behaviour repair and new anomaly blocks.
 *
 * <p>Важное: это всё ещё path-driven слой. Он цепляет поведение к CZO-блокам, если их id
 * содержит нужный ключ: voronka, carousel/karusel/vikh, trampoline/springboard/tramplin,
 * hydraulic/gidrav, inferno/zhar/burner, steam/par, electra, gas, zero_gravity, cobra,
 * obr_116/toxic_moss, bubble.</p>
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoAnomalyServerEvents {
    private static final int SCAN_RADIUS = 10;
    private static final int SCAN_DOWN = 2;
    private static final int SCAN_UP = 16;

    // Нульграва работает не сферой, а вертикальной шахтой над блоком:
    // 3x3 по X/Z и 64 блока вверх от самой аномалии.
    private static final int ZERO_GRAVITY_HALF_WIDTH = 1;
    private static final int ZERO_GRAVITY_HEIGHT_UP = 64;
    private static final int ZERO_GRAVITY_SCAN_HORIZONTAL = 2;

    private static final int THERMAL_ACTIVE_TICKS = 240;      // 12 секунд.
    private static final int THERMAL_COOLDOWN_TICKS = 20;     // 1 секунда после активной фазы.
    private static final int VORONKA_ACTIVE_TICKS = 200;      // 10 секунд засасывания.
    private static final int VORONKA_COOLDOWN_TICKS = 60;

    private static final Map<String, Integer> COBRA_COOLDOWNS = new HashMap<>();
    private static final Map<String, Integer> IMPACT_COOLDOWNS = new HashMap<>();
    private static final Map<String, Integer> ACID_COOLDOWNS = new HashMap<>();
    private static final Map<String, Long> THERMAL_ACTIVE_UNTIL = new HashMap<>();
    private static final Map<String, Long> THERMAL_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<String, Long> VORONKA_STARTED_AT = new HashMap<>();
    private static final Map<String, Long> VORONKA_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<String, Long> PROJECTILE_TRIGGER_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> BUBBLE_EXPOSURE_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> CAROUSEL_EXPOSURE_TICKS = new HashMap<>();
    private static final Set<UUID> ZERO_GRAVITY_PLAYERS = new HashSet<>();

    private CzoAnomalyServerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity rawEntity = event.getEntity();

        // Stage 8G/8H: временный "болт" для тестов — обычный снежок.
        // В 26.1.2 имя класса снежка в mappings может отличаться, поэтому не
        // импортируем Snowball напрямую: определяем его по EntityType id.
        if (isTemporaryBoltEntity(rawEntity) && rawEntity.level() instanceof ServerLevel projectileLevel) {
            long now = projectileLevel.getGameTime();
            tickCooldowns(now);
            scanAndActivateByProjectile(projectileLevel, rawEntity, now);
            return;
        }

        if (!(rawEntity instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (player.level().isClientSide() || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            BUBBLE_EXPOSURE_TICKS.remove(player.getUUID());
            CAROUSEL_EXPOSURE_TICKS.remove(player.getUUID());
            clearZeroGravity(player);
            return;
        }

        long now = level.getGameTime();
        tickCooldowns(now);
        scanAndApply(level, player, now);
    }

    private static void tickCooldowns(long now) {
        tickIntMap(COBRA_COOLDOWNS);
        tickIntMap(IMPACT_COOLDOWNS);
        tickIntMap(ACID_COOLDOWNS);
        removeExpired(THERMAL_COOLDOWN_UNTIL, now);
        removeExpired(VORONKA_COOLDOWN_UNTIL, now);
        removeExpired(PROJECTILE_TRIGGER_COOLDOWN_UNTIL, now);

        // Когда жарка/инферно догорели, ставим короткий кулдаун. Это чинит ситуацию,
        // когда активное состояние исчезало без cooldown и тут же дергалось снова.
        Iterator<Map.Entry<String, Long>> thermalIterator = THERMAL_ACTIVE_UNTIL.entrySet().iterator();
        while (thermalIterator.hasNext()) {
            Map.Entry<String, Long> entry = thermalIterator.next();
            if (entry.getValue() <= now) {
                thermalIterator.remove();
                THERMAL_COOLDOWN_UNTIL.put(entry.getKey(), now + THERMAL_COOLDOWN_TICKS);
            }
        }

        Iterator<Map.Entry<String, Long>> voronkaIterator = VORONKA_STARTED_AT.entrySet().iterator();
        while (voronkaIterator.hasNext()) {
            Map.Entry<String, Long> entry = voronkaIterator.next();
            if (now - entry.getValue() > VORONKA_ACTIVE_TICKS + 2L) {
                voronkaIterator.remove();
                VORONKA_COOLDOWN_UNTIL.put(entry.getKey(), now + VORONKA_COOLDOWN_TICKS);
            }
        }
    }

    private static void tickIntMap(Map<String, Integer> map) {
        map.replaceAll((key, value) -> value - 1);
        map.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    private static void removeExpired(Map<String, Long> map, long now) {
        map.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static boolean isTemporaryBoltEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && "snowball".equals(id.getPath())) {
            return true;
        }
        // Fallback for mappings/classes where the id path is not enough.
        String className = entity.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("snowball");
    }

    private static BlockPos blockPosOf(Entity entity) {
        return new BlockPos(
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ())
        );
    }

    private static void discardEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        try {
            Entity.class.getMethod("discard").invoke(entity);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Entity.class.getMethod("remove", Entity.RemovalReason.class).invoke(entity, Entity.RemovalReason.DISCARDED);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void scanAndActivateByProjectile(ServerLevel level, Entity projectile, long now) {
        BlockPos base = blockPosOf(projectile);
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    scan.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    String path = czoBlockPath(level, scan);
                    if (path.isEmpty() || !path.contains("anomaly")) {
                        continue;
                    }

                    BlockPos pos = scan.immutable();
                    if (activateAnomalyByProjectile(level, projectile, pos, path, now)) {
                        discardEntity(projectile);
                        return;
                    }
                }
            }
        }
    }

    private static boolean activateAnomalyByProjectile(ServerLevel level, Entity projectile, BlockPos pos, String path, long now) {
        double distance = distanceToBlockEdge(new Vec3(projectile.getX(), projectile.getY(), projectile.getZ()), pos);
        if (distance > 1.85D) {
            return false;
        }

        String key = "projectile@" + pos.asLong();
        if (PROJECTILE_TRIGGER_COOLDOWN_UNTIL.containsKey(key)) {
            return false;
        }

        PROJECTILE_TRIGGER_COOLDOWN_UNTIL.put(key, now + 10L);

        if (isThermal(path)) {
            if (isSteam(path)) {
                spawnSteamColumn(level, pos);
            } else {
                THERMAL_ACTIVE_UNTIL.put(posKey(pos), now + THERMAL_ACTIVE_TICKS);
                spawnThermalColumn(level, pos);
            }
            return true;
        }

        if (isElectra(path)) {
            spawnElectraBurstParticles(level, pos);
            return true;
        }

        if (isAcid(path)) {
            spawnAcidBurstParticles(level, pos);
            return true;
        }

        if (isVoronka(path)) {
            VORONKA_STARTED_AT.putIfAbsent(posKey(pos), now);
            spawnVoronkaParticles(level, pos, 0.15D, VORONKA_ACTIVE_TICKS);
            return true;
        }

        if (isCarousel(path)) {
            spawnCarouselParticles(level, pos, 30);
            return true;
        }

        if (isBubble(path)) {
            spawnBubbleParticles(level, pos);
            return true;
        }

        if (isCobra(path)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 22, 0.35D, 0.35D, 0.35D, 0.025D);
            return true;
        }

        if (isHydraulic(path) || isSpringboard(path) || isGenericGravityImpact(path)) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D, 28, 0.42D, 0.18D, 0.42D, 0.16D);
            return true;
        }

        if (isZeroGravity(path)) {
            spawnZeroGravityParticles(level, pos);
            return true;
        }

        if (isToxicMoss(path)) {
            level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 18, 0.35D, 0.40D, 0.35D, 0.010D);
            return true;
        }

        return false;
    }

    private static void scanAndApply(ServerLevel level, ServerPlayer player, long now) {
        BlockPos base = player.blockPosition();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        boolean insideBubble = false;
        boolean insideCarousel = false;

        // Отдельная проверка Нульгравы. Не расширяем общий скан до 64 блоков вниз,
        // чтобы остальные аномалии не начали просчитываться слишком далеко от игрока.
        boolean insideZeroGravity = applyZeroGravityColumnIfInside(level, player);

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                for (int dy = -SCAN_DOWN; dy <= SCAN_UP; dy++) {
                    scan.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    String path = czoBlockPath(level, scan);
                    if (path.isEmpty() || !path.contains("anomaly")) {
                        continue;
                    }

                    BlockPos pos = scan.immutable();

                    if (isThermal(path)) {
                        applyThermalAnomaly(level, player, pos, isSteam(path), now);
                    }

                    if (isZeroGravity(path)) {
                        // Сам эффект Нульгравы считается отдельно в колонне 3x3x64.
                        // Здесь оставляем только серверные частицы возле найденного блока.
                        spawnZeroGravityParticles(level, pos);
                    }

                    if (isCobra(path)) {
                        applyCobra(level, player, pos);
                    }

                    if (isToxicMoss(path)) {
                        applyToxicMoss(level, player, pos);
                    }

                    if (isBubble(path) && isInsideBubble(player, pos)) {
                        insideBubble = true;
                        spawnBubbleParticles(level, pos);
                    }

                    if (isAcid(path)) {
                        applyAcid(level, player, pos);
                    }

                    if (isElectra(path)) {
                        applyElectra(level, player, pos);
                    }

                    if (isVoronka(path)) {
                        applyVoronka(level, player, pos, now);
                    }

                    if (isCarousel(path)) {
                        insideCarousel |= applyCarousel(level, player, pos);
                    }

                    if (isHydraulic(path)) {
                        applyHydraulic(level, player, pos);
                    } else if (isSpringboard(path) || isGenericGravityImpact(path)) {
                        applyImpactGravity(level, player, pos);
                    }
                }
            }
        }

        applyBubbleSlowdown(player, insideBubble);
        applyZeroGravityState(player, insideZeroGravity);
        if (!insideCarousel) {
            CAROUSEL_EXPOSURE_TICKS.remove(player.getUUID());
        }
    }

    private static String czoBlockPath(ServerLevel level, BlockPos pos) {
        var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (id == null || !CZO.MODID.equals(id.getNamespace())) {
            return "";
        }
        return id.getPath().toLowerCase(Locale.ROOT);
    }

    private static boolean isZeroGravity(String path) {
        return path.contains("zero_gravity") || path.contains("null_gravity") || path.contains("nevesomost") || path.contains("невесом");
    }

    private static boolean isCobra(String path) {
        return path.contains("cobra") || path.contains("кобра");
    }

    private static boolean isToxicMoss(String path) {
        return path.contains("obr_116") || path.contains("obr116") || path.contains("toxic_moss") || path.contains("moss") || path.contains("мох");
    }

    private static boolean isBubble(String path) {
        return path.contains("bubble") || path.contains("пузыр");
    }

    private static boolean isSteam(String path) {
        return path.contains("steam") || path.contains("par") || path.contains("пар");
    }

    private static boolean isThermal(String path) {
        return isSteam(path)
                || path.contains("burner")
                || path.contains("inferno")
                || path.contains("zhar")
                || path.contains("жарк")
                || path.contains("fireball");
    }

    private static boolean isAcid(String path) {
        return path.contains("acid") || path.contains("kisel") || path.contains("кисел");
    }

    private static boolean isElectra(String path) {
        return path.contains("electro") || path.contains("electra") || path.contains("электр");
    }

    private static boolean isVoronka(String path) {
        return path.contains("vortex") || path.contains("voronka") || path.contains("funnel") || path.contains("ворон");
    }

    private static boolean isCarousel(String path) {
        return path.contains("carousel") || path.contains("karusel") || path.contains("kарус") || path.contains("vikh") || path.contains("whirl") || path.contains("карус") || path.contains("вихр");
    }

    private static boolean isSpringboard(String path) {
        return path.contains("springboard") || path.contains("trampoline") || path.contains("tramplin") || path.contains("трамп");
    }

    private static boolean isHydraulic(String path) {
        return path.contains("hydraulic") || path.contains("gidrav") || path.contains("гидрав");
    }

    private static boolean isGenericGravityImpact(String path) {
        if (isZeroGravity(path) || isVoronka(path) || isCarousel(path) || isHydraulic(path) || isSpringboard(path)) return false;
        if (isThermal(path) || isAcid(path) || isElectra(path) || isCobra(path) || isToxicMoss(path) || isBubble(path)) return false;
        return path.contains("gravi") || path.contains("grav") || path.contains("gravity") || path.contains("грави");
    }

    private static void applyThermalAnomaly(ServerLevel level, ServerPlayer player, BlockPos pos, boolean steam, long now) {
        double distance = distanceToBlockEdge(player.position(), pos);
        String key = posKey(pos);

        if (steam) {
            // Пар работает только пока игрок прямо в аномалии. Это не 12-секундная жарка.
            if (distance <= 0.22D) {
                spawnSteamColumn(level, pos);
                if (level.getGameTime() % 8L == 0L) {
                    player.hurtServer(level, player.damageSources().generic(), 1.0F);
                }
            }
            return;
        }

        if (distance <= 0.22D && !THERMAL_ACTIVE_UNTIL.containsKey(key) && !THERMAL_COOLDOWN_UNTIL.containsKey(key)) {
            THERMAL_ACTIVE_UNTIL.put(key, now + THERMAL_ACTIVE_TICKS);
        }

        Long activeUntil = THERMAL_ACTIVE_UNTIL.get(key);
        if (activeUntil != null && activeUntil > now) {
            // Главное исправление: столб горит все 12 секунд после триггера,
            // даже если игрок уже вышел из самой клетки аномалии.
            spawnThermalColumn(level, pos);
            if (distance <= 1.45D && level.getGameTime() % 10L == 0L) {
                player.hurtServer(level, player.damageSources().generic(), 2.0F);
            }
        }
    }

    private static void applyImpactGravity(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (distanceToBlockEdge(player.position(), pos) > 0.18D) {
            return;
        }

        String key = player.getUUID() + "@" + pos.asLong();
        if (IMPACT_COOLDOWNS.containsKey(key)) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.38D, 1.06D, motion.z * 0.38D);
        player.hurtServer(level, player.damageSources().generic(), 4.0F);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D, 24, 0.42D, 0.18D, 0.42D, 0.16D);
        IMPACT_COOLDOWNS.put(key, 22);
    }


    private static void applyHydraulic(ServerLevel level, ServerPlayer player, BlockPos pos) {
        // Гидравлика сохраняет поведение трамплина при непосредственном касании.
        if (distanceToBlockEdge(player.position(), pos) <= 0.18D) {
            applyImpactGravity(level, player, pos);
        }

        // Но дополнительно снова дамажит колонну под собой. Новый радиус по ТЗ: 5 блоков вниз.
        Vec3 c = center(pos);
        double dx = player.getX() - c.x;
        double dz = player.getZ() - c.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double below = pos.getY() - player.getY();
        if (flat <= 1.35D && below >= 0.0D && below <= 5.0D) {
            if (level.getGameTime() % 8L == 0L) {
                player.hurtServer(level, player.damageSources().generic(), 3.0F);
            }
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.76D, Math.min(motion.y, -0.34D), motion.z * 0.76D);
            if (level.getGameTime() % 2L == 0L) {
                level.sendParticles(ParticleTypes.CLOUD, c.x, pos.getY() - 0.15D, c.z, 12, 0.55D, 0.35D, 0.55D, 0.10D);
            }
        }
    }

    private static void applyElectra(ServerLevel level, ServerPlayer player, BlockPos pos) {
        double distance = flatDistance(player.position(), center(pos));
        if (distance > 2.4D || Math.abs(player.getY() - pos.getY()) > 2.0D) {
            return;
        }

        spawnElectraBurstParticles(level, pos);

        String key = player.getUUID() + "@electra@" + pos.asLong();
        if (!IMPACT_COOLDOWNS.containsKey(key)) {
            player.hurtServer(level, player.damageSources().generic(), 5.0F);
            IMPACT_COOLDOWNS.put(key, 24);
        }
    }

    private static void applyAcid(ServerLevel level, ServerPlayer player, BlockPos pos) {
        // Кисель/Холодец: теперь работает как контактная химическая аномалия
        // без видимого блока. По логике похож на Электру: задел область —
        // получил вспышку и урон, затем короткая перезарядка. Кулдаун меньше
        // секунды, чтобы лужа ощущалась "кислотной", но не спамила 20 раз/сек.
        //
        // Stage 8F.11: область Киселя сделана честной 3x3 по X/Z, как поле
        // Электры в тестовой сборке: блок аномалии + один блок во все стороны.
        // Проверяем именно hitbox игрока, поэтому в мультиплеере эффект получает
        // только тот игрок, который реально задел кислотную зону.
        if (!isInsideAcidArea3x3(player, pos)) {
            return;
        }

        String key = player.getUUID() + "@acid@" + pos.asLong();
        if (ACID_COOLDOWNS.containsKey(key)) {
            return;
        }

        spawnAcidBurstParticles(level, pos);
        player.hurtServer(level, player.damageSources().generic(), 3.0F);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 55, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0, false, true, true));
        player.setRemainingFireTicks(0);
        ACID_COOLDOWNS.put(key, 8); // 0.4 секунды при 20 TPS.
    }

    private static boolean isInsideAcidArea3x3(ServerPlayer player, BlockPos pos) {
        double minX = pos.getX() - 1.0D;
        double maxX = pos.getX() + 2.0D;
        double minY = pos.getY() - 0.20D;
        double maxY = pos.getY() + 2.15D;
        double minZ = pos.getZ() - 1.0D;
        double maxZ = pos.getZ() + 2.0D;

        return player.getBoundingBox().maxX > minX
                && player.getBoundingBox().minX < maxX
                && player.getBoundingBox().maxY > minY
                && player.getBoundingBox().minY < maxY
                && player.getBoundingBox().maxZ > minZ
                && player.getBoundingBox().minZ < maxZ;
    }

    private static void applyVoronka(ServerLevel level, ServerPlayer player, BlockPos pos, long now) {
        // Воронка: радиус ровно 2 блока от центра, без таймерного урона.
        // Она 10 секунд физически затягивает к центру, затем одним ударом разрывает.
        Vec3 c = center(pos);
        double distance = flatDistance(player.position(), c);
        double vertical = Math.abs(player.getY() - pos.getY());
        String key = posKey(pos);

        boolean inTriggerRadius = distance <= 2.0D && vertical <= 2.75D;
        if (inTriggerRadius && !VORONKA_STARTED_AT.containsKey(key) && !VORONKA_COOLDOWN_UNTIL.containsKey(key)) {
            VORONKA_STARTED_AT.put(key, now);
        }

        Long start = VORONKA_STARTED_AT.get(key);
        if (start == null) {
            return;
        }

        long elapsedTicks = now - start;
        double progress = Math.min(1.0D, elapsedTicks / (double) VORONKA_ACTIVE_TICKS);
        spawnVoronkaParticles(level, pos, progress, VORONKA_ACTIVE_TICKS - (int) elapsedTicks);

        if (elapsedTicks >= VORONKA_ACTIVE_TICKS) {
            if (distance <= 2.45D && vertical <= 3.4D) {
                pullPlayerHardToCenter(player, c, 0.92D);
                oneShotPlayer(level, player);
            }
            level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.35D, c.z, 120, 1.95D, 0.75D, 1.95D, 0.32D);
            level.sendParticles(ParticleTypes.ASH, c.x, c.y + 0.35D, c.z, 95, 1.85D, 0.65D, 1.85D, 0.28D);
            VORONKA_STARTED_AT.remove(key);
            VORONKA_COOLDOWN_UNTIL.put(key, now + VORONKA_COOLDOWN_TICKS);
            return;
        }

        // Тянет только область самой воронки, а не всю округу. Радиус воздействия немного
        // больше радиуса триггера, чтобы уже зацепленный игрок не вываливался из силы.
        if (distance <= 2.45D && vertical <= 3.25D) {
            Vec3 target = new Vec3(c.x, player.getY() + 0.08D, c.z);
            Vec3 toCenter = target.subtract(player.position());
            double length = Math.max(0.08D, toCenter.length());
            Vec3 direction = toCenter.scale(1.0D / length);
            Vec3 motion = player.getDeltaMovement();
            double pull = 0.105D + progress * progress * 0.62D;
            double drag = 0.42D - Math.min(0.18D, progress * 0.14D);

            Vec3 next = motion.scale(drag).add(direction.scale(pull));
            player.setDeltaMovement(next.x, next.y * 0.82D, next.z);
            markVelocityChanged(player);
        }
    }

    private static boolean applyCarousel(ServerLevel level, ServerPlayer player, BlockPos pos) {
        // Карусель/Вихрь: физически закручивает и медленно стягивает к центру.
        Vec3 c = center(pos);
        double distance = flatDistance(player.position(), c);
        double vertical = Math.abs(player.getY() - pos.getY());
        if (distance > 3.35D || vertical > 6.2D) {
            return false;
        }

        int ticks = CAROUSEL_EXPOSURE_TICKS.getOrDefault(player.getUUID(), 0) + 1;
        CAROUSEL_EXPOSURE_TICKS.put(player.getUUID(), ticks);
        spawnCarouselParticles(level, pos, ticks);

        Vec3 delta = player.position().subtract(c);
        double len = Math.max(0.16D, Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        double tangentX = -delta.z / len;
        double tangentZ = delta.x / len;
        double inwardX = -delta.x / len;
        double inwardZ = -delta.z / len;

        double charge = Math.min(1.0D, ticks / 120.0D);

        // Stage 8F.9: раньше Карусель каждую секунду разгоняла игрока всё сильнее.
        // В итоге он набирал слишком большую тангенциальную скорость и сам вылетал
        // из зоны засасывания. Теперь по мере "заряда" аномалии растёт не только
        // вращение/стягивание, но и вязкое торможение + жёсткий лимит горизонтальной
        // скорости. В центре игрока должно всё сильнее вязать, а не катапультировать.
        double centerFactor = clamp(1.0D - len / 3.35D, 0.0D, 1.0D);
        double viscosity = 0.58D - charge * 0.26D - centerFactor * 0.16D;
        viscosity = clamp(viscosity, 0.20D, 0.58D);

        double spin = 0.11D + charge * 0.18D;
        double pull = 0.045D + charge * 0.20D + centerFactor * 0.055D;
        double lift = 0.018D + charge * 0.10D;
        Vec3 motion = player.getDeltaMovement();

        double nextX = motion.x * viscosity + tangentX * spin + inwardX * pull;
        double nextZ = motion.z * viscosity + tangentZ * spin + inwardZ * pull;
        double horizontalSpeed = Math.sqrt(nextX * nextX + nextZ * nextZ);
        double maxHorizontalSpeed = 0.82D - charge * 0.34D - centerFactor * 0.14D;
        maxHorizontalSpeed = clamp(maxHorizontalSpeed, 0.26D, 0.82D);
        if (horizontalSpeed > maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / Math.max(0.0001D, horizontalSpeed);
            nextX *= scale;
            nextZ *= scale;
        }

        double nextY = motion.y * (0.50D - charge * 0.16D) + lift;
        double maxUpSpeed = 0.42D - charge * 0.16D;
        nextY = clamp(nextY, -0.18D, maxUpSpeed);

        Vec3 next = new Vec3(nextX, nextY, nextZ);
        player.setDeltaMovement(next);
        markVelocityChanged(player);

        if (ticks > 100 && level.getGameTime() % 20L == 0L) {
            player.hurtServer(level, player.damageSources().generic(), 1.0F);
        }
        return true;
    }

    private static void applyZeroGravity(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 motion = player.getDeltaMovement();
        double newY = motion.y;

        // Нульграва теперь не просто кидает Slow Falling, а реально гасит обычную гравитацию
        // только у конкретного игрока внутри вертикальной колонны 3x3x64 над аномалией.
        if (motion.y < -0.015D) {
            newY = motion.y * 0.16D;
        } else if (motion.y > 0.14D) {
            newY = 0.14D + (motion.y - 0.14D) * 0.35D;
        } else if (!player.onGround()) {
            newY = motion.y * 0.92D + 0.003D;
        }

        player.setDeltaMovement(motion.x * 0.992D, newY, motion.z * 0.992D);
        player.fallDistance = 0.0F;
        markVelocityChanged(player);
    }

    private static boolean applyZeroGravityColumnIfInside(ServerLevel level, ServerPlayer player) {
        BlockPos base = player.blockPosition();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();

        int minY = Math.max(level.getMinY(), base.getY() - ZERO_GRAVITY_HEIGHT_UP - 2);
        int maxY = Math.min(level.getMinY() + level.getHeight() - 1, base.getY() + 1);

        for (int dx = -ZERO_GRAVITY_SCAN_HORIZONTAL; dx <= ZERO_GRAVITY_SCAN_HORIZONTAL; dx++) {
            for (int dz = -ZERO_GRAVITY_SCAN_HORIZONTAL; dz <= ZERO_GRAVITY_SCAN_HORIZONTAL; dz++) {
                for (int y = maxY; y >= minY; y--) {
                    scan.set(base.getX() + dx, y, base.getZ() + dz);

                    String path = czoBlockPath(level, scan);
                    if (path.isEmpty() || !path.contains("anomaly") || !isZeroGravity(path)) {
                        continue;
                    }

                    BlockPos anomalyPos = scan.immutable();
                    if (isInsideZeroGravityColumn(player, anomalyPos)) {
                        applyZeroGravity(level, player, anomalyPos);
                        spawnZeroGravityParticles(level, anomalyPos);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isInsideZeroGravityColumn(ServerPlayer player, BlockPos pos) {
        // Колонна 3x3 по блокам: от pos.x - 1 до pos.x + 1,
        // от pos.z - 1 до pos.z + 1, вверх на 64 блока от Y аномалии.
        double minX = pos.getX() - ZERO_GRAVITY_HALF_WIDTH;
        double maxX = pos.getX() + ZERO_GRAVITY_HALF_WIDTH + 1.0D;
        double minY = pos.getY();
        double maxY = pos.getY() + ZERO_GRAVITY_HEIGHT_UP;
        double minZ = pos.getZ() - ZERO_GRAVITY_HALF_WIDTH;
        double maxZ = pos.getZ() + ZERO_GRAVITY_HALF_WIDTH + 1.0D;

        return player.getBoundingBox().maxX > minX
                && player.getBoundingBox().minX < maxX
                && player.getBoundingBox().maxY > minY
                && player.getBoundingBox().minY < maxY
                && player.getBoundingBox().maxZ > minZ
                && player.getBoundingBox().minZ < maxZ;
    }

    private static void applyZeroGravityState(ServerPlayer player, boolean insideZeroGravity) {
        if (!insideZeroGravity) {
            clearZeroGravity(player);
            return;
        }

        UUID playerId = player.getUUID();
        if (!player.isNoGravity()) {
            ZERO_GRAVITY_PLAYERS.add(playerId);
        }

        player.setNoGravity(true);
        player.fallDistance = 0.0F;
    }

    private static void clearZeroGravity(ServerPlayer player) {
        if (ZERO_GRAVITY_PLAYERS.remove(player.getUUID())) {
            player.setNoGravity(false);
            markVelocityChanged(player);
        }
    }

    private static void applyCobra(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (player.isShiftKeyDown()) {
            return;
        }

        if (flatDistance(player.position(), center(pos)) > 4.0D) {
            return;
        }

        String key = player.getUUID() + "@" + pos.asLong();
        if (COBRA_COOLDOWNS.containsKey(key)) {
            return;
        }

        float damage = Math.max(1.0F, player.getMaxHealth() * 0.30F);
        player.hurtServer(level, player.damageSources().generic(), damage);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, true, true));
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0D, player.getZ(), 18, 0.35D, 0.55D, 0.35D, 0.08D);
        COBRA_COOLDOWNS.put(key, 80);
    }

    private static void applyToxicMoss(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (distanceToBlockEdge(player.position(), pos) > 0.18D) {
            return;
        }

        if (level.getGameTime() % 20L == 0L) {
            player.hurtServer(level, player.damageSources().generic(), 2.0F);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        }
    }

    private static void applyBubbleSlowdown(ServerPlayer player, boolean insideBubble) {
        UUID playerId = player.getUUID();
        if (!insideBubble) {
            BUBBLE_EXPOSURE_TICKS.remove(playerId);
            return;
        }

        int ticks = BUBBLE_EXPOSURE_TICKS.getOrDefault(playerId, 0) + 1;
        BUBBLE_EXPOSURE_TICKS.put(playerId, ticks);

        int seconds = Math.min(10, ticks / 20);
        double slowPercent = Math.min(0.96D, 0.02D * Math.pow(2.0D, seconds));
        int amplifier = Math.max(0, Math.min(5, (int) Math.floor(slowPercent * 7.0D)));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, amplifier, false, true, true));
    }

    private static boolean isInsideBubble(ServerPlayer player, BlockPos pos) {
        return flatDistance(player.position(), center(pos)) <= 6.0D && Math.abs(player.getY() - pos.getY()) <= 6.0D;
    }

    private static void spawnAcidBurstParticles(ServerLevel level, BlockPos pos) {
        Vec3 c = center(pos);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.36F, 1.0F, 0.10F),
                c.x,
                pos.getY() + 0.14D,
                c.z,
                18,
                0.95D,
                0.10D,
                0.95D,
                0.035D
        );
        level.sendParticles(ParticleTypes.ITEM_SLIME, c.x, pos.getY() + 0.12D, c.z, 14, 0.82D, 0.08D, 0.82D, 0.055D);
        level.sendParticles(ParticleTypes.COMPOSTER, c.x, pos.getY() + 0.18D, c.z, 10, 0.74D, 0.16D, 0.74D, 0.025D);
        level.sendParticles(ParticleTypes.SMOKE, c.x, pos.getY() + 0.34D, c.z, 6, 0.65D, 0.22D, 0.65D, 0.018D);
    }

    private static void spawnElectraBurstParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D, 48, 1.15D, 0.72D, 1.15D, 0.18D);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.18F, 0.78F, 1.0F),
                pos.getX() + 0.5D,
                pos.getY() + 0.65D,
                pos.getZ() + 0.5D,
                26,
                0.95D,
                0.52D,
                0.95D,
                0.03D
        );
    }

    private static void spawnZeroGravityParticles(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 16L != 0L) return;
        level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 1.25D + level.getRandom().nextDouble() * 10.0D, pos.getZ() + 0.5D, 1, 1.8D, 3.0D, 1.8D, 0.003D);
    }

    private static void spawnBubbleParticles(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 8L != 0L) return;
        level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D, 4, 4.2D, 1.8D, 4.2D, 0.003D);
        level.sendParticles(ParticleTypes.DUST_PLUME, pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 0.5D, 3, 3.2D, 0.35D, 3.2D, 0.010D);
    }

    private static void spawnThermalColumn(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 2L != 0L) return;
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D, 28, 0.46D, 2.25D, 0.46D, 0.075D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 1.65D, pos.getZ() + 0.5D, 8, 0.42D, 1.35D, 0.42D, 0.035D);
    }

    private static void spawnSteamColumn(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 2L != 0L) return;
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 18, 0.45D, 1.65D, 0.45D, 0.055D);
    }

    private static void spawnVoronkaParticles(ServerLevel level, BlockPos pos, double progress, int remaining) {
        int green = 8 + (int) (progress * 16.0D);
        int dust = 16 + (int) (progress * 38.0D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, green, 2.15D, 0.72D, 2.15D, 0.022D);
        level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D, dust, 2.4D, 0.75D, 2.4D, 0.038D);
        if (remaining <= 12) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D, 38, 0.16D, 0.2D, 0.16D, 0.24D);
        }
    }

    private static void spawnCarouselParticles(ServerLevel level, BlockPos pos, int ticks) {
        if (level.getGameTime() % 2L != 0L) return;
        double baseAngle = ticks * 0.52D;
        double lift = Math.min(6.8D, ticks * 0.045D);
        for (int i = 0; i < 7; i++) {
            double angle = baseAngle + i * Math.PI * 0.5D;
            double radius = 1.05D + 0.22D * Math.sin(ticks * 0.07D + i);
            double y = 0.35D + lift + i * 0.16D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.ASH, x, pos.getY() + y, z, 2, 0.055D, 0.055D, 0.055D, 0.010D);
        }
    }

    private static void pullPlayerHardToCenter(ServerPlayer player, Vec3 center, double strength) {
        Vec3 target = new Vec3(center.x, player.getY() + 0.05D, center.z);
        Vec3 toCenter = target.subtract(player.position());
        double length = Math.max(0.08D, toCenter.length());
        Vec3 direction = toCenter.scale(1.0D / length);
        player.setDeltaMovement(direction.x * strength, Math.max(0.0D, direction.y * strength), direction.z * strength);
        markVelocityChanged(player);
    }

    private static void oneShotPlayer(ServerLevel level, ServerPlayer player) {
        float damage = Math.max(1000.0F, player.getMaxHealth() * 50.0F);
        player.hurtServer(level, player.damageSources().generic(), damage);
        if (player.isAlive()) {
            player.setHealth(0.0F);
        }
    }

    private static void markVelocityChanged(Entity entity) {
        // Разные mappings держат флаг синхронизации скорости под разными именами.
        // Reflection не ломает компиляцию, но помогает серверу быстрее протолкнуть motion клиенту.
        trySetBooleanField(entity, "hasImpulse", true);
        trySetBooleanField(entity, "hurtMarked", true);
    }

    private static void trySetBooleanField(Entity entity, String name, boolean value) {
        try {
            java.lang.reflect.Field field = Entity.class.getField(name);
            field.setBoolean(entity, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static double flatDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceToBlockEdge(Vec3 point, BlockPos pos) {
        double dx = Math.max(0.0D, Math.abs(point.x - (pos.getX() + 0.5D)) - 0.5D);
        double dy = Math.max(0.0D, Math.abs(point.y - (pos.getY() + 0.5D)) - 0.5D);
        double dz = Math.max(0.0D, Math.abs(point.z - (pos.getZ() + 0.5D)) - 0.5D);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String posKey(BlockPos pos) {
        return Long.toString(pos.asLong());
    }
}
