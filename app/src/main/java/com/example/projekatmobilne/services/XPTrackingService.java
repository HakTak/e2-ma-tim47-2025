package com.example.projekatmobilne.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.projekatmobilne.enums.Difficulty;
import com.example.projekatmobilne.enums.Importance;

import java.util.Calendar;

public class XPTrackingService {

    private static final String TAG = "XP_TRACKING";
    private static final String PREFS_NAME = "xp_tracking_prefs";

    // Keys za SharedPreferences
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_LAST_RESET_WEEK = "last_reset_week";
    private static final String KEY_LAST_RESET_MONTH = "last_reset_month";

    // Counters
    private static final String KEY_VERY_EASY_COUNT = "very_easy_count";
    private static final String KEY_EASY_COUNT = "easy_count";
    private static final String KEY_HARD_COUNT = "hard_count";
    private static final String KEY_EXTREME_DIFF_COUNT = "extreme_diff_count";

    private static final String KEY_NORMAL_COUNT = "normal_count";
    private static final String KEY_IMPORTANT_COUNT = "important_count";
    private static final String KEY_EXTREME_IMP_COUNT = "extreme_imp_count";
    private static final String KEY_SPECIAL_COUNT = "special_count";

    // Limiti
    private static final int LIMIT_VERY_EASY_DAILY = 5;
    private static final int LIMIT_EASY_DAILY = 5;
    private static final int LIMIT_HARD_DAILY = 2;
    private static final int LIMIT_EXTREME_DIFF_WEEKLY = 1;

    private static final int LIMIT_NORMAL_DAILY = 5;
    private static final int LIMIT_IMPORTANT_DAILY = 5;
    private static final int LIMIT_EXTREME_IMP_DAILY = 2;
    private static final int LIMIT_SPECIAL_MONTHLY = 1;

    private final SharedPreferences prefs;

    public XPTrackingService(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        resetCountersIfNeeded();
    }

    // ===================================================
    // VALIDACIJA I DODELA XP
    // ===================================================

    /**
     * Izračunava koliko XP korisnik ZAISTA dobija sa zadatka.
     * Proverava limite i vraća (difficultyXP, importanceXP, ukupnoXP).
     */
    public XPResult calculateAwardedXP(Difficulty difficulty, Importance importance) {
        resetCountersIfNeeded();

        int difficultyXP = getDifficultyXP(difficulty);
        int importanceXP = getImportanceXP(importance);

        Log.d(TAG, "=== Provera XP za Difficulty: " + difficulty + " (" + difficultyXP + " XP) | Importance: " + importance + " (" + importanceXP + " XP)");

        return new XPResult(difficultyXP, importanceXP);
    }

    /**
     * Vraća XP za Difficulty ili 0 ako je limit dostignut
     */
    private int getDifficultyXP(Difficulty difficulty) {
        switch (difficulty) {
            case VERY_EASY:
                return checkAndIncrementCounter(KEY_VERY_EASY_COUNT, LIMIT_VERY_EASY_DAILY, "VERY_EASY")
                        ? difficulty.getXp() : 0;
            case EASY:
                return checkAndIncrementCounter(KEY_EASY_COUNT, LIMIT_EASY_DAILY, "EASY")
                        ? difficulty.getXp() : 0;
            case HARD:
                return checkAndIncrementCounter(KEY_HARD_COUNT, LIMIT_HARD_DAILY, "HARD")
                        ? difficulty.getXp() : 0;
            case EXTREME:
                return checkAndIncrementCounter(KEY_EXTREME_DIFF_COUNT, LIMIT_EXTREME_DIFF_WEEKLY, "EXTREME")
                        ? difficulty.getXp() : 0;
            default:
                return 0;
        }
    }

