package com.czo.contamination;

public final class ContaminationData {
    public static final double MAX_CONTAMINATION = 1_000_000.0D;

    private double radiation;
    private double psy;
    private double biological;
    private double thermal;

    public double get(ContaminationType type) {
        return switch (type) {
            case RADIATION -> radiation;
            case PSY -> psy;
            case BIOLOGICAL -> biological;
            case THERMAL -> thermal;
        };
    }

    public void set(ContaminationType type, double value) {
        double clamped = clamp(value, 0.0D, MAX_CONTAMINATION);

        switch (type) {
            case RADIATION -> radiation = clamped;
            case PSY -> psy = clamped;
            case BIOLOGICAL -> biological = clamped;
            case THERMAL -> thermal = clamped;
        }
    }

    public void add(ContaminationType type, double amount) {
        set(type, get(type) + amount);
    }

    public void reduce(ContaminationType type, double amount) {
        set(type, get(type) - amount);
    }

    public boolean isEmpty() {
        return radiation <= 0.0D
                && psy <= 0.0D
                && biological <= 0.0D
                && thermal <= 0.0D;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
