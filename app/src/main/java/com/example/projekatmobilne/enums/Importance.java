package com.example.projekatmobilne.enums;

public enum Importance {
    NORMAL(1),
    IMPORTANT(3),
    EXTREME(10),
    SPECIAL(100);

    private final int xp;

    Importance(int xp) {
        this.xp = xp;
    }

    public int getXp() {
        return xp;
    }
}

