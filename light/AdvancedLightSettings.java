package com.czo.light;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Per-source configuration for Advanced lights.
 * Every numeric renderer option is stored independently and is safe to expose as
 * both a slider and an exact numeric input.
 */
public final class AdvancedLightSettings {
    public enum Section {
        SHADOWS("Тени"), LIGHT_SHAPE("Форма света"), COLORED_GLASS("Цветное стекло"),
        VOLUMETRIC("Объёмный свет"), SPOTLIGHT("Прожектор"), COLOR("Цвет");

        private final String title;
        Section(String title) { this.title = title; }
        public String title() { return title; }
    }

    public enum QualityPreset {
        PERFORMANCE("Производительность"), BALANCED("Сбалансированный"),
        HIGH("Высокое качество"), CINEMATIC("Кинематографический"), CUSTOM("Пользовательский");

        private final String title;
        QualityPreset(String title) { this.title = title; }
        public String title() { return title; }
    }

    public enum Parameter {
        SHADOW_INTENSITY("Интенсивность теней", Section.SHADOWS, 0, 1, .85, .01, 2),
        SHADOW_SOFTNESS("Мягкость краёв", Section.SHADOWS, 0, 1, .35, .01, 2),
        SHADOW_BLUR_RADIUS("Радиус размытия", Section.SHADOWS, 0, 32, 8, 1, 0),
        SHADOW_MAP_SIZE("Разрешение карты", Section.SHADOWS, 512, 8192, 2048, 512, 0),
        SHADOW_DISTANCE("Дальность теней", Section.SHADOWS, 1, 256, 48, 1, 0),
        SHADOW_BIAS("Shadow Bias", Section.SHADOWS, 0, .05, .0015, .0001, 4),
        SHADOW_NORMAL_BIAS("Normal Bias", Section.SHADOWS, 0, .1, .015, .001, 3),
        CONTACT_SHADOW_DISTANCE("Контактная дальность", Section.SHADOWS, 0, 32, 6, .25, 2),
        SHADOW_GEOMETRY_ACCURACY("Точность геометрии", Section.SHADOWS, 0, 1, .75, .01, 2),

        SOURCE_RADIUS("Радиус источника", Section.LIGHT_SHAPE, 0, 16, .5, .05, 2),
        LIGHT_SOFTNESS("Мягкость света", Section.LIGHT_SHAPE, 0, 1, .25, .01, 2),
        LIGHT_TRANSITION_RADIUS("Радиус перехода", Section.LIGHT_SHAPE, 0, 32, 4, .1, 1),
        LIGHT_UNIFORMITY("Равномерность", Section.LIGHT_SHAPE, 0, 1, .8, .01, 2),
        LIGHT_SCATTERING("Рассеивание", Section.LIGHT_SHAPE, 0, 1, .15, .01, 2),
        CENTER_INTENSITY("Интенсивность центра", Section.LIGHT_SHAPE, 0, 5, 1, .01, 2),
        EDGE_INTENSITY("Интенсивность краёв", Section.LIGHT_SHAPE, 0, 5, .6, .01, 2),
        ATTENUATION_CONSTANT("Постоянное затухание", Section.LIGHT_SHAPE, 0, 4, 1, .01, 2),
        ATTENUATION_LINEAR("Линейное затухание", Section.LIGHT_SHAPE, 0, 4, .09, .01, 2),
        ATTENUATION_QUADRATIC("Квадратичное затухание", Section.LIGHT_SHAPE, 0, 4, .032, .001, 3),
        MAX_DISTANCE("Максимальная дальность", Section.LIGHT_SHAPE, 1, 256, 32, 1, 0),

