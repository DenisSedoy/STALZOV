package com.czo.contamination;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ContaminationProtection {
    private ContaminationProtection() {
    }

    /**
     * Будущий хук под костюмы, респираторы, артефакты и медикаменты.
     *
     * Значение защиты считается в условных единицах:
     * 100 = полная защита от зоны 1 уровня,
     * 200 = полная защита от зоны 2 уровня,
     * 90 = 90% защиты от зоны 1 уровня.
     *
     * Важно: метод принимает базового Player, чтобы клиентский детектор звуков
     * тоже мог посчитать защиту по экипировке, когда мы её добавим. Серверный
     * ServerPlayer-вариант ниже оставлен для старого кода и делегирует сюда.
     */
    public static int getProtection(Player player, ContaminationType type) {
        return 0;
    }

    public static int getProtection(ServerPlayer player, ContaminationType type) {
        return getProtection((Player) player, type);
    }
}
