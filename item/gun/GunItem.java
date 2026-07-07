package com.czo.item.gun;

import com.czo.CZO;
import com.czo.item.gun.ammo.CzoAmmo;
import com.czo.item.gun.ammo.CzoAmmoDefinition;
import com.czo.item.gun.definition.CzoGuns;
import com.czo.item.gun.definition.GunDefinition;
import com.czo.item.gun.definition.GunStats;
import com.czo.item.gun.module.CzoGunModules;
import com.czo.item.gun.module.GunModule;
import com.czo.item.gun.module.GunModuleSlot;
import com.czo.registry.CzoDataComponents;
import com.czo.inventory.grid.CzoBackpackItemTransfer;
import com.czo.inventory.grid.CzoStackCounts;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GunItem extends Item {
    private final String defaultGunId;
    private final Map<UUID, Long> nextShotTick = new ConcurrentHashMap<>();

    public GunItem(Properties properties) {
        this(properties, CzoGuns.PM);
    }

    public GunItem(Properties properties, String defaultGunId) {
        super(properties);
        this.defaultGunId = defaultGunId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // ПКМ не стреляет. ПКМ используется клиентом как aim.
        return InteractionResult.CONSUME;
    }

    private GunDefinition getGunDefinition(ItemStack stack) {
        String gunId = stack.get(CzoDataComponents.GUN_ID.get());

        if (gunId == null || gunId.isBlank()) {
            stack.set(CzoDataComponents.GUN_ID.get(), defaultGunId);
            return CzoGuns.get(defaultGunId);
        }

        return CzoGuns.get(gunId);
    }

    public GunStats getEffectiveStats(ItemStack stack) {
        return getEffectiveStatsWithPreview(stack, null, null);
    }

    public GunStats getEffectiveStatsWithPreview(ItemStack stack, String previewSlotId, String previewModuleId) {
        GunDefinition gun = getGunDefinition(stack);
        GunModule magazine = getMagazineForPreview(stack, previewSlotId, previewModuleId);

        GunStats stats = withReloadTicks(gun.stats(), magazine.reloadTicks());

        stats = applyInstalledModuleStats(stats, stack, "muzzle", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "optic", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "stock", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "grip", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "barrel", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "handguard", previewSlotId, previewModuleId);
        stats = applyInstalledModuleStats(stats, stack, "charm", previewSlotId, previewModuleId);

        CzoAmmoDefinition ammo = CzoAmmo.getOrDefault(getAmmoIdForStats(stack, gun), gun);
        return ammo.applyTo(stats);
    }

    private GunStats applyInstalledModuleStats(
            GunStats stats,
            ItemStack stack,
            String slotId,
            String previewSlotId,
            String previewModuleId
    ) {
        String moduleId = getInstalledModuleIdForStats(stack, slotId);

        if (slotId.equals(previewSlotId)) {
            moduleId = previewModuleId;
        }

        if (moduleId == null || moduleId.isBlank() || CzoGunModules.NONE.equals(moduleId)) {
            return stats;
        }

        GunModule module = CzoGunModules.getModule(moduleId);

        if (module == null || module.isMagazine()) {
            return stats;
        }

        return module.applyTo(stats);
    }

    private GunModule getMagazineForPreview(ItemStack stack, String previewSlotId, String previewModuleId) {
        if ("magazine".equals(previewSlotId)) {
            return CzoGunModules.getMagazine(previewModuleId);
        }

        return getMagazine(stack);
    }

    private String getInstalledModuleIdForStats(ItemStack stack, String slotId) {
        String moduleId = switch (slotId) {
            case "magazine" -> stack.get(CzoDataComponents.MAGAZINE_ID.get());
            case "muzzle" -> stack.get(CzoDataComponents.MUZZLE_ID.get());
            case "optic" -> stack.get(CzoDataComponents.OPTIC_ID.get());
            case "stock" -> stack.get(CzoDataComponents.STOCK_ID.get());
            case "grip" -> stack.get(CzoDataComponents.GRIP_ID.get());
            case "barrel" -> stack.get(CzoDataComponents.BARREL_ID.get());
            case "handguard" -> stack.get(CzoDataComponents.HANDGUARD_ID.get());
            case "charm" -> stack.get(CzoDataComponents.CHARM_ID.get());
            case "laser" -> stack.get(CzoDataComponents.LASER_ID.get());
            default -> CzoGunModules.NONE;
        };

        if (moduleId == null || moduleId.isBlank() || CzoGunModules.NONE.equals(moduleId)) {
            GunModuleSlot slot = GunModuleSlot.fromId(slotId).orElse(null);
            return slot == null ? CzoGunModules.NONE : CzoGunModules.getDefaultModuleIdForSlot(slot);
        }

        return moduleId;
    }

    private GunStats withReloadTicks(GunStats stats, int reloadTicks) {
        return new GunStats(
                stats.damage(),
                stats.range(),
                stats.rpm(),
                Math.max(1, reloadTicks),
                stats.hipSpreadDegrees(),
                stats.aimSpreadDegrees(),
                stats.recoilVertical(),
                stats.recoilHorizontal(),
                stats.muzzleVelocity(),
                stats.gravity(),
                stats.drag()
        );
    }

    public int getHudMagazineCapacity(ItemStack stack) {
        return getMagazineCapacity(stack);
    }

    public int getHudMagazineCapacityWithPreview(ItemStack stack, String previewSlotId, String previewModuleId) {
        return getMagazineForPreview(stack, previewSlotId, previewModuleId).magazineCapacity();
    }

    public int getHudReloadTicks(ItemStack stack) {
        return getEffectiveStats(stack).baseReloadTicks();
    }

    public double getHudReloadSeconds(ItemStack stack) {
        return ticksToSeconds(getHudReloadTicks(stack));
    }

    public static double ticksToSeconds(int ticks) {
        return ticks / 20.0D;
    }

    public String getSelectedAmmoId(ItemStack stack) {
        return getSelectedAmmoId(stack, getGunDefinition(stack));
    }

    private String getSelectedAmmoId(ItemStack stack, GunDefinition gun) {
        String ammoId = stack.get(CzoDataComponents.SELECTED_AMMO_ID.get());

        if (ammoId == null || ammoId.isBlank() || !CzoAmmo.isCompatible(gun, ammoId)) {
            ammoId = gun.ammoItemId();
            stack.set(CzoDataComponents.SELECTED_AMMO_ID.get(), ammoId);
        }

        return ammoId;
    }

    private String getLoadedAmmoId(ItemStack stack, GunDefinition gun) {
        String ammoId = stack.get(CzoDataComponents.LOADED_AMMO_ID.get());

        if (ammoId == null || ammoId.isBlank() || !CzoAmmo.isCompatible(gun, ammoId)) {
            ammoId = getSelectedAmmoId(stack, gun);
            stack.set(CzoDataComponents.LOADED_AMMO_ID.get(), ammoId);
        }

        return ammoId;
    }

    private void setLoadedAmmoId(ItemStack stack, String ammoId) {
        stack.set(CzoDataComponents.LOADED_AMMO_ID.get(), ammoId);
    }

    private String getAmmoIdForStats(ItemStack stack, GunDefinition gun) {
        // Если в оружии уже есть патроны, стреляем и считаем статы именно по реально заряженному типу.
        // Если оружие пустое, показываем статы выбранного для следующей зарядки типа.
        return getLoadedAmmoCount(stack) > 0 ? getLoadedAmmoId(stack, gun) : getSelectedAmmoId(stack, gun);
    }

    private int getLoadedAmmoCount(ItemStack stack) {
        int capacity = getMagazineCapacity(stack);
        return getAmmoInMag(stack, capacity) + getChamberedRound(stack);
    }

    public String getLoadedAmmoDisplayName(ItemStack stack) {
        GunDefinition gun = getGunDefinition(stack);
        CzoAmmoDefinition ammo = CzoAmmo.getOrDefault(getLoadedAmmoId(stack, gun), gun);
        return ammo.displayName();
    }

    /**
     * Used by the compact CZO survival HUD to render the physical ammo item icon.
     */
    public String getHudLoadedAmmoItemId(ItemStack stack) {
        GunDefinition gun = getGunDefinition(stack);
        return getLoadedAmmoId(stack, gun);
    }

    public String getSelectedAmmoDisplayName(ItemStack stack) {
        GunDefinition gun = getGunDefinition(stack);
        CzoAmmoDefinition ammo = CzoAmmo.getOrDefault(getSelectedAmmoId(stack, gun), gun);
        return ammo.displayName();
    }

    public void setSelectedAmmo(ServerPlayer player, ItemStack stack, String ammoId) {
        if (stack.getItem() != this) {
            return;
        }

        if (isReloading(stack)) {
            showActionBar(player, "Сначала дождись конца перезарядки");
            return;
        }

        GunDefinition gun = getGunDefinition(stack);

        if (!CzoAmmo.isCompatible(gun, ammoId)) {
            showActionBar(player, "Патрон не подходит");
            return;
        }

        CzoAmmoDefinition ammo = CzoAmmo.getOrDefault(ammoId, gun);
        String oldSelectedAmmoId = getSelectedAmmoId(stack, gun);
        String loadedAmmoId = getLoadedAmmoId(stack, gun);
        int loadedAmmoCount = getLoadedAmmoCount(stack);

        if (oldSelectedAmmoId.equals(ammo.id()) && (loadedAmmoCount <= 0 || loadedAmmoId.equals(ammo.id()))) {
            showActionBar(player, "Уже выбрано: " + ammo.displayName());
            return;
        }

        int availableAmmo = countAmmoInInventory(player, gun, ammo.id());

        if (availableAmmo <= 0) {
            showActionBar(player, "Нет патронов: " + ammo.displayName());
            return;
        }

        stack.set(CzoDataComponents.SELECTED_AMMO_ID.get(), ammo.id());

        // Если внутри оружия лежит другой тип патронов, не превращаем их магически.
        // Сначала возвращаем старые патроны в инвентарь, потом запускаем перезарядку новым типом.
        if (loadedAmmoCount > 0 && !loadedAmmoId.equals(ammo.id())) {
            returnLoadedAmmoToInventory(player, stack, loadedAmmoId);
            setAmmoInMag(stack, 0);
            setChamberedRound(stack, 0);
            setLoadedAmmoId(stack, ammo.id());
            beginReload(player, stack, ammo, false);
            return;
        }

        showActionBar(player, "Выбрано: " + ammo.displayName());
    }

    private boolean isRequiredAmmo(ItemStack ammoStack, GunDefinition gun, String selectedAmmoId) {
        if (ammoStack.isEmpty() || !CzoAmmo.isCompatible(gun, selectedAmmoId)) {
            return false;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        return itemId != null && itemId.toString().equals(selectedAmmoId);
    }

    public int countSelectedAmmoInInventory(Player player, ItemStack gunStack) {
        GunDefinition gun = getGunDefinition(gunStack);
        String selectedAmmoId = getSelectedAmmoId(gunStack, gun);
        return countAmmoInInventory(player, gun, selectedAmmoId);
    }

    public int countAmmoInInventory(Player player, GunDefinition gun, String ammoId) {
        int total = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

            if (isRequiredAmmo(stack, gun, ammoId)) {
                total += CzoStackCounts.logicalCount(stack);
            }
        }

        return total;
    }

    private int consumeAmmoFromInventory(ServerPlayer player, GunDefinition gun, String selectedAmmoId, int amount) {
        int remaining = amount;
        int consumed = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = player.getInventory().getItem(slot);

            if (!isRequiredAmmo(stack, gun, selectedAmmoId)) {
                continue;
            }

            int toTake = Math.min(CzoStackCounts.logicalCount(stack), remaining);
            CzoStackCounts.shrinkLogical(stack, toTake);

            remaining -= toTake;
            consumed += toTake;
        }

        return consumed;
    }

    public void installModule(ServerPlayer player, ItemStack stack, String slotId, String moduleId) {
        if (stack.getItem() != this) {
            return;
        }

        GunModuleSlot slot = GunModuleSlot.fromId(slotId).orElse(null);

        if (slot == null) {
            showActionBar(player, "Неизвестный слот: " + slotId);
            return;
        }

        String currentModuleId = getInstalledModuleIdForStats(stack, slotId);
        GunModule currentModule = CzoGunModules.getModule(currentModuleId);

        // Повторный клик по установленному модулю / команда NONE = снять модуль.
        if (moduleId == null
                || moduleId.isBlank()
                || CzoGunModules.NONE.equals(moduleId)
                || (currentModule != null && currentModule.id().equals(moduleId) && !currentModule.defaultModule())) {
            unequipModule(player, stack, slot, currentModule);
            return;
        }

        GunModule module = CzoGunModules.getModule(moduleId);

        if (module == null) {
            showActionBar(player, "Неизвестный модуль");
            return;
        }

        if (module.defaultModule()) {
            showActionBar(player, "Базовый модуль ставится автоматически");
            return;
        }

        if (module.slot() != slot) {
            showActionBar(player, "Модуль не подходит для слота");
            return;
        }

        if (module.hasPhysicalItem() && !consumePhysicalModuleItem(player, module)) {
            showActionBar(player, "Нет модуля в инвентаре: " + module.displayName());
            return;
        }

        if (currentModule != null && currentModule.hasPhysicalItem()) {
            givePhysicalModuleItem(player, currentModule);
        }

        setReloadTicksLeft(stack, 0);
        setModuleIdForSlot(stack, slot, module.id());

        if (slot == GunModuleSlot.MAGAZINE) {
            int capacity = module.magazineCapacity();
            int magAmmo = getAmmoInMag(stack, capacity);
            setAmmoInMag(stack, magAmmo);
        }

        showActionBar(player, "Установлено: " + module.displayName());
    }

    private void unequipModule(ServerPlayer player, ItemStack stack, GunModuleSlot slot, GunModule currentModule) {
        if (currentModule != null && currentModule.hasPhysicalItem()) {
            givePhysicalModuleItem(player, currentModule);
        }

        setReloadTicksLeft(stack, 0);

        String fallbackModuleId = CzoGunModules.getDefaultModuleIdForSlot(slot);
        setModuleIdForSlot(stack, slot, fallbackModuleId);

        if (slot == GunModuleSlot.MAGAZINE) {
            int capacity = getMagazine(stack).magazineCapacity();
            int magAmmo = getAmmoInMag(stack, capacity);
            setAmmoInMag(stack, magAmmo);
        }

        if (currentModule == null || CzoGunModules.NONE.equals(currentModule.id()) || currentModule.defaultModule()) {
            showActionBar(player, "Слот уже пуст");
        } else {
            showActionBar(player, "Снято: " + currentModule.displayName());
        }
    }

    private void setModuleIdForSlot(ItemStack stack, GunModuleSlot slot, String moduleId) {
        switch (slot) {
            case MAGAZINE -> stack.set(CzoDataComponents.MAGAZINE_ID.get(), moduleId);
            case MUZZLE -> stack.set(CzoDataComponents.MUZZLE_ID.get(), moduleId);
            case OPTIC -> stack.set(CzoDataComponents.OPTIC_ID.get(), moduleId);
            case STOCK -> stack.set(CzoDataComponents.STOCK_ID.get(), moduleId);
            case GRIP -> stack.set(CzoDataComponents.GRIP_ID.get(), moduleId);
            case BARREL -> stack.set(CzoDataComponents.BARREL_ID.get(), moduleId);
            case HANDGUARD -> stack.set(CzoDataComponents.HANDGUARD_ID.get(), moduleId);
            case CHARM -> stack.set(CzoDataComponents.CHARM_ID.get(), moduleId);
            //case LASER -> stack.set(CzoDataComponents.LASER_ID.get(), moduleId);
        }
    }

    private boolean consumePhysicalModuleItem(ServerPlayer player, GunModule module) {
        if (!module.hasPhysicalItem()) {
            return true;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);

            if (!isPhysicalItemForModule(inventoryStack, module)) {
                continue;
            }

            inventoryStack.shrink(1);
            return true;
        }

        return false;
    }

    private void givePhysicalModuleItem(ServerPlayer player, GunModule module) {
        ItemStack moduleStack = createPhysicalModuleStack(module);

        if (moduleStack.isEmpty()) {
            return;
        }

        CzoBackpackItemTransfer.insertIntoBackpack(player, moduleStack);
        if (!moduleStack.isEmpty()) {
            player.drop(moduleStack, false);
        }
    }

    private ItemStack createPhysicalModuleStack(GunModule module) {
        if (CzoGunModules.PM_MAG_12.equals(module.id())) {
            return new ItemStack(CZO.PM_MAG_12.get());
        }

        return ItemStack.EMPTY;
    }

    private boolean isPhysicalItemForModule(ItemStack stack, GunModule module) {
        if (stack.isEmpty() || !module.hasPhysicalItem()) {
            return false;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.toString().equals(module.physicalItemId());
    }


    public void serverTickReload(ServerPlayer player, ItemStack stack) {
        if (stack.getItem() != this) {
            return;
        }

        int reloadTicksLeft = getReloadTicksLeft(stack);

        if (reloadTicksLeft <= 0) {
            return;
        }

        reloadTicksLeft--;
        setReloadTicksLeft(stack, reloadTicksLeft);

        if (reloadTicksLeft > 0) {
            return;
        }

        completeReload(player, stack);
    }

    public void cancelReload(ItemStack stack) {
        if (!isReloading(stack)) {
            return;
        }

        setReloadTicksLeft(stack, 0);
    }

    public void cancelReload(ServerPlayer player, ItemStack stack) {
        if (!isReloading(stack)) {
            return;
        }

        setReloadTicksLeft(stack, 0);
        showActionBar(player, "Перезарядка отменена");
    }

    public void tryFire(ServerPlayer shooter, ItemStack stack) {
        if (!(shooter.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (stack.getItem() != this) {
            return;
        }

        if (isReloading(stack)) {
            showActionBar(shooter, "Перезарядка...");
            return;
        }

        GunStats stats = getEffectiveStats(stack);

        long gameTime = serverLevel.getGameTime();
        UUID playerId = shooter.getUUID();
        long nextAllowedShot = nextShotTick.getOrDefault(playerId, 0L);

        if (gameTime < nextAllowedShot) {
            return;
        }

        int capacity = getMagazineCapacity(stack);
        int magAmmo = getAmmoInMag(stack, capacity);
        int chambered = getChamberedRound(stack);

        if (chambered <= 0 && magAmmo <= 0) {
            nextShotTick.put(playerId, gameTime + 6);

            serverLevel.playSound(
                    null,
                    shooter.getX(),
                    shooter.getY(),
                    shooter.getZ(),
                    SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.4F
            );

            showActionBar(shooter, "Пусто: 0/" + capacity);
            return;
        }

        if (chambered <= 0 && magAmmo > 0) {
            magAmmo--;
            chambered = 1;
        }

        chambered = 0;

        if (magAmmo > 0) {
            magAmmo--;
            chambered = 1;
        }

        setAmmoInMag(stack, magAmmo);
        setChamberedRound(stack, chambered);

        nextShotTick.put(playerId, gameTime + stats.fireDelayTicks());

        shoot(serverLevel, shooter, stats);

        // Ammo state is rendered by the CZO HUD; do not spam the vanilla actionbar as an old weapon HUD.
    }

    public void tryReload(ServerPlayer shooter, ItemStack stack) {
        if (!(shooter.level() instanceof ServerLevel)) {
            return;
        }

        if (stack.getItem() != this) {
            return;
        }

        if (isReloading(stack)) {
            showActionBar(shooter, "Уже перезаряжается...");
            return;
        }

        int capacity = getMagazineCapacity(stack);
        int magAmmo = getAmmoInMag(stack, capacity);
        int chambered = getChamberedRound(stack);
        GunDefinition gun = getGunDefinition(stack);

        if (magAmmo >= capacity && chambered > 0) {
            showActionBar(shooter, "Магазин полон: " + formatAmmo(stack));
            return;
        }

        int ammoNeeded = getAmmoNeededForReload(capacity, magAmmo, chambered);
        String selectedAmmoId = getSelectedAmmoId(stack, gun);
        String reloadAmmoId = chooseReloadAmmoId(shooter, gun, selectedAmmoId, ammoNeeded);
        CzoAmmoDefinition reloadAmmo = CzoAmmo.getOrDefault(reloadAmmoId, gun);

        int availableAmmo = countAmmoInInventory(shooter, gun, reloadAmmoId);

        if (availableAmmo <= 0) {
            showActionBar(shooter, "Нет патронов: " + reloadAmmo.displayName());
            return;
        }

        if (!reloadAmmoId.equals(selectedAmmoId)) {
            stack.set(CzoDataComponents.SELECTED_AMMO_ID.get(), reloadAmmoId);
            showActionBar(shooter, "Автовыбор: " + reloadAmmo.displayName());
        }

        String loadedAmmoId = getLoadedAmmoId(stack, gun);
        int loadedAmmoCount = getLoadedAmmoCount(stack);

        if (loadedAmmoCount > 0 && !loadedAmmoId.equals(reloadAmmoId)) {
            returnLoadedAmmoToInventory(shooter, stack, loadedAmmoId);
            setAmmoInMag(stack, 0);
            setChamberedRound(stack, 0);
        }

        setLoadedAmmoId(stack, reloadAmmoId);
        beginReload(shooter, stack, reloadAmmo, chambered > 0 && loadedAmmoId.equals(reloadAmmoId));
    }

    private void completeReload(ServerPlayer player, ItemStack stack) {
        int capacity = getMagazineCapacity(stack);
        int magAmmo = getAmmoInMag(stack, capacity);
        int chambered = getChamberedRound(stack);
        GunDefinition gun = getGunDefinition(stack);
        String selectedAmmoId = getSelectedAmmoId(stack, gun);
        int ammoNeededForFullMag = getAmmoNeededForReload(capacity, magAmmo, chambered);

        int consumed = consumeAmmoFromInventory(player, gun, selectedAmmoId, ammoNeededForFullMag);

        if (consumed <= 0) {
            showActionBar(player, "Нет патронов");
            return;
        }

        setLoadedAmmoId(stack, selectedAmmoId);

        if (chambered > 0) {
            setAmmoInMag(stack, magAmmo + consumed);
            setChamberedRound(stack, 1);
        } else {
            int newChambered = 0;
            int ammoForMag = consumed;

            if (ammoForMag > 0) {
                newChambered = 1;
                ammoForMag--;
            }

            setChamberedRound(stack, newChambered);
            setAmmoInMag(stack, magAmmo + ammoForMag);
        }

        // Reload result is visible in the CZO ammo HUD.
    }

    private int getAmmoNeededForReload(int capacity, int magAmmo, int chambered) {
        int ammoNeededForFullMag = capacity - magAmmo;

        if (ammoNeededForFullMag <= 0 && chambered <= 0) {
            ammoNeededForFullMag = 1;
        }

        return Math.max(1, ammoNeededForFullMag);
    }

    private String chooseReloadAmmoId(ServerPlayer player, GunDefinition gun, String preferredAmmoId, int ammoNeeded) {
        int preferredCount = countAmmoInInventory(player, gun, preferredAmmoId);

        if (preferredCount >= ammoNeeded) {
            return preferredAmmoId;
        }

        String bestAmmoId = preferredAmmoId;
        int bestCount = preferredCount;

        for (CzoAmmoDefinition ammo : CzoAmmo.getCompatibleAmmo(gun)) {
            int count = countAmmoInInventory(player, gun, ammo.id());

            if (count > bestCount) {
                bestAmmoId = ammo.id();
                bestCount = count;
            }
        }

        return bestAmmoId;
    }

    private void beginReload(ServerPlayer player, ItemStack stack, CzoAmmoDefinition ammo, boolean tactical) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        setReloadTicksLeft(stack, getReloadDurationTicks(stack));

        serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.8F,
                0.75F
        );

        if (tactical) {
            showActionBar(player, "Тактическая перезарядка: " + ammo.displayName());
        } else {
            showActionBar(player, "Перезарядка: " + ammo.displayName());
        }
    }

    private void returnLoadedAmmoToInventory(ServerPlayer player, ItemStack stack, String ammoId) {
        int count = getLoadedAmmoCount(stack);

        if (count <= 0) {
            return;
        }

        ItemStack ammoStack = createAmmoStack(ammoId, count);

        if (ammoStack.isEmpty()) {
            return;
        }

        CzoBackpackItemTransfer.insertIntoBackpack(player, ammoStack);
        if (!ammoStack.isEmpty()) {
            player.drop(ammoStack, false);
        }
    }

    private ItemStack createAmmoStack(String ammoId, int count) {
        ItemStack stack = ItemStack.EMPTY;
        if (CzoAmmo.AMMO_9X18_AP.equals(ammoId)) {
            stack = new ItemStack(CZO.AMMO_9X18_AP.get());
        } else if (CzoAmmo.AMMO_9X18_FMJ.equals(ammoId)) {
            stack = new ItemStack(CZO.AMMO_9X18_FMJ.get());
        }

        if (!stack.isEmpty()) {
            CzoStackCounts.setLogicalCount(stack, count);
        }

        return stack;
    }

    private Vec3 applySpread(ServerLevel level, Vec3 direction, float spreadDegrees) {
        if (spreadDegrees <= 0.0F) {
            return direction.normalize();
        }

        double spread = Math.toRadians(spreadDegrees);

        double x = (level.getRandom().nextDouble() - 0.5D) * spread;
        double y = (level.getRandom().nextDouble() - 0.5D) * spread;

        return direction.add(x, y, 0.0D).normalize();
    }

    private void shoot(ServerLevel level, Player shooter, GunStats stats) {
        Vec3 start = shooter.getEyePosition();
        Vec3 direction = applySpread(level, shooter.getViewVector(1.0F), stats.hipSpreadDegrees());
        Vec3 end = start.add(direction.scale(stats.range()));

        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));

        double blockDistanceSqr = start.distanceToSqr(blockHit.getLocation());
        GunHit entityHit = findEntityHit(level, shooter, start, end, blockDistanceSqr);

        Vec3 hitPos = blockHit.getLocation();

        if (entityHit != null) {
            Entity target = entityHit.entity();
            hitPos = entityHit.location();

            target.hurtServer(
                    level,
                    shooter.damageSources().playerAttack(shooter),
                    stats.damage()
            );
        }

        level.playSound(
                null,
                shooter.getX(),
                shooter.getY(),
                shooter.getZ(),
                SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.75F
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                hitPos.x,
                hitPos.y,
                hitPos.z,
                5,
                0.03D,
                0.03D,
                0.03D,
                0.01D
        );
    }

    public String getHudGunName(ItemStack stack) {
        return getGunDefinition(stack).displayName();
    }

    private GunHit findEntityHit(ServerLevel level, Player shooter, Vec3 start, Vec3 end, double maxDistanceSqr) {
        AABB searchBox = shooter.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(1.0D);

        Entity closestEntity = null;
        Vec3 closestPos = null;
        double closestDistanceSqr = maxDistanceSqr;

        for (Entity entity : level.getEntities(shooter, searchBox, entity ->
                entity.isPickable()
                        && !entity.isSpectator()
                        && entity != shooter
        )) {
            AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.15D);
            Optional<Vec3> optionalHit = hitBox.clip(start, end);

            if (hitBox.contains(start)) {
                optionalHit = Optional.of(start);
            }

            if (optionalHit.isEmpty()) {
                continue;
            }

            Vec3 hitPos = optionalHit.get();
            double distanceSqr = start.distanceToSqr(hitPos);

            if (distanceSqr < closestDistanceSqr) {
                closestEntity = entity;
                closestPos = hitPos;
                closestDistanceSqr = distanceSqr;
            }
        }

        if (closestEntity == null) {
            return null;
        }

        return new GunHit(closestEntity, closestPos);
    }

    private int getMagazineCapacity(ItemStack stack) {
        return getMagazine(stack).magazineCapacity();
    }

    private int getReloadDurationTicks(ItemStack stack) {
        return getEffectiveStats(stack).baseReloadTicks();
    }

    private GunModule getMagazine(ItemStack stack) {
        String magazineId = stack.get(CzoDataComponents.MAGAZINE_ID.get());

        if (magazineId == null || magazineId.isBlank() || CzoGunModules.NONE.equals(magazineId)) {
            stack.set(CzoDataComponents.MAGAZINE_ID.get(), CzoGunModules.PM_MAG_8);
            return CzoGunModules.getMagazine(CzoGunModules.PM_MAG_8);
        }

        return CzoGunModules.getMagazine(magazineId);
    }

    public String getHudAmmoText(ItemStack stack) {
        return formatAmmo(stack);
    }

    public String getHudMagazineText(ItemStack stack) {
        return getMagazine(stack).displayName();
    }

    public String getHudSelectedAmmoText(ItemStack stack) {
        String selected = getSelectedAmmoDisplayName(stack);
        String loaded = getLoadedAmmoDisplayName(stack);

        if (getLoadedAmmoCount(stack) > 0 && !selected.equals(loaded)) {
            return "Заряжено: " + loaded + " / выбрано: " + selected;
        }

        return selected;
    }

    public String getHudStateText(ItemStack stack) {
        if (isReloading(stack)) {
            return "RLD";
        }

        return "";
    }

    private int getAmmoInMag(ItemStack stack, int capacity) {
        Integer ammo = stack.get(CzoDataComponents.AMMO_IN_MAG.get());

        if (ammo == null) {
            int defaultAmmo = capacity - 1;
            stack.set(CzoDataComponents.AMMO_IN_MAG.get(), defaultAmmo);
            return defaultAmmo;
        }

        return clamp(ammo, 0, capacity);
    }

    private void setAmmoInMag(ItemStack stack, int ammo) {
        int capacity = getMagazineCapacity(stack);
        int clampedAmmo = clamp(ammo, 0, capacity);
        stack.set(CzoDataComponents.AMMO_IN_MAG.get(), clampedAmmo);
    }

    private int getChamberedRound(ItemStack stack) {
        Integer chambered = stack.get(CzoDataComponents.CHAMBERED_ROUND.get());

        if (chambered == null) {
            stack.set(CzoDataComponents.CHAMBERED_ROUND.get(), 1);
            return 1;
        }

        return clamp(chambered, 0, 1);
    }

    private void setChamberedRound(ItemStack stack, int chambered) {
        stack.set(CzoDataComponents.CHAMBERED_ROUND.get(), clamp(chambered, 0, 1));
    }

    private int getReloadTicksLeft(ItemStack stack) {
        Integer ticks = stack.get(CzoDataComponents.RELOAD_TICKS_LEFT.get());

        if (ticks == null) {
            stack.set(CzoDataComponents.RELOAD_TICKS_LEFT.get(), 0);
            return 0;
        }

        return Math.max(0, ticks);
    }

    private void setReloadTicksLeft(ItemStack stack, int ticks) {
        stack.set(CzoDataComponents.RELOAD_TICKS_LEFT.get(), Math.max(0, ticks));
    }

    public boolean isReloading(ItemStack stack) {
        return getReloadTicksLeft(stack) > 0;
    }

    private String formatAmmo(ItemStack stack) {
        int capacity = getMagazineCapacity(stack);
        int magAmmo = getAmmoInMag(stack, capacity);
        int chambered = getChamberedRound(stack);

        if (magAmmo >= capacity && chambered > 0) {
            return capacity + "+1";
        }

        int total = magAmmo + chambered;
        return total + "/" + capacity;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void showActionBar(ServerPlayer player, String text) {
        player.connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal(text))
        );
    }

    private record GunHit(Entity entity, Vec3 location) {
    }
}
