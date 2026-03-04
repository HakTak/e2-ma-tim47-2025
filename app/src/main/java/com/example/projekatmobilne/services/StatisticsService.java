package com.example.projekatmobilne.services;

import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Category;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StatisticsService {

    private static final String COMPLETED_STATUS = "DONE";
    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final String FAILED_STATUS = "FAILED";

    // ===================================================
    // XP CHART (poslednjih 7 dana)
    // ===================================================

    public Map<String, Object> prepareXPChartData(Map<String, Integer> xpHistory) {
        Map<String, Object> chartData = new HashMap<>();
        List<Integer> xpValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

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

    // ===================================================
    // PIE CHART - broj zadataka
    // ===================================================

    public Map<String, Integer> prepareTasksPieChartData(User user, List<Task> tasks) {
        Map<String, Integer> taskData = new HashMap<>();

        if (tasks == null || tasks.isEmpty()) {
            taskData.put("completed", 0);
            taskData.put("cancelled", 0);
            taskData.put("unfinished", 0);
            taskData.put("active", 0);
            return taskData;
        }

        int completed = 0, cancelled = 0, unfinished = 0, active = 0;

        for (Task task : tasks) {
            Map<String, String> occurrences = task.getOccurrenceStatuses();

            if (occurrences == null || occurrences.isEmpty()) {
                // Nema occurrence-a → aktivan zadatak
                active++;
            } else {
                for (String status : occurrences.values()) {
                    if (COMPLETED_STATUS.equals(status)) completed++;
                    else if (CANCELLED_STATUS.equals(status)) cancelled++;
                    else if (FAILED_STATUS.equals(status)) unfinished++;
                }
            }
        }

        taskData.put("completed", completed);
        taskData.put("cancelled", cancelled);
        taskData.put("unfinished", unfinished);
        taskData.put("active", active);
        return taskData;
    }

    // ===================================================
    // BAR CHART - završeni zadaci po kategoriji
    // ===================================================

    public Map<String, Integer> prepareBarChartData(List<Task> tasks, List<Category> categories) {
        Map<String, Integer> result = new HashMap<>();
        for (Category cat : categories) {
            result.put(cat.getId(), 0);
        }

        if (tasks == null) return result;

        for (Task task : tasks) {
            String catId = task.getCategoryId();
            if (catId == null || !result.containsKey(catId)) continue;

            Map<String, String> occurrences = task.getOccurrenceStatuses();
            if (occurrences != null && !occurrences.isEmpty()) {
                int completedCount = 0;
                for (String status : occurrences.values()) {
                    if (COMPLETED_STATUS.equals(status)) completedCount++;
                }
                result.put(catId, result.get(catId) + completedCount);
            } else {
                if (task.getStatus() == TaskStatus.DONE) {
                    result.put(catId, result.get(catId) + 1);
                }
            }
        }

        return result;
    }

    // ===================================================
    // LINE CHART - prosječna težina završenih zadataka
    // ===================================================

    public Map<String, Object> prepareDifficultyChartData(List<Task> tasks) {
        Map<String, Object> chartData = new HashMap<>();
        List<Float> avgXpValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = sdf.format(calendar.getTime());
            String label = labelFormat.format(calendar.getTime());

            int totalXp = 0;
            int count = 0;

            if (tasks != null) {
                for (Task task : tasks) {
                    Map<String, String> occurrences = task.getOccurrenceStatuses();
                    if (occurrences != null && occurrences.containsKey(dateKey)) {
                        if (COMPLETED_STATUS.equals(occurrences.get(dateKey))) {
                            totalXp += task.getTotalXp();
                            count++;
                        }
                    } else {
                        String taskDate = sdf.format(new java.util.Date(task.getExecutionTime()));
                        if (taskDate.equals(dateKey) && task.getStatus() == TaskStatus.DONE) {
                            totalXp += task.getTotalXp();
                            count++;
                        }
                    }
                }
            }

            float avg = count > 0 ? (float) totalXp / count : 0f;
            avgXpValues.add(avg);
            labels.add(label);
        }

        chartData.put("values", avgXpValues);
        chartData.put("labels", labels);
        return chartData;
    }

    // ===================================================
    // LONGEST STREAK - iz taskova (ispravna implementacija)
    // Niz se prekida SAMO ako postoji dan sa FAILED taskom.
    // Dan bez ijednog zadatka NE prekida niz.
    // ===================================================

    public int calculateLongestStreakFromTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0;

        // Prikupi sve datume koji imaju barem jedan task (occurrence)
        // i označi koji datumi imaju FAILED task
        Set<String> datesWithFailed = new HashSet<>();
        Set<String> datesWithAnyTask = new TreeSet<>(); // TreeSet → sortirano

        for (Task task : tasks) {
            Map<String, String> occurrences = task.getOccurrenceStatuses();
            if (occurrences != null) {
                for (Map.Entry<String, String> entry : occurrences.entrySet()) {
                    String date = entry.getKey();
                    String status = entry.getValue();
                    datesWithAnyTask.add(date);
                    if (FAILED_STATUS.equals(status)) {
                        datesWithFailed.add(date);
                    }
                }
            } else {
                // Jednokratni task bez occurrence - koristi executionTime
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String date = sdf.format(new java.util.Date(task.getExecutionTime()));
                datesWithAnyTask.add(date);
                if (task.getStatus() == TaskStatus.FAILED) {
                    datesWithFailed.add(date);
                }
            }
        }

        if (datesWithAnyTask.isEmpty()) return 0;

        // Sortiraj datume
        List<String> sortedDates = new ArrayList<>(datesWithAnyTask);
        Collections.sort(sortedDates);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int longestStreak = 0;
        int currentStreak = 0;
        String previousDate = null;

        for (String date : sortedDates) {
            boolean hasFailed = datesWithFailed.contains(date);

            if (hasFailed) {
                // Dan sa FAILED taskom - prekida niz
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 0;
                previousDate = date;
                continue;
            }

            if (previousDate == null) {
                // Prvi dan
                currentStreak = 1;
            } else {
                // Provjeri da li je gap između datuma (dani bez taskova su OK)
                // Jedino što prekida niz je FAILED dan, ne prazan dan
                // Dakle uvijek nastavljamo streak osim ako je prethodni bio FAILED
                currentStreak++;
            }

            longestStreak = Math.max(longestStreak, currentStreak);
            previousDate = date;
        }

        return longestStreak;
    }

    // ===================================================
    // DIFFICULTY LABEL
    // ===================================================

    public String getAverageDifficultyLabel(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return "";

        int totalXp = 0;
        int count = 0;

        for (Task task : tasks) {
            Map<String, String> occurrences = task.getOccurrenceStatuses();
            if (occurrences != null && !occurrences.isEmpty()) {
                for (String status : occurrences.values()) {
                    if (COMPLETED_STATUS.equals(status)) {
                        totalXp += task.getTotalXp();
                        count++;
                    }
                }
            } else if (task.getStatus() == TaskStatus.DONE) {
                totalXp += task.getTotalXp();
                count++;
            }
        }

        if (count == 0) return "Nema završenih zadataka";

        float avg = (float) totalXp / count;

        if (avg <= 2) return "Korisnik uglavnom rešava veoma lake zadatke";
        else if (avg <= 6) return "Korisnik uglavnom rešava lake zadatke";
        else if (avg <= 13) return "Korisnik uglavnom rešava teške zadatke";
        else return "Korisnik uglavnom rešava ekstremno teške zadatke";
    }

    // ===================================================
    // FORMATIRANJE
    // ===================================================

    public String formatActiveDays(int activeDays) {
        return activeDays + " uzastopna dana";
    }

    public String formatLongestStreak(int longestStreak) {
        return longestStreak + " dana zaredom";
    }
}