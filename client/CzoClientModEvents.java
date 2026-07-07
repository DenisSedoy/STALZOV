package com.czo.client;

import com.czo.CZO;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;

@EventBusSubscriber(
        modid = CZO.MODID,
        value = Dist.CLIENT
)
public final class CzoClientModEvents {
    private CzoClientModEvents() {
    }

    @SubscribeEvent
    public static void registerConditionalItemModelProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(CZO.MODID, "aiming"),
                AimingItemModelProperty.MAP_CODEC
        );
    }
}