        GLASS_FILTER_STRENGTH("Сила фильтрации", Section.COLORED_GLASS, 0, 1, 1, .01, 2),
        GLASS_ABSORPTION("Поглощение света", Section.COLORED_GLASS, 0, 1, .12, .01, 2),
        GLASS_BRIGHTNESS_RETENTION("Сохранение яркости", Section.COLORED_GLASS, 0, 1, .88, .01, 2),
        GLASS_SATURATION("Насыщенность результата", Section.COLORED_GLASS, 0, 2, 1, .01, 2),
        GLASS_MAX_LAYERS("Максимум слоёв", Section.COLORED_GLASS, 1, 32, 8, 1, 0),
        GLASS_INTERNAL_SCATTERING("Рассеивание в стекле", Section.COLORED_GLASS, 0, 1, .08, .01, 2),
        GLASS_QUALITY("Качество расчёта", Section.COLORED_GLASS, 1, 4, 2, 1, 0),

        VOLUME_INTENSITY("Интенсивность объёма", Section.VOLUMETRIC, 0, 5, .7, .01, 2),
        VOLUME_DENSITY("Плотность", Section.VOLUMETRIC, 0, 1, .12, .005, 3),
        VOLUME_DISTANCE("Дальность объёма", Section.VOLUMETRIC, 1, 256, 48, 1, 0),
        VOLUME_START_DISTANCE("Начальная дистанция", Section.VOLUMETRIC, 0, 64, 0, .25, 2),
        VOLUME_SCATTERING("Рассеивание объёма", Section.VOLUMETRIC, 0, 1, .55, .01, 2),
        VOLUME_ABSORPTION("Поглощение объёма", Section.VOLUMETRIC, 0, 1, .08, .01, 2),
        VOLUME_CENTER_BRIGHTNESS("Яркость центра", Section.VOLUMETRIC, 0, 4, 1, .01, 2),
        VOLUME_EDGE_SOFTNESS("Мягкость границ", Section.VOLUMETRIC, 0, 1, .45, .01, 2),
        VOLUME_STEPS("Количество шагов", Section.VOLUMETRIC, 4, 128, 32, 1, 0),
        VOLUME_EXTRA_SAMPLES("Доп. выборки", Section.VOLUMETRIC, 0, 16, 2, 1, 0),
        VOLUME_NOISE("Шум объёма", Section.VOLUMETRIC, 0, 1, .15, .01, 2),
        VOLUME_NOISE_SPEED("Скорость шума", Section.VOLUMETRIC, 0, 4, .25, .01, 2),

        SPOT_INNER_ANGLE("Внутренний угол", Section.SPOTLIGHT, 0, 179, 24, 1, 0),
        SPOT_OUTER_ANGLE("Внешний угол", Section.SPOTLIGHT, 0, 179, 36, 1, 0),
        SPOT_EDGE_SOFTNESS("Мягкость края", Section.SPOTLIGHT, 0, 1, .35, .01, 2),
        SPOT_LENGTH("Длина луча", Section.SPOTLIGHT, 1, 256, 48, 1, 0),
        SPOT_START_WIDTH("Ширина начала", Section.SPOTLIGHT, 0, 16, 0, .05, 2),
        SPOT_EXPANSION("Расширение луча", Section.SPOTLIGHT, 0, 4, 1, .01, 2),
        SPOT_YAW("Yaw", Section.SPOTLIGHT, -180, 180, 0, 1, 0),
        SPOT_PITCH("Pitch", Section.SPOTLIGHT, -90, 90, 0, 1, 0),

        COLOR_RED("Красный канал", Section.COLOR, 0, 255, 255, 1, 0),
        COLOR_GREEN("Зелёный канал", Section.COLOR, 0, 255, 255, 1, 0),
        COLOR_BLUE("Синий канал", Section.COLOR, 0, 255, 255, 1, 0),
        COLOR_TEMPERATURE("Температура, K", Section.COLOR, 1000, 20000, 6500, 50, 0),
        COLOR_SATURATION("Насыщенность", Section.COLOR, 0, 2, 1, .01, 2),
        COLOR_EXPOSURE("Экспозиция", Section.COLOR, -8, 8, 0, .05, 2),
        COLOR_INTENSITY("Цветовая интенсивность", Section.COLOR, 0, 8, 1, .01, 2),
        HIGHLIGHT_CLAMP("Ограничение пересвета", Section.COLOR, 0, 32, 8, .1, 1);

