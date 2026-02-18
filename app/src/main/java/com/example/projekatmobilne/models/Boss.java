package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class Boss {

    private String id;
    private String userId;
    private int level;
    private int maxHp;
    private int coins;
    private boolean defeated;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Boss() {}

    public Boss(String userId, int level) {
        this.userId = userId;
        this.level = level;
        this.maxHp = calculateHp(level);
        this.coins = calculateCoins(level);
        this.defeated = false;
    }

    // ===================================================
    // STATIČKE FORMULE
    // ===================================================

    public static int calculateHp(int level) {
        if (level <= 1) return 200;
        int hp = 200;
        for (int i = 1; i < level; i++) {
            hp = hp * 2 + hp / 2;
        }
        return hp;
    }

    public static int calculateCoins(int level) {
        if (level <= 1) return 200;
        double coins = 200;
        for (int i = 1; i < level; i++) {
            coins = coins * 1.2;
        }
        return (int) Math.round(coins);
    }

    // ===================================================
    // FIRESTORE KONVERZIJA
    // ===================================================

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("level", level);
        map.put("maxHp", maxHp);
        map.put("coins", coins);
        map.put("defeated", defeated);
        return map;
    }

    // ===================================================
    // GETTERI I SETTERI
    // ===================================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isDefeated() { return defeated; }
    public void setDefeated(boolean defeated) { this.defeated = defeated; }
}