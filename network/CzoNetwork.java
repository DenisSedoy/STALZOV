package com.czo.network;

import com.czo.CZO;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CZO.MODID)
public final class CzoNetwork {
    private CzoNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ServerboundFireGunPacket.TYPE,
                ServerboundFireGunPacket.STREAM_CODEC,
                ServerboundFireGunPacket::handle
        );

        registrar.playToServer(
                ServerboundReloadGunPacket.TYPE,
                ServerboundReloadGunPacket.STREAM_CODEC,
                ServerboundReloadGunPacket::handle
        );

        registrar.playToServer(
                ServerboundSetGunModulePacket.TYPE,
                ServerboundSetGunModulePacket.STREAM_CODEC,
                ServerboundSetGunModulePacket::handle
        );

        registrar.playToServer(
                ServerboundSetSelectedAmmoPacket.TYPE,
                ServerboundSetSelectedAmmoPacket.STREAM_CODEC,
                ServerboundSetSelectedAmmoPacket::handle
        );

        registrar.playToServer(
                ServerboundSetInventoryGridPositionPacket.TYPE,
                ServerboundSetInventoryGridPositionPacket.STREAM_CODEC,
                ServerboundSetInventoryGridPositionPacket::handle
        );

        registrar.playToServer(
                ServerboundSortInventoryGridPacket.TYPE,
                ServerboundSortInventoryGridPacket.STREAM_CODEC,
                ServerboundSortInventoryGridPacket::handle
        );

        registrar.playToServer(
                ServerboundEquipFromInventoryPacket.TYPE,
                ServerboundEquipFromInventoryPacket.STREAM_CODEC,
                ServerboundEquipFromInventoryPacket::handle
        );

        registrar.playToServer(
                ServerboundUnequipToBackpackPacket.TYPE,
                ServerboundUnequipToBackpackPacket.STREAM_CODEC,
                ServerboundUnequipToBackpackPacket::handle
        );

        registrar.playToServer(
                ServerboundUnequipToGridPositionPacket.TYPE,
                ServerboundUnequipToGridPositionPacket.STREAM_CODEC,
                ServerboundUnequipToGridPositionPacket::handle
        );

        registrar.playToServer(
                ServerboundQuickEquipFromInventoryPacket.TYPE,
                ServerboundQuickEquipFromInventoryPacket.STREAM_CODEC,
                ServerboundQuickEquipFromInventoryPacket::handle
        );

        registrar.playToServer(
                ServerboundManualPickupItemPacket.TYPE,
                ServerboundManualPickupItemPacket.STREAM_CODEC,
                ServerboundManualPickupItemPacket::handle
        );

        registrar.playToServer(
                ServerboundDropBackpackStackPacket.TYPE,
                ServerboundDropBackpackStackPacket.STREAM_CODEC,
                ServerboundDropBackpackStackPacket::handle
        );

        registrar.playToServer(
                ServerboundSetCzoSelectedSlotPacket.TYPE,
                ServerboundSetCzoSelectedSlotPacket.STREAM_CODEC,
                ServerboundSetCzoSelectedSlotPacket::handle
        );
    }
}