        private final String title;
        private final Section section;
        private final double min, max, defaultValue, step;
        private final int decimals;

        Parameter(String title, Section section, double min, double max, double defaultValue, double step, int decimals) {
            this.title = title; this.section = section; this.min = min; this.max = max;
            this.defaultValue = defaultValue; this.step = step; this.decimals = decimals;
        }

        public String title() { return title; }
        public Section section() { return section; }
        public double min() { return min; }
        public double max() { return max; }
        public double defaultValue() { return defaultValue; }
        public int decimals() { return decimals; }
        double normalize(double value) {
            double clamped = Math.max(min, Math.min(max, value));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }
    }

    private final EnumMap<Parameter, Double> values = new EnumMap<>(Parameter.class);
    private boolean shadowsEnabled = true, contactShadowsEnabled = true, transparentShadowsEnabled;
    private boolean coloredGlassEnabled, glassThicknessEnabled = true, glassColorMixingEnabled = true;
    private boolean volumetricEnabled, volumetricShadowsEnabled = true, volumetricColoredGlassEnabled = true;
    private LightDirectionPreset directionPreset = LightDirectionPreset.SOUTH;
    private QualityPreset qualityPreset = QualityPreset.BALANCED;

    public AdvancedLightSettings() { resetAll(); }
    public AdvancedLightSettings(AdvancedLightSettings other) { copyFrom(other); }
    public double get(Parameter parameter) { return values.getOrDefault(parameter, parameter.defaultValue()); }

    public void set(Parameter parameter, double value) {
        values.put(parameter, parameter.normalize(value)); qualityPreset = QualityPreset.CUSTOM;
        if (parameter == Parameter.SPOT_YAW || parameter == Parameter.SPOT_PITCH) directionPreset = LightDirectionPreset.CUSTOM;
        if (get(Parameter.SPOT_INNER_ANGLE) > get(Parameter.SPOT_OUTER_ANGLE)) {
            if (parameter == Parameter.SPOT_INNER_ANGLE) values.put(Parameter.SPOT_OUTER_ANGLE, get(parameter));
            else values.put(Parameter.SPOT_INNER_ANGLE, get(parameter));
        }
    }

    public void reset(Parameter parameter) { values.put(parameter, parameter.defaultValue()); qualityPreset = QualityPreset.CUSTOM; }
    public void reset(Section section) { for (Parameter p : Parameter.values()) if (p.section() == section) values.put(p, p.defaultValue()); qualityPreset = QualityPreset.CUSTOM; }
    public void resetAll() {
        values.clear(); for (Parameter p : Parameter.values()) values.put(p, p.defaultValue());
        shadowsEnabled = true; contactShadowsEnabled = true; transparentShadowsEnabled = false;
        coloredGlassEnabled = false; glassThicknessEnabled = true; glassColorMixingEnabled = true;
        volumetricEnabled = false; volumetricShadowsEnabled = true; volumetricColoredGlassEnabled = true;
        directionPreset = LightDirectionPreset.SOUTH; qualityPreset = QualityPreset.BALANCED; applyDirectionPreset(directionPreset);
    }

    public void copyFrom(AdvancedLightSettings other) {
        values.clear(); values.putAll(other.values);
        shadowsEnabled = other.shadowsEnabled; contactShadowsEnabled = other.contactShadowsEnabled;
        transparentShadowsEnabled = other.transparentShadowsEnabled; coloredGlassEnabled = other.coloredGlassEnabled;
        glassThicknessEnabled = other.glassThicknessEnabled; glassColorMixingEnabled = other.glassColorMixingEnabled;
        volumetricEnabled = other.volumetricEnabled; volumetricShadowsEnabled = other.volumetricShadowsEnabled;
        volumetricColoredGlassEnabled = other.volumetricColoredGlassEnabled;
        directionPreset = other.directionPreset; qualityPreset = other.qualityPreset;
    }

    public AdvancedLightSettings copy() { return new AdvancedLightSettings(this); }
    public Map<Parameter, Double> valuesView() { return Collections.unmodifiableMap(values); }