    /**
     * Vraća XP za Importance ili 0 ako je limit dostignut
     */
    private int getImportanceXP(Importance importance) {
        switch (importance) {
            case NORMAL:
                return checkAndIncrementCounter(KEY_NORMAL_COUNT, LIMIT_NORMAL_DAILY, "NORMAL")
                        ? importance.getXp() : 0;
            case IMPORTANT:
                return checkAndIncrementCounter(KEY_IMPORTANT_COUNT, LIMIT_IMPORTANT_DAILY, "IMPORTANT")
                        ? importance.getXp() : 0;
            case EXTREME:
                return checkAndIncrementCounter(KEY_EXTREME_IMP_COUNT, LIMIT_EXTREME_IMP_DAILY, "EXTREME_IMP")
                        ? importance.getXp() : 0;
            case SPECIAL:
                return checkAndIncrementCounter(KEY_SPECIAL_COUNT, LIMIT_SPECIAL_MONTHLY, "SPECIAL")
                        ? importance.getXp() : 0;
            default:
                return 0;
        }
    }

    /**
     * Proverava da li je counter ispod limita, inkrementira ga ako jeste.
     * @return true ako je XP dodeljen, false ako je limit dostignut
     */
    private boolean checkAndIncrementCounter(String key, int limit, String label) {
        int currentCount = prefs.getInt(key, 0);

        if (currentCount >= limit) {
            Log.d(TAG, label + " limit dostignut (" + currentCount + "/" + limit + ") - XP se NE dodeljuje");
            return false;
        }

        prefs.edit().putInt(key, currentCount + 1).apply();
        Log.d(TAG, label + " counter: " + (currentCount + 1) + "/" + limit + " - XP se dodeljuje");
        return true;
    }

    // ===================================================
    // RESET LOGIKA
    // ===================================================

    /**
     * Resetuje countere ako je prošao dan/nedelja/mesec
     */
    private void resetCountersIfNeeded() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        String todayKey = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
        String thisWeekKey = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.WEEK_OF_YEAR);
        String thisMonthKey = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);

        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");
        String lastResetWeek = prefs.getString(KEY_LAST_RESET_WEEK, "");
        String lastResetMonth = prefs.getString(KEY_LAST_RESET_MONTH, "");

        // Dnevni reset
        if (!todayKey.equals(lastResetDate)) {
            Log.d(TAG, "Novi dan detektovan - resetujem dnevne countere");
            resetDailyCounters();
            prefs.edit().putString(KEY_LAST_RESET_DATE, todayKey).apply();
        }

        // Nedeljni reset
        if (!thisWeekKey.equals(lastResetWeek)) {
            Log.d(TAG, "Nova nedelja detektovana - resetujem nedeljne countere");
            resetWeeklyCounters();
            prefs.edit().putString(KEY_LAST_RESET_WEEK, thisWeekKey).apply();
        }

        // Mesečni reset
        if (!thisMonthKey.equals(lastResetMonth)) {
            Log.d(TAG, "Novi mesec detektovan - resetujem mesečne countere");
            resetMonthlyCounters();
            prefs.edit().putString(KEY_LAST_RESET_MONTH, thisMonthKey).apply();
        }
    }

    private void resetDailyCounters() {
        prefs.edit()
                .putInt(KEY_VERY_EASY_COUNT, 0)
                .putInt(KEY_EASY_COUNT, 0)
                .putInt(KEY_HARD_COUNT, 0)
                .putInt(KEY_NORMAL_COUNT, 0)
                .putInt(KEY_IMPORTANT_COUNT, 0)
                .putInt(KEY_EXTREME_IMP_COUNT, 0)
                .apply();
    }

    private void resetWeeklyCounters() {
        prefs.edit()
                .putInt(KEY_EXTREME_DIFF_COUNT, 0)
                .apply();
    }

    private void resetMonthlyCounters() {
        prefs.edit()
                .putInt(KEY_SPECIAL_COUNT, 0)
                .apply();
    }

    // ===================================================
    // RESULT CLASS
    // ===================================================

    public static class XPResult {
        public final int difficultyXP;
        public final int importanceXP;
        public final int totalXP;

        public XPResult(int difficultyXP, int importanceXP) {
            this.difficultyXP = difficultyXP;
            this.importanceXP = importanceXP;
            this.totalXP = difficultyXP + importanceXP;
        }

        public boolean hasEarnedXP() {
            return totalXP > 0;
        }

        public String getBreakdown() {
            if (totalXP == 0) {
                return "0 XP (limit dostignut)";
            }
            return totalXP + " XP (Težina: " + difficultyXP + " + Važnost: " + importanceXP + ")";
        }
    }
}