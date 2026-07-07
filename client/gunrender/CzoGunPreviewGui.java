package com.czo.client.gunrender;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;

/**
 * GUI bridge for rendering the real weapon model inside GunModificationScreen.
 *
 * NeoForge 26.1.2 here does not expose the newer PiP classes, so the screen still goes
 * through the registered special item renderer. The important difference from the old
 * preview is that this bridge supplies a dedicated preview context: scale, centering and
 * socket projection now use the OBJ bounds instead of a stretched 16x16 item sprite.
 */
public final class CzoGunPreviewGui {
    /** Longest model dimension inside a normal 16x16 item icon, in item-local units. */
    public static final float ICON_TARGET_LOCAL_SIZE = 0.92F;

    /** Longest model dimension inside the modification preview, in item-local units. */
    public static final float PREVIEW_TARGET_LOCAL_SIZE = 1.70F;

    private static final ThreadLocal<PreviewContext> PREVIEW_CONTEXT = new ThreadLocal<>();

    private CzoGunPreviewGui() {
    }

    public static void submit(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            int width,
            int height,
            float yawDegrees,
            float pitchDegrees,
            float rollDegrees
    ) {
        if (stack.isEmpty() || width <= 0 || height <= 0) {
            return;
        }

        float pixelsPerItemUnit = modelPixelsPerItemUnit(width, height);
        float itemScale = pixelsPerItemUnit / 16.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width / 2.0F, y + height / 2.0F);
        graphics.pose().scale(itemScale, itemScale);

        PREVIEW_CONTEXT.set(new PreviewContext(PREVIEW_TARGET_LOCAL_SIZE, yawDegrees, pitchDegrees, rollDegrees));
        try {
            graphics.item(stack, -8, -8);
        } finally {
            PREVIEW_CONTEXT.remove();
            graphics.pose().popMatrix();
        }
    }

    public static Vector2f projectSlot(
            ItemStack stack,
            String slotName,
            int x,
            int y,
            int width,
            int height,
            float yawDegrees,
            float pitchDegrees,
            float rollDegrees
    ) {
        return CzoGunScreenSocketProjector.projectSlot(
                stack,
                slotName,
                x + width / 2.0F,
                y + height / 2.0F,
                modelPixelsPerItemUnit(width, height),
                PREVIEW_TARGET_LOCAL_SIZE,
                yawDegrees,
                pitchDegrees,
                rollDegrees
        );
    }

    public static PreviewContext currentPreviewContext() {
        return PREVIEW_CONTEXT.get();
    }

    private static float modelPixelsPerItemUnit(int width, int height) {
        // Делает превью крупнее, но оставляет запас под ноды/подписи.
        // Если модель снова будет слишком большой — первым делом крутим 1.18F вниз.
        return Math.max(120.0F, Math.min(width, height) * 1.18F);
    }

    public record PreviewContext(float targetLocalSize, float yawDegrees, float pitchDegrees, float rollDegrees) {
    }
}