    public void applyDirectionPreset(LightDirectionPreset preset) {
        directionPreset = preset;
        if (preset != LightDirectionPreset.CUSTOM) {
            values.put(Parameter.SPOT_YAW, preset.yaw()); values.put(Parameter.SPOT_PITCH, preset.pitch());
        }
    }

    public void applyQualityPreset(QualityPreset preset) {
        qualityPreset = preset;
        switch (preset) {
            case PERFORMANCE -> setPreset(1024, 2, 32, 2, 1, 12, 0);
            case BALANCED -> setPreset(2048, 8, 48, 8, 2, 32, 2);
            case HIGH -> setPreset(4096, 16, 96, 16, 3, 64, 4);
            case CINEMATIC -> setPreset(8192, 24, 160, 32, 4, 128, 8);
            case CUSTOM -> { }
        }
        qualityPreset = preset;
    }

    private void setPreset(double map, double blur, double distance, double layers, double glassQuality, double steps, double samples) {
        values.put(Parameter.SHADOW_MAP_SIZE, map); values.put(Parameter.SHADOW_BLUR_RADIUS, blur);
        values.put(Parameter.SHADOW_DISTANCE, distance); values.put(Parameter.GLASS_MAX_LAYERS, layers);
        values.put(Parameter.GLASS_QUALITY, glassQuality); values.put(Parameter.VOLUME_STEPS, steps);
        values.put(Parameter.VOLUME_EXTRA_SAMPLES, samples);
    }

    public String estimatedLoad() {
        double score = 0;
        if (shadowsEnabled) score += get(Parameter.SHADOW_MAP_SIZE) / 2048 + get(Parameter.SHADOW_BLUR_RADIUS) / 16 + get(Parameter.SHADOW_DISTANCE) / 96 + (contactShadowsEnabled ? .5 : 0) + (transparentShadowsEnabled ? .75 : 0);
        if (coloredGlassEnabled) score += get(Parameter.GLASS_MAX_LAYERS) / 8 + get(Parameter.GLASS_QUALITY) / 2;
        if (volumetricEnabled) score += get(Parameter.VOLUME_STEPS) / 24 + get(Parameter.VOLUME_EXTRA_SAMPLES) / 4 + get(Parameter.VOLUME_DISTANCE) / 96;
        return score < 2 ? "Низкая" : score < 4 ? "Средняя" : score < 7 ? "Высокая" : "Очень высокая";
    }

    public boolean shadowsEnabled() { return shadowsEnabled; }
    public void setShadowsEnabled(boolean v) { shadowsEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean contactShadowsEnabled() { return contactShadowsEnabled; }
    public void setContactShadowsEnabled(boolean v) { contactShadowsEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean transparentShadowsEnabled() { return transparentShadowsEnabled; }
    public void setTransparentShadowsEnabled(boolean v) { transparentShadowsEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean coloredGlassEnabled() { return coloredGlassEnabled; }
    public void setColoredGlassEnabled(boolean v) { coloredGlassEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean glassThicknessEnabled() { return glassThicknessEnabled; }
    public void setGlassThicknessEnabled(boolean v) { glassThicknessEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean glassColorMixingEnabled() { return glassColorMixingEnabled; }
    public void setGlassColorMixingEnabled(boolean v) { glassColorMixingEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean volumetricEnabled() { return volumetricEnabled; }
    public void setVolumetricEnabled(boolean v) { volumetricEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean volumetricShadowsEnabled() { return volumetricShadowsEnabled; }
    public void setVolumetricShadowsEnabled(boolean v) { volumetricShadowsEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public boolean volumetricColoredGlassEnabled() { return volumetricColoredGlassEnabled; }
    public void setVolumetricColoredGlassEnabled(boolean v) { volumetricColoredGlassEnabled = v; qualityPreset = QualityPreset.CUSTOM; }
    public LightDirectionPreset directionPreset() { return directionPreset; }
    public QualityPreset qualityPreset() { return qualityPreset; }
}
