package com.example.projekatmobilne.Enum;

public enum Difficulty {
    VERY_EASY(1),
    EASY(3),
    HARD(7),
    EXTREME(20);

    private final int xp;

    Difficulty(int xp) {
        this.xp = xp;
    }

    public int getXp() {
        return xp;
    }

}