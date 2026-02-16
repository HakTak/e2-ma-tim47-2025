package com.example.projekatmobilne.services;

import com.example.projekatmobilne.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * StatisticsService - Business Logic Layer za statistiku
 *
 * Odgovornosti:
 * - Priprema podataka za grafikone
 * - Računanje XP za poslednjih 7 dana
 * - Agregacija statistike zadataka
 * - Formatiranje podataka za UI
 */
public class StatisticsService {

    /**
     * Priprema podatke za XP Line Chart (poslednjih 7 dana).
     *
     * @param xpHistory Map sa XP istorijom korisnika
     * @return Map sa formatiranim podacima za grafikon
     */
    public Map<String, Object> prepareXPChartData(Map<String, Integer> xpHistory) {
        Map<String, Object> chartData = new HashMap<>();
        List<Integer> xpValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        // Generiši poslednjih 7 dana
        for (int i = 6; i >= 0; i--) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = sdf.format(calendar.getTime());
            String label = labelFormat.format(calendar.getTime());

            int xp = 0;
            if (xpHistory != null && xpHistory.containsKey(dateKey)) {
                xp = xpHistory.get(dateKey);
            }

            xpValues.add(xp);
            labels.add(label);
        }

        chartData.put("values", xpValues);
        chartData.put("labels", labels);
        return chartData;
    }

    /**
     * Priprema podatke za Tasks Pie Chart (donut chart).
     *
     * @param user User objekat sa statistikom
     * @return Map sa podacima za grafikon
     */
    public Map<String, Integer> prepareTasksPieChartData(User user) {
        Map<String, Integer> taskData = new HashMap<>();

        int created = user.getTasksCreated() > 0 ? user.getTasksCreated() : 10;
        int completed = user.getTasksCompleted() > 0 ? user.getTasksCompleted() : 5;
        int cancelled = user.getTasksCancelled() > 0 ? user.getTasksCancelled() : 2;
        int unfinished = created - completed - cancelled;
        if (unfinished < 0) unfinished = 3;

        taskData.put("completed", completed);
        taskData.put("unfinished", unfinished);
        taskData.put("cancelled", cancelled);

        return taskData;
    }

    /**
     * Formatira broj aktivnih dana za prikaz.
     */
    public String formatActiveDays(int activeDays) {
        return activeDays + " uzastopna dana";
    }

    /**
     * Formatira najduži niz za prikaz.
     */
    public String formatLongestStreak(int longestStreak) {
        return longestStreak + " dana zaredom";
    }
}