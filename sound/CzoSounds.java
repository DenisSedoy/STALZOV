package com.czo.sound;

import com.czo.CZO;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Lightweight sound handles for CZO anomaly/contamination audio.
 *
 * <p>NeoForge registration mappings differ between local builds, so the project keeps these
 * as variable-range SoundEvent handles resolved through assets/czo/sounds.json.</p>
 */
public final class CzoSounds {
    public static final Identifier ANOMALY_GRAVI_IDLE_ID = id("anomaly.gravi.idle");
    public static final Identifier ANOMALY_GRAVI_ACTIVE_ID = id("anomaly.gravi.active");
    public static final Identifier ANOMALY_GRAVI_MINCER_ACTIVE_ID = id("anomaly.gravi.mincer_active");
    public static final Identifier ANOMALY_GRAVI_RUMBLE_ID = id("anomaly.gravi.rumble");

    public static final Identifier ANOMALY_THERMAL_IDLE_ID = id("anomaly.thermal.idle");
    public static final Identifier ANOMALY_THERMAL_ACTIVE_ID = id("anomaly.thermal.active");
    public static final Identifier ANOMALY_STEAM_IDLE_ID = id("anomaly.steam.idle");
    public static final Identifier ANOMALY_STEAM_ACTIVE_ID = id("anomaly.steam.active");

    public static final Identifier ANOMALY_GAS_IDLE_ID = id("anomaly.gas.idle");
    public static final Identifier ANOMALY_GAS_ACTIVE_ID = id("anomaly.gas.active");

    public static final Identifier ANOMALY_ELECTRO_IDLE_ID = id("anomaly.electro.idle");
    public static final Identifier ANOMALY_ELECTRO_ACTIVE_ID = id("anomaly.electro.active");

    public static final Identifier ANOMALY_ACID_IDLE_ID = id("anomaly.acid.idle");
    public static final Identifier ANOMALY_ACID_ACTIVE_ID = id("anomaly.acid.active");

    public static final Identifier RADIATION_GEIGER_ID = id("radiation.geiger");
    public static final Identifier RADIATION_GEIGER_1_ID = id("radiation.geiger_1");
    public static final Identifier RADIATION_GEIGER_2_ID = id("radiation.geiger_2");
    public static final Identifier RADIATION_GEIGER_3_ID = id("radiation.geiger_3");

    public static final Identifier DETECTOR_THERMAL_ID = id("detector.thermal");
    public static final Identifier DETECTOR_BIO_ID = id("detector.bio");

    public static final Identifier PSY_ENTER_ID = id("psy.enter");
    public static final Identifier PSY_LOOP_ID = id("psy.loop");

    public static final SoundEvent ANOMALY_GRAVI_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_GRAVI_IDLE_ID);
    public static final SoundEvent ANOMALY_GRAVI_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_GRAVI_ACTIVE_ID);
    public static final SoundEvent ANOMALY_GRAVI_MINCER_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_GRAVI_MINCER_ACTIVE_ID);
    public static final SoundEvent ANOMALY_GRAVI_RUMBLE = SoundEvent.createVariableRangeEvent(ANOMALY_GRAVI_RUMBLE_ID);

    public static final SoundEvent ANOMALY_THERMAL_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_THERMAL_IDLE_ID);
    public static final SoundEvent ANOMALY_THERMAL_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_THERMAL_ACTIVE_ID);
    public static final SoundEvent ANOMALY_STEAM_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_STEAM_IDLE_ID);
    public static final SoundEvent ANOMALY_STEAM_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_STEAM_ACTIVE_ID);

    public static final SoundEvent ANOMALY_GAS_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_GAS_IDLE_ID);
    public static final SoundEvent ANOMALY_GAS_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_GAS_ACTIVE_ID);

    public static final SoundEvent ANOMALY_ELECTRO_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_ELECTRO_IDLE_ID);
    public static final SoundEvent ANOMALY_ELECTRO_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_ELECTRO_ACTIVE_ID);

    public static final SoundEvent ANOMALY_ACID_IDLE = SoundEvent.createVariableRangeEvent(ANOMALY_ACID_IDLE_ID);
    public static final SoundEvent ANOMALY_ACID_ACTIVE = SoundEvent.createVariableRangeEvent(ANOMALY_ACID_ACTIVE_ID);

    public static final SoundEvent RADIATION_GEIGER = SoundEvent.createVariableRangeEvent(RADIATION_GEIGER_ID);
    public static final SoundEvent RADIATION_GEIGER_1 = SoundEvent.createVariableRangeEvent(RADIATION_GEIGER_1_ID);
    public static final SoundEvent RADIATION_GEIGER_2 = SoundEvent.createVariableRangeEvent(RADIATION_GEIGER_2_ID);
    public static final SoundEvent RADIATION_GEIGER_3 = SoundEvent.createVariableRangeEvent(RADIATION_GEIGER_3_ID);

    public static final SoundEvent DETECTOR_THERMAL = SoundEvent.createVariableRangeEvent(DETECTOR_THERMAL_ID);
    public static final SoundEvent DETECTOR_BIO = SoundEvent.createVariableRangeEvent(DETECTOR_BIO_ID);

    public static final SoundEvent PSY_ENTER = SoundEvent.createVariableRangeEvent(PSY_ENTER_ID);
    public static final SoundEvent PSY_LOOP = SoundEvent.createVariableRangeEvent(PSY_LOOP_ID);

    private CzoSounds() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CZO.MODID, path);
    }
}
