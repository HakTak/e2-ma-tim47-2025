package com.example.projekatmobilne.services;

import java.util.HashMap;
import java.util.Map;

/**
 * LevelService - Business Logic Layer za napredovanje kroz nivoe
 *
 * Odgovornosti:
 * - Računanje XP threshold-a za svaki nivo
 * - Računanje PP nagrade za svaki nivo
 * - Dodela titula
 * - Računanje XP vrednosti za težinu i bitnost zadataka
 */
public class LevelService {

    // Bazne XP vrednosti za bitnost (nivo 0)
    private static final int BASE_NORMAL = 1;
    private static final int BASE_IMPORTANT = 3;
    private static final int BASE_EXTREME_IMPORTANT = 10;
    private static final int BASE_SPECIAL = 100;

    /**
     * Računa kumulativni XP threshold za dostizanje datog nivoa.
     *
     * Nivo 0 = 0 XP (početno stanje)
     * Nivo 1 = 200 XP
     * Nivo 2 = 500 XP
     * Nivo 3 = 1250 XP
     */
    public int calculateXPThreshold(int level) {
        if (level <= 0) return 0;
        if (level == 1) return 200;

        int previous = calculateXPThreshold(level - 1);
        int raw = previous * 2 + previous / 2;

        // Zaokruži na prvu narednu stotinu
        return (int)(Math.ceil(raw / 100.0) * 100);
    }

    /**
     * Računa PP nagradu za prelazak na dati nivo.
     *
     * Nivo 1 = 40 PP
     * Nivo 2 = 70 PP
     * Nivo 3 = 123 PP
     */
    public int calculatePPReward(int level) {
        if (level <= 0) return 0;
        if (level == 1) return 40;

        int previousPP = calculatePPReward(level - 1);
        return Math.round(previousPP + (3f / 4f) * previousPP);
    }

    /**
     * Vraća titulu za dati nivo.
     * Prva 3 nivoa imaju fiksne titule, ostali generišu automatski.
     */
    public String getTitleForLevel(int level) {
        switch (level) {
            case 0: return "Početnik";
            case 1: return "Iskusni Borac";
            case 2: return "Veteran";
            case 3: return "Gospodar";
            default: return "Legenda " + level + ". nivoa";
        }
    }

    /**
     * Proverava da li korisnik treba da pređe na sledeći nivo.
     *
     * @param currentXP   Trenutni XP korisnika
     * @param currentLevel Trenutni nivo korisnika
     * @return true ako je dostignuto dovoljno XP za sledeći nivo
     */
    public boolean shouldLevelUp(int currentXP, int currentLevel) {
        int nextThreshold = calculateXPThreshold(currentLevel + 1);
        return currentXP >= nextThreshold;
    }

    /**
     * Vraća XP vrednosti za bitnost zadataka na datom nivou.
     * Kolega koristi ove vrednosti u TaskService-u.
     */
    public Map<String, Integer> getImportanceXPValues(int level) {
        int normal = BASE_NORMAL;
        int important = BASE_IMPORTANT;
        int extreme = BASE_EXTREME_IMPORTANT;
        int special = BASE_SPECIAL;

        for (int i = 0; i < level; i++) {
            normal = Math.round(normal + normal / 2f);
            important = Math.round(important + important / 2f);
            extreme = Math.round(extreme + extreme / 2f);
            special = Math.round(special + special / 2f);
        }

        Map<String, Integer> values = new HashMap<>();
        values.put("NORMAL", normal);
        values.put("IMPORTANT", important);
        values.put("EXTREME", extreme);
        values.put("SPECIAL", special);
        return values;
    }
}