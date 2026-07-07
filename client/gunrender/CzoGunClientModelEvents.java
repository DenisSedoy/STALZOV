package com.czo.client.gunrender;

import com.czo.CZO;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

@EventBusSubscriber(modid = CZO.MODID, value = Dist.CLIENT)
public final class CzoGunClientModelEvents {
    private CzoGunClientModelEvents() {
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(CzoGunSpecialItemModel.ID, CzoGunSpecialItemModel.MAP_CODEC);
    }
}
