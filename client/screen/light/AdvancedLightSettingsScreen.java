package com.czo.client.screen.light;

import com.czo.light.AdvancedLightSettings;
import com.czo.light.AdvancedLightSettings.Parameter;
import com.czo.light.AdvancedLightSettings.QualityPreset;
import com.czo.light.AdvancedLightSettings.Section;
import com.czo.light.LightDirectionPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Two-tab editor used only by Advanced light sources.
 * The Basic tab deliberately remains a host for the current simple controls;
 * all renderer-specific values live on the Advanced tab.
 */
public final class AdvancedLightSettingsScreen extends Screen {
    private static final int BACKGROUND = 0xEE070907;
    private static final int PANEL = 0xDD111411;
    private static final int PANEL_HOVER = 0xEE202820;
    private static final int OUTLINE = 0xFF596359;
    private static final int ACCENT = 0xFF78E878;
    private static final int WARNING = 0xFFFFC45C;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int MUTED = 0xFFA6ADA6;
    private static final int DISABLED = 0xFF626762;
    private static final int FIELD = 0xFF0A0D0A;
    private static final int SLIDER_TRACK = 0xFF313831;
    private static final int SLIDER_FILL = 0xFF70C970;

    private final AdvancedLightSettings target;
    private final AdvancedLightSettings working;
    private final Consumer<AdvancedLightSettings> onApply;
    private final List<Hitbox> hitboxes = new ArrayList<>();

    private Tab activeTab = Tab.BASIC;
    private Section activeSection = Section.SHADOWS;
    private Parameter editingParameter;
    private String editBuffer = "";

