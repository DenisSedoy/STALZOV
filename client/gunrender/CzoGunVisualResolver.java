package com.czo.client.gunrender;

import com.czo.CZO;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bridge between existing server gun logic and the new client visual layer.
 *
 * Stage 1G:
 * - PM stack still maps to PM base layout.
 * - PM 12-round magazine has its own visual layout.
 * - Until we wire a clean public getter from GunItem/CzoDataComponents, installed modules are detected from the
 *   stack component text. This is intentionally isolated here so the renderer itself stays clean.
 */
public final class CzoGunVisualResolver {
    public static final Identifier PM_LAYOUT = Identifier.fromNamespaceAndPath(CZO.MODID, "models/guns/pm/pm.layout.json");
    public static final Identifier PM_MAG_12_LAYOUT = Identifier.fromNamespaceAndPath(CZO.MODID, "models/guns/pm/modules/pm_mag_12.layout.json");

    private CzoGunVisualResolver() {
    }

    public static Identifier layoutFor(ItemStack stack) {
        // TODO stage 2: read real GunId from your existing gun data component and map czo:pm -> PM_LAYOUT.
        return PM_LAYOUT;
    }

    public static Set<String> hiddenParts(ItemStack stack, CzoGunLayout layout) {
        Set<String> hidden = new HashSet<>();
        for (VisualModule module : installedVisualModules(stack, layout)) {
            CzoGunLayout.ModuleSlot slot = layout.moduleSlots().get(module.slot());
            if (slot != null) {
                hidden.addAll(slot.hidePartsWhenInstalled());
            }
        }
        return hidden;
    }

    public static List<VisualModule> installedVisualModules(ItemStack stack, CzoGunLayout layout) {
        List<VisualModule> modules = new ArrayList<>();

        // Existing logic should already write the installed PM 12 magazine module into the ItemStack.
        // This text bridge catches common names without importing your gun classes yet.
        String text = stackText(stack);
        if (text.contains("pm_mag_12") || text.contains("pm.mag.12") || text.contains("pm_magazine_12") || text.contains("magazine_12") || text.contains("pm 12") || text.contains("пм 12")) {
            modules.add(new VisualModule("magazine", PM_MAG_12_LAYOUT));
        }

        return modules;
    }

    private static String stackText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(256);
        builder.append(stack).append('\n');
        builder.append(stack.getItem()).append('\n');

        Object components = callNoArg(stack, "getComponents");
        if (components != null) {
            builder.append(components).append('\n');
        }

        Object tag = callNoArg(stack, "getTag");
        if (tag != null) {
            builder.append(tag).append('\n');
        }

        Object shareTag = callNoArg(stack, "getShareTag");
        if (shareTag != null) {
            builder.append(shareTag).append('\n');
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static Object callNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public record VisualModule(String slot, Identifier layout) {
    }
}
