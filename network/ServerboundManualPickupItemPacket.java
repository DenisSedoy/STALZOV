package com.czo.network;

import com.czo.CZO;
import com.czo.inventory.grid.CzoBackpackItemTransfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Manual F-pickup for CZO backpack. Vanilla auto-pickup is disabled separately by mixin. */
public record ServerboundManualPickupItemPacket(int entityId) implements CustomPacketPayload {
    private static final double PICKUP_RANGE = 2.0D;
    private static final double PICKUP_RANGE_SQR = PICKUP_RANGE * PICKUP_RANGE + 0.25D;

    public static final Type<ServerboundManualPickupItemPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CZO.MODID, "manual_pickup_item"));

    public static final StreamCodec<ByteBuf, ServerboundManualPickupItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundManualPickupItemPacket::entityId,
                    ServerboundManualPickupItemPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundManualPickupItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }

            Entity entity = level.getEntity(packet.entityId());
            if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                return;
            }
            if (player.distanceToSqr(itemEntity) > PICKUP_RANGE_SQR || !isLookingAt(player, itemEntity)) {
                warn(player, "Нужно смотреть на предмет ближе 2 блоков");
                return;
            }

            ItemStack worldStack = itemEntity.getItem();
            if (worldStack.isEmpty()) {
                itemEntity.discard();
                return;
            }

            ItemStack before = worldStack.copy();
            Item item = worldStack.getItem();
            int inserted = CzoBackpackItemTransfer.insertIntoBackpack(player, worldStack);
            if (inserted <= 0) {
                warn(player, "В рюкзаке нет места");
                return;
            }

            if (worldStack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(worldStack);
            }

            player.take(itemEntity, inserted);
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), inserted);
            player.onItemPickup(itemEntity);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static boolean isLookingAt(ServerPlayer player, ItemEntity itemEntity) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(PICKUP_RANGE));
        AABB box = itemEntity.getBoundingBox().inflate(0.25D);
        return box.contains(from) || box.clip(from, to).isPresent();
    }

    private static void warn(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}
