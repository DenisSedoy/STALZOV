package com.czo.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

/** Client-side 2-block raycast helper for manual CZO item pickup. */
public final class CzoWorldItemRaycast {
    private CzoWorldItemRaycast() {
    }

    public static Optional<ItemEntity> findLookedAtItem(LocalPlayer player, double range) {
        if (player == null || player.level() == null) {
            return Optional.empty();
        }

        Vec3 from = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(0.75D);

        return player.level()
                .getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive() && !item.getItem().isEmpty())
                .stream()
                .map(item -> new Hit(item, hitDistanceSqr(item, from, to)))
                .filter(hit -> hit.distanceSqr() >= 0.0D)
                .min(Comparator.comparingDouble(Hit::distanceSqr))
                .map(Hit::item);
    }

    private static double hitDistanceSqr(ItemEntity item, Vec3 from, Vec3 to) {
        AABB box = item.getBoundingBox().inflate(0.25D);
        if (box.contains(from)) {
            return 0.0D;
        }
        return box.clip(from, to)
                .map(from::distanceToSqr)
                .orElse(-1.0D);
    }

    private record Hit(ItemEntity item, double distanceSqr) {
    }
}
