package com.czo.player;

import com.czo.CZO;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CZO player baseline: 100 HP, faster CZO regen, no vanilla post-hit immunity,
 * and a lightweight server stamina safety layer.
 */
@EventBusSubscriber(modid = CZO.MODID)
public final class CzoPlayerHealthServerEvents {
    private static final double BASE_MAX_HEALTH = 100.0D;
    private static final float REGEN_PER_SECOND = 10.0F;

    private static final double MAX_STAMINA = 100.0D;
    private static final double JUMP_COST = 20.0D;
    private static final double SPRINT_COST_PER_TICK = 5.0D / 20.0D;
    private static final double STAND_REGEN_PER_TICK = 10.0D / 20.0D;
    private static final double WALK_REGEN_PER_TICK = 5.0D / 20.0D;
    private static final double UNLOCK_STAMINA = 20.0D;

    private static final Map<UUID, StaminaState> STAMINA = new HashMap<>();

    private CzoPlayerHealthServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            STAMINA.remove(player.getUUID());
            return;
        }

        ensureBaseHealth(player);
        clearVanillaDamageImmunity(player);
        tickHealthRegen(player);
        tickServerStamina(player);
    }

    private static void tickHealthRegen(ServerPlayer player) {
        if (player.level().getGameTime() % 20L == 0L && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + REGEN_PER_SECOND));
        }
    }

    private static void tickServerStamina(ServerPlayer player) {
        UUID id = player.getUUID();
        StaminaState state = STAMINA.computeIfAbsent(id, ignored -> new StaminaState());
        boolean onGround = player.onGround();
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        boolean jumped = state.wasOnGround && !onGround && motion.y > 0.10D;
        if (jumped) {
            consume(state, JUMP_COST);
        }

        if (player.isSprinting() && horizontal > 0.035D) {
            consume(state, SPRINT_COST_PER_TICK);
        } else if (onGround) {
            double regen = horizontal < 0.025D ? STAND_REGEN_PER_TICK : WALK_REGEN_PER_TICK;
            state.value = Math.min(MAX_STAMINA, state.value + regen);
        }

        if (state.locked && state.value > UNLOCK_STAMINA) {
            state.locked = false;
        }

        if (state.locked || state.value <= 0.0D) {
            state.locked = true;
            player.setSprinting(false);
        }

        state.value = clamp(state.value, 0.0D, MAX_STAMINA);
        state.wasOnGround = onGround;
    }

    private static void consume(StaminaState state, double amount) {
        state.value = Math.max(0.0D, state.value - Math.max(0.0D, amount));
        if (state.value <= 0.0D) {
            state.locked = true;
        }
    }

    private static void ensureBaseHealth(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && Math.abs(maxHealth.getBaseValue() - BASE_MAX_HEALTH) > 0.001D) {
            maxHealth.setBaseValue(BASE_MAX_HEALTH);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void clearVanillaDamageImmunity(ServerPlayer player) {
        setIntField(player, "invulnerableTime", 0);
        setIntField(player, "hurtTime", 0);
        setIntField(player, "hurtDuration", 0);
    }

    private static void setIntField(Object target, String fieldName, int value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                if (field.getType() == int.class) {
                    field.setInt(target, value);
                }
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class StaminaState {
        private double value = MAX_STAMINA;
        private boolean locked;
        private boolean wasOnGround = true;
    }
}
