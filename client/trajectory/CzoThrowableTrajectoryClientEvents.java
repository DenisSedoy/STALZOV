package com.czo.client.trajectory;

import com.czo.CZO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Temporary transparent-ish trajectory helper for throwable anomaly activators.
 * Currently bound to vanilla snowball until the real bolt item exists.
 */
@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoThrowableTrajectoryClientEvents {
    private static int tick;

    private CzoThrowableTrajectoryClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        if (!isHoldingSnowball(player)) {
            return;
        }

        tick++;
        if ((tick & 1) != 0) {
            return;
        }

        Vec3 start = player.getEyePosition(1.0F).add(0.0D, -0.08D, 0.0D);
        Vec3 velocity = player.getLookAngle().normalize().scale(1.45D);
        double gravity = 0.03D;

        for (int i = 1; i <= 18; i++) {
            double t = i * 0.38D;
            double x = start.x + velocity.x * t;
            double y = start.y + velocity.y * t - gravity * t * t;
            double z = start.z + velocity.z * t;
            minecraft.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean isHoldingSnowball(LocalPlayer player) {
        return isSnowball(player.getMainHandItem()) || isSnowball(player.getOffhandItem());
    }

    private static boolean isSnowball(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SNOWBALL;
    }
}