    public AdvancedLightSettingsScreen(AdvancedLightSettings settings, Consumer<AdvancedLightSettings> onApply) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Настройки Advanced-источника"));
        this.target = settings;
        this.working = settings.copy();
        this.onApply = onApply;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        centered(graphics, "ADVANCED LIGHT SOURCE", 8, TEXT);
        tabs(graphics, mouseX, mouseY);
        if (activeTab == Tab.BASIC) basicTab(graphics);
        else advancedTab(graphics, mouseX, mouseY);
        bottomButtons(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void tabs(GuiGraphicsExtractor graphics, int mx, int my) {
        int w = 126, h = 22, gap = 6, x = (width - w * 2 - gap) / 2, y = 25;
        tab(graphics, Tab.BASIC, "Основное", x, y, w, h, mx, my);
        tab(graphics, Tab.ADVANCED, "Advanced", x + w + gap, y, w, h, mx, my);
    }

    private void tab(GuiGraphicsExtractor graphics, Tab tab, String title, int x, int y, int w, int h, int mx, int my) {
        boolean selected = activeTab == tab;
        panel(graphics, x, y, w, h, selected ? ACCENT : hovered(x, y, w, h, mx, my) ? WARNING : OUTLINE,
                hovered(x, y, w, h, mx, my) ? PANEL_HOVER : PANEL);
        centeredIn(graphics, title, x, y, w, h, selected ? ACCENT : TEXT);
        hitboxes.add(Hitbox.tab(x, y, w, h, tab));
    }

    private void basicTab(GuiGraphicsExtractor graphics) {
        int w = Math.min(570, width - 50), h = 122, x = (width - w) / 2, y = 72;
        panel(graphics, x, y, w, h, OUTLINE, PANEL);
        centeredIn(graphics, "ОСНОВНЫЕ ПАРАМЕТРЫ ИСТОЧНИКА", x, y + 9, w, 18, TEXT);
        centeredIn(graphics, "Сюда подключается существующее меню: включение, яркость,", x, y + 39, w, 14, MUTED);
        centeredIn(graphics, "базовый цвет, радиус и обычное направление.", x, y + 56, w, 14, MUTED);
        centeredIn(graphics, "Новые параметры не меняют Basic-источники и находятся только в Advanced.", x, y + 84, w, 14, ACCENT);
    }

    private void advancedTab(GuiGraphicsExtractor graphics, int mx, int my) {
        qualityPresets(graphics, mx, my);
        int sx = 14, sy = 82, sw = 136, sh = 22;
        int i = 0;
        for (Section section : Section.values()) {
            int y = sy + i++ * (sh + 5);
            boolean selected = activeSection == section;
            button(graphics, section.title(), sx, y, sw, sh, selected ? ACCENT : OUTLINE, mx, my,
                    Hitbox.section(sx, y, sw, sh, section));
        }
        int resetY = sy + Section.values().length * (sh + 5) + 4;
        button(graphics, "Сбросить раздел", sx, resetY, sw, 20, WARNING, mx, my,
                Hitbox.simple(sx, resetY, sw, 20, Kind.RESET_SECTION));

        int cx = 164, cy = 82, cw = width - cx - 14, ch = height - cy - 50;
        panel(graphics, cx, cy, cw, ch, OUTLINE, PANEL);
        text(graphics, activeSection.title(), cx + 12, cy + 10, ACCENT);

        int controlsY = toggles(graphics, cx + 12, cy + 30, cw - 24, mx, my);
        controlsY = directionPresets(graphics, cx + 12, controlsY, cw - 24, mx, my);

        List<Parameter> parameters = parameters(activeSection);
        int columns = cw >= 660 ? 2 : 1;
        int gap = 12;
        int colW = (cw - 24 - (columns - 1) * gap) / columns;
        int perColumn = (parameters.size() + columns - 1) / columns;
        for (i = 0; i < parameters.size(); i++) {
            int col = i / perColumn, row = i % perColumn;
            parameter(graphics, parameters.get(i), cx + 12 + col * (colW + gap), controlsY + row * 34, colW, mx, my);
        }

        int loadColor = switch (working.estimatedLoad()) {
            case "Низкая" -> ACCENT;
            case "Средняя" -> 0xFFFFFF77;
            case "Высокая" -> WARNING;
            default -> 0xFFFF6666;
        };
        text(graphics, "Оценка нагрузки: " + working.estimatedLoad(), cx + 12, cy + ch - 16, loadColor);
    }

    private void qualityPresets(GuiGraphicsExtractor graphics, int mx, int my) {
        QualityPreset[] presets = {QualityPreset.PERFORMANCE, QualityPreset.BALANCED, QualityPreset.HIGH, QualityPreset.CINEMATIC};
        int gap = 5, w = Math.max(92, Math.min(132, (width - 40 - gap * 3) / 4));
        int x = (width - w * 4 - gap * 3) / 2, y = 54;
        for (int i = 0; i < presets.length; i++) {
            QualityPreset preset = presets[i];
            int bx = x + i * (w + gap);
            button(graphics, preset.title(), bx, y, w, 20, working.qualityPreset() == preset ? ACCENT : OUTLINE, mx, my,
                    Hitbox.quality(bx, y, w, 20, preset));
        }
    }

    private int toggles(GuiGraphicsExtractor graphics, int x, int y, int width, int mx, int my) {
        List<Toggle> toggles = switch (activeSection) {
            case SHADOWS -> List.of(Toggle.SHADOWS, Toggle.CONTACT_SHADOWS, Toggle.TRANSPARENT_SHADOWS);
            case COLORED_GLASS -> List.of(Toggle.COLORED_GLASS, Toggle.GLASS_THICKNESS, Toggle.GLASS_MIXING);
            case VOLUMETRIC -> List.of(Toggle.VOLUMETRIC, Toggle.VOLUMETRIC_SHADOWS, Toggle.VOLUMETRIC_GLASS);
            default -> List.of();
        };
        if (toggles.isEmpty()) return y;
        int gap = 6, bw = Math.max(108, Math.min(170, (width - gap * 2) / 3));
        for (int i = 0; i < toggles.size(); i++) {
            Toggle toggle = toggles.get(i);
            boolean enabled = toggle.get(working);
            int bx = x + i * (bw + gap);
            button(graphics, (enabled ? "[ВКЛ] " : "[ВЫКЛ] ") + toggle.title, bx, y, bw, 20,
                    enabled ? ACCENT : DISABLED, mx, my, Hitbox.toggle(bx, y, bw, 20, toggle));
        }
        return y + 29;
    }

    private int directionPresets(GuiGraphicsExtractor graphics, int x, int y, int width, int mx, int my) {
        if (activeSection != Section.SPOTLIGHT) return y;
        text(graphics, "Пресеты направления:", x, y + 5, MUTED);
        LightDirectionPreset[] presets = {LightDirectionPreset.UP, LightDirectionPreset.DOWN, LightDirectionPreset.NORTH,
                LightDirectionPreset.SOUTH, LightDirectionPreset.WEST, LightDirectionPreset.EAST};
        int start = x + 130, bw = Math.max(56, Math.min(82, (width - 130 - 20) / 6));
        for (int i = 0; i < presets.length; i++) {
            LightDirectionPreset preset = presets[i];
            int bx = start + i * (bw + 4);
            button(graphics, preset.title(), bx, y, bw, 20, working.directionPreset() == preset ? ACCENT : OUTLINE, mx, my,
                    Hitbox.direction(bx, y, bw, 20, preset));
        }
        return y + 29;
    }

    private void parameter(GuiGraphicsExtractor graphics, Parameter p, int x, int y, int width, int mx, int my) {
        int labelW = Math.min(142, Math.max(96, width / 3)), inputW = 65, resetW = 20, gap = 5;
        int sliderX = x + labelW, sliderW = Math.max(68, width - labelW - inputW - resetW - gap * 3);
        int inputX = sliderX + sliderW + gap, resetX = inputX + inputW + gap;
        text(graphics, p.title(), x, y + 7, TEXT);

        int trackY = y + 11;
        graphics.fill(sliderX, trackY, sliderX + sliderW, trackY + 4, SLIDER_TRACK);
        double normalized = (working.get(p) - p.min()) / (p.max() - p.min());
        int fill = (int) Math.round(normalized * sliderW);
        graphics.fill(sliderX, trackY, sliderX + fill, trackY + 4, SLIDER_FILL);
        graphics.fill(sliderX + fill - 1, trackY - 3, sliderX + fill + 2, trackY + 7, ACCENT);
        hitboxes.add(Hitbox.parameter(sliderX, y, sliderW, 24, Kind.SLIDER, p));

        boolean editing = editingParameter == p;
        panel(graphics, inputX, y + 2, inputW, 20, editing ? ACCENT : OUTLINE, FIELD);
        centeredIn(graphics, editing ? editBuffer : format(p, working.get(p)), inputX, y + 2, inputW, 20, editing ? ACCENT : TEXT);
        hitboxes.add(Hitbox.parameter(inputX, y + 2, inputW, 20, Kind.INPUT, p));
        button(graphics, "↺", resetX, y + 2, resetW, 20, WARNING, mx, my,
                Hitbox.parameter(resetX, y + 2, resetW, 20, Kind.RESET_PARAMETER, p));
    }

    private void bottomButtons(GuiGraphicsExtractor graphics, int mx, int my) {
        int y = height - 32, w = 108, gap = 8, x = width - 14 - w * 2 - gap;
        button(graphics, "Отмена", x, y, w, 22, OUTLINE, mx, my, Hitbox.simple(x, y, w, 22, Kind.CANCEL));
        button(graphics, "Применить", x + w + gap, y, w, 22, ACCENT, mx, my, Hitbox.simple(x + w + gap, y, w, 22, Kind.APPLY));
        button(graphics, "Сбросить Advanced", 14, y, 146, 22, WARNING, mx, my, Hitbox.simple(14, y, 146, 22, Kind.RESET_ALL));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        int mx = (int) event.x(), my = (int) event.y();
        commitInput();
        for (Hitbox box : hitboxes) {
            if (!hovered(box.x, box.y, box.w, box.h, mx, my)) continue;
            switch (box.kind) {
                case TAB -> activeTab = box.tab;
                case SECTION -> activeSection = box.section;
                case QUALITY -> working.applyQualityPreset(box.quality);
                case TOGGLE -> box.toggle.flip(working);
                case DIRECTION -> working.applyDirectionPreset(box.direction);
                case SLIDER -> {
                    double n = (mx - box.x) / (double) Math.max(1, box.w);
                    working.set(box.parameter, box.parameter.min() + n * (box.parameter.max() - box.parameter.min()));
                }
                case INPUT -> beginInput(box.parameter);
                case RESET_PARAMETER -> working.reset(box.parameter);
                case RESET_SECTION -> working.reset(activeSection);
                case RESET_ALL -> working.resetAll();
                case APPLY -> {
                    target.copyFrom(working);
                    if (onApply != null) onApply.accept(target.copy());
                    onClose();
                }
                case CANCEL -> onClose();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editingParameter == null) return super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { commitInput(); return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { editingParameter = null; editBuffer = ""; return true; }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editingParameter == null) return super.charTyped(event);
        int codepoint = codepoint(event);
        if (codepoint < 0) return true;
        char c = (char) codepoint;
        if ((c >= '0' && c <= '9') || c == '.' || c == ',' || (c == '-' && editBuffer.isEmpty())) {
            if (editBuffer.length() < 14) editBuffer += c;
        }
        return true;
    }

    private void beginInput(Parameter parameter) { editingParameter = parameter; editBuffer = format(parameter, working.get(parameter)); }
    private void commitInput() {
        if (editingParameter == null) return;
        try {
            String value = editBuffer.replace(',', '.').trim();
            if (!value.isEmpty() && !value.equals("-") && !value.equals(".")) working.set(editingParameter, Double.parseDouble(value));
        } catch (NumberFormatException ignored) { }
        editingParameter = null; editBuffer = "";
    }

    private int codepoint(CharacterEvent event) {
        for (String name : new String[]{"codepoint", "codePoint", "character"}) {
            try {
                Method method = event.getClass().getMethod(name);
                Object value = method.invoke(event);
                if (value instanceof Number number) return number.intValue();
                if (value instanceof Character character) return character;
            } catch (ReflectiveOperationException ignored) { }
        }
        return -1;
    }

    private List<Parameter> parameters(Section section) {
        List<Parameter> result = new ArrayList<>();
        for (Parameter parameter : Parameter.values()) if (parameter.section() == section) result.add(parameter);
        return result;
    }

    private String format(Parameter p, double value) { return String.format(Locale.ROOT, "%." + p.decimals() + "f", value); }

    private void button(GuiGraphicsExtractor graphics, String title, int x, int y, int w, int h, int outline, int mx, int my, Hitbox box) {
        boolean hovered = hovered(x, y, w, h, mx, my);
        panel(graphics, x, y, w, h, hovered ? WARNING : outline, hovered ? PANEL_HOVER : PANEL);
        centeredIn(graphics, title, x, y, w, h, outline == DISABLED ? MUTED : TEXT);
        hitboxes.add(box);
    }

    private void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int outline, int fill) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, outline); graphics.fill(x, y + h - 1, x + w, y + h, outline);
        graphics.fill(x, y, x + 1, y + h, outline); graphics.fill(x + w - 1, y, x + w, y + h, outline);
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        graphics.text(font, Component.literal(value), x, y, color, true);
    }

    private void centered(GuiGraphicsExtractor graphics, String value, int y, int color) { text(graphics, value, width / 2 - font.width(value) / 2, y, color); }
    private void centeredIn(GuiGraphicsExtractor graphics, String value, int x, int y, int w, int h, int color) {
        text(graphics, value, x + Math.max(3, (w - font.width(value)) / 2), y + Math.max(2, (h - 8) / 2), color);
    }
    private boolean hovered(int x, int y, int w, int h, int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    @Override public boolean isPauseScreen() { return false; }

    private enum Tab { BASIC, ADVANCED }
    private enum Kind { TAB, SECTION, QUALITY, TOGGLE, DIRECTION, SLIDER, INPUT, RESET_PARAMETER, RESET_SECTION, RESET_ALL, APPLY, CANCEL }

    private enum Toggle {
        SHADOWS("Тени") { boolean get(AdvancedLightSettings s) { return s.shadowsEnabled(); } void flip(AdvancedLightSettings s) { s.setShadowsEnabled(!s.shadowsEnabled()); } },
        CONTACT_SHADOWS("Контактные тени") { boolean get(AdvancedLightSettings s) { return s.contactShadowsEnabled(); } void flip(AdvancedLightSettings s) { s.setContactShadowsEnabled(!s.contactShadowsEnabled()); } },
        TRANSPARENT_SHADOWS("Прозрачные тени") { boolean get(AdvancedLightSettings s) { return s.transparentShadowsEnabled(); } void flip(AdvancedLightSettings s) { s.setTransparentShadowsEnabled(!s.transparentShadowsEnabled()); } },
        COLORED_GLASS("Цветное стекло") { boolean get(AdvancedLightSettings s) { return s.coloredGlassEnabled(); } void flip(AdvancedLightSettings s) { s.setColoredGlassEnabled(!s.coloredGlassEnabled()); } },
        GLASS_THICKNESS("Толщина стекла") { boolean get(AdvancedLightSettings s) { return s.glassThicknessEnabled(); } void flip(AdvancedLightSettings s) { s.setGlassThicknessEnabled(!s.glassThicknessEnabled()); } },
        GLASS_MIXING("Смешивание цветов") { boolean get(AdvancedLightSettings s) { return s.glassColorMixingEnabled(); } void flip(AdvancedLightSettings s) { s.setGlassColorMixingEnabled(!s.glassColorMixingEnabled()); } },
        VOLUMETRIC("Объёмный свет") { boolean get(AdvancedLightSettings s) { return s.volumetricEnabled(); } void flip(AdvancedLightSettings s) { s.setVolumetricEnabled(!s.volumetricEnabled()); } },
        VOLUMETRIC_SHADOWS("Тени в объёме") { boolean get(AdvancedLightSettings s) { return s.volumetricShadowsEnabled(); } void flip(AdvancedLightSettings s) { s.setVolumetricShadowsEnabled(!s.volumetricShadowsEnabled()); } },
        VOLUMETRIC_GLASS("Стекло в объёме") { boolean get(AdvancedLightSettings s) { return s.volumetricColoredGlassEnabled(); } void flip(AdvancedLightSettings s) { s.setVolumetricColoredGlassEnabled(!s.volumetricColoredGlassEnabled()); } };

        final String title;
        Toggle(String title) { this.title = title; }
        abstract boolean get(AdvancedLightSettings settings);
        abstract void flip(AdvancedLightSettings settings);
    }

    private static final class Hitbox {
        final int x, y, w, h; final Kind kind; final Tab tab; final Section section; final QualityPreset quality;
        final Toggle toggle; final LightDirectionPreset direction; final Parameter parameter;
        private Hitbox(int x, int y, int w, int h, Kind kind, Tab tab, Section section, QualityPreset quality,
                       Toggle toggle, LightDirectionPreset direction, Parameter parameter) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.kind = kind; this.tab = tab; this.section = section;
            this.quality = quality; this.toggle = toggle; this.direction = direction; this.parameter = parameter;
        }
        static Hitbox tab(int x, int y, int w, int h, Tab v) { return new Hitbox(x,y,w,h,Kind.TAB,v,null,null,null,null,null); }
        static Hitbox section(int x, int y, int w, int h, Section v) { return new Hitbox(x,y,w,h,Kind.SECTION,null,v,null,null,null,null); }
        static Hitbox quality(int x, int y, int w, int h, QualityPreset v) { return new Hitbox(x,y,w,h,Kind.QUALITY,null,null,v,null,null,null); }
        static Hitbox toggle(int x, int y, int w, int h, Toggle v) { return new Hitbox(x,y,w,h,Kind.TOGGLE,null,null,null,v,null,null); }
        static Hitbox direction(int x, int y, int w, int h, LightDirectionPreset v) { return new Hitbox(x,y,w,h,Kind.DIRECTION,null,null,null,null,v,null); }
        static Hitbox parameter(int x, int y, int w, int h, Kind kind, Parameter v) { return new Hitbox(x,y,w,h,kind,null,null,null,null,null,v); }
        static Hitbox simple(int x, int y, int w, int h, Kind kind) { return new Hitbox(x,y,w,h,kind,null,null,null,null,null,null); }
    }
}
