package com.czo.contamination;

public enum ContaminationType {
    RADIATION("radiation", "Радиация", "☢"),
    PSY("psy", "Пси-излучение", "Ψ"),
    BIOLOGICAL("bio", "Биологическое", "☣"),
    THERMAL("thermal", "Термальное", "♨");

    private final String id;
    private final String displayName;
    private final String symbol;

    ContaminationType(String id, String displayName, String symbol) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }
}
