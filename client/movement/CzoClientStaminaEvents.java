package com.czo.client.movement;

import com.czo.CZO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Client-side stamina prediction and temporary prone control.
 * Server safety layer lives in CzoPlayerHealthServerEvents; this class drives HUD and input feel.
 */
@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoClientStaminaEvents {
    private static final double MAX_STAMINA = 100.0D;
    private static final double JUMP_COST = 20.0D;
    private static final double SPRINT_COST_PER_TICK = 5.0D / 20.0D;
    private static final double STAND_REGEN_PER_TICK = 10.0D / 20.0D;
    private static final double WALK_REGEN_PER_TICK = 5.0D / 20.0D;
    private static final double PRONE_ENTER_COST = 15.0D;
    private static final double PRONE_EXIT_COST = 10.0D;
    private static final double UNLOCK_STAMINA = 20.0D;

    private static double stamina = MAX_STAMINA;
    private static boolean exhaustedLock;
    private static boolean prone;
    private static boolean wasOnGround = true;
    private static boolean lastZDown;
    private static boolean lastSpaceDown;

    private CzoClientStaminaEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            stamina = MAX_STAMINA;
            exhaustedLock = false;
            prone = false;
            lastZDown = false;
            lastSpaceDown = false;
            return;
        }

        tickProneKey(minecraft, player);
        tickStamina(minecraft, player);
        applyPronePose(player);
    }

    public static float staminaRatio(Player ignored) {
        return (float) clamp(stamina / MAX_STAMINA, 0.0D, 1.0D);
    }

    public static boolean isProne() {
        return prone;
    }

    private static void tickProneKey(Minecraft minecraft, LocalPlayer player) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) {
            return;
        }

        boolean zDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS;
        boolean spaceDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

        if (zDown && !lastZDown) {
            if (!prone && stamina >= PRONE_ENTER_COST && !exhaustedLock) {
                stamina -= PRONE_ENTER_COST;
                prone = true;
            } else if (prone && stamina >= PRONE_EXIT_COST) {
                stamina -= PRONE_EXIT_COST;
                prone = false;
                clearPronePose(player);
            }
        }

        // Лёжа: Space теперь не прыгает, а выводит игрока из prone.
        if (spaceDown && !lastSpaceDown && prone && stamina >= PRONE_EXIT_COST) {
            stamina -= PRONE_EXIT_COST;
            prone = false;
            clearPronePose(player);
            trySetOptionKey(minecraft.options, "keyJump", false);
        }

        lastZDown = zDown;
        lastSpaceDown = spaceDown;
    }

    private static void tickStamina(Minecraft minecraft, LocalPlayer player) {
        boolean onGround = player.onGround();
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        boolean jumped = wasOnGround && !onGround && motion.y > 0.10D;
        if (jumped) {
            consume(JUMP_COST);
        }

        if (player.isSprinting() && horizontal > 0.035D) {
            consume(SPRINT_COST_PER_TICK);
        } else if (onGround) {
            stamina = Math.min(MAX_STAMINA, stamina + (horizontal < 0.025D ? STAND_REGEN_PER_TICK : WALK_REGEN_PER_TICK));
        }

        if (exhaustedLock && stamina > UNLOCK_STAMINA) {
            exhaustedLock = false;
        }

        if (exhaustedLock || stamina <= 0.0D) {
            exhaustedLock = true;
            player.setSprinting(false);
            trySetOptionKey(minecraft.options, "keySprint", false);
            trySetOptionKey(minecraft.options, "keyJump", false);
        }

        if (prone) {
            player.setSprinting(false);
            trySetOptionKey(minecraft.options, "keySprint", false);
        }

        stamina = clamp(stamina, 0.0D, MAX_STAMINA);
        wasOnGround = onGround;
    }

    private static void consume(double amount) {
        stamina = Math.max(0.0D, stamina - Math.max(0.0D, amount));
        if (stamina <= 0.0D) {
            exhaustedLock = true;
        }
    }

    private static void applyPronePose(LocalPlayer player) {
        if (!prone) {
            return;
        }
        invokePoseSetter(player, true);
    }

    private static void clearPronePose(LocalPlayer player) {
        invokePoseSetter(player, false);
    }

    private static void invokePoseSetter(LocalPlayer player, boolean enable) {
        Pose targetPose = enable ? Pose.SWIMMING : Pose.STANDING;
        Class<?> type = player.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!("setForcedPose".equals(method.getName()) || "setPose".equals(method.getName()))) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Class<?> parameter = method.getParameterTypes()[0];
                    if (parameter == Pose.class) {
                        method.invoke(player, targetPose);
                        return;
                    }
                    if (parameter == Optional.class) {
                        method.invoke(player, enable ? Optional.of(Pose.SWIMMING) : Optional.empty());
                        return;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            type = type.getSuperclass();
        }

        setField(player, "forcedPose", enable ? Pose.SWIMMING : null);
    }

    private static void trySetOptionKey(Object options, String fieldName, boolean down) {
        if (options == null) {
            return;
        }
        try {
            Field field = options.getClass().getField(fieldName);
            Object keyMapping = field.get(options);
            if (keyMapping == null) {
                return;
            }
            Method method = keyMapping.getClass().getMethod("setDown", boolean.class);
            method.invoke(keyMapping, down);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
