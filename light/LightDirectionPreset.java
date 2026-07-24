package com.czo.light;

public enum LightDirectionPreset {
    UP("Вверх", 0.0D, -90.0D, 0.0D, 1.0D, 0.0D),
    DOWN("Вниз", 0.0D, 90.0D, 0.0D, -1.0D, 0.0D),
    NORTH("Север", 180.0D, 0.0D, 0.0D, 0.0D, -1.0D),
    SOUTH("Юг", 0.0D, 0.0D, 0.0D, 0.0D, 1.0D),
    WEST("Запад", 90.0D, 0.0D, -1.0D, 0.0D, 0.0D),
    EAST("Восток", -90.0D, 0.0D, 1.0D, 0.0D, 0.0D),
    CUSTOM("Пользовательское", 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    private final String title;
    private final double yaw;
    private final double pitch;
    private final double x;
    private final double y;
    private final double z;

    LightDirectionPreset(String title, double yaw, double pitch, double x, double y, double z) {
        this.title = title;
        this.yaw = yaw;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String title() { return title; }
    public double yaw() { return yaw; }
    public double pitch() { return pitch; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
}
