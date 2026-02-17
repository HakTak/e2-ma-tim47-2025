package com.example.projekatmobilne.models;

import android.util.Log;

import com.example.projekatmobilne.enums.*;
import java.io.Serializable;
import java.util.*;

public class Task implements Serializable {

    private String id;
    private String userId;
    private String categoryId;
    private String title;
    private String description;
    private FrequencyType frequencyType;
    private Integer repeatInterval;
    private RepeatUnit repeatUnit;
    private Long repeatStartDate;
    private Long repeatEndDate;
    private long executionTime;
    private List<Long> recurringDates = new ArrayList<>();
    private Difficulty difficulty;
    private Importance importance;
    private TaskStatus status; // FALLBACK status
    private int totalXp;

    // >>> NOVO POLJE <
    private Map<String, String> occurrenceStatuses = new HashMap<>();

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Task() {}

    public Task(String userId, String categoryId, String title, String description,
                FrequencyType frequencyType, Integer repeatInterval, RepeatUnit repeatUnit,
                Long repeatStartDate, Long repeatEndDate, long executionTime,
                Difficulty difficulty, Importance importance, List<Long> recurringDates) {

        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.frequencyType = frequencyType;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.repeatStartDate = repeatStartDate;
        this.repeatEndDate = repeatEndDate;
        this.executionTime = executionTime;
        this.difficulty = difficulty;
        this.importance = importance;
        this.recurringDates = recurringDates;
        this.status = TaskStatus.ACTIVE;
        this.totalXp = difficulty.getXp() + importance.getXp();
        this.occurrenceStatuses = new HashMap<>(); // Inicijalizuj praznu mapu
    }

    // >>> NOVA METODA: Dobij status za specifičan datum <
    public TaskStatus getStatusForDate(long timestamp) {
        String dateKey = timestampToDateKey(timestamp);

        // Ako postoji specifičan status za taj datum, vrati ga
        if (occurrenceStatuses.containsKey(dateKey)) {
            return TaskStatus.valueOf(occurrenceStatuses.get(dateKey));
        }

        // Inače vrati globalni status
        return status != null ? status : TaskStatus.ACTIVE;
    }

    // >>> NOVA METODA: Postavi status za specifičan datum <
    public void setStatusForDate(long timestamp, TaskStatus newStatus) {
        String dateKey = timestampToDateKey(timestamp);
        occurrenceStatuses.put(dateKey, newStatus.name());

        Log.d("TASK_MODEL", "setStatusForDate() pozvan:");
        Log.d("TASK_MODEL", "  - Timestamp: " + timestamp);
        Log.d("TASK_MODEL", "  - Date Key: " + dateKey);
        Log.d("TASK_MODEL", "  - New Status: " + newStatus);
        Log.d("TASK_MODEL", "  - Mapa posle izmene: " + occurrenceStatuses.toString());
    }

    // >>> POMOĆNA METODA: Konvertuj timestamp u String ključ (YYYY-MM-DD) <
    private String timestampToDateKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("categoryId", categoryId);
        map.put("title", title);
        map.put("description", description);
        map.put("frequencyType", frequencyType.name());
        map.put("repeatInterval", repeatInterval);
        map.put("repeatUnit", repeatUnit != null ? repeatUnit.name() : null);
        map.put("repeatStartDate", repeatStartDate);
        map.put("repeatEndDate", repeatEndDate);
        map.put("executionTime", executionTime);
        map.put("recurringDates", recurringDates);
        map.put("difficulty", difficulty.name());
        map.put("importance", importance.name());
        map.put("totalXp", totalXp);
        map.put("status", status.name());
        map.put("occurrenceStatuses", occurrenceStatuses); // >>> DODAJ MAPU <
        return map;
    }

    // Getteri i Setteri
    public Map<String, String> getOccurrenceStatuses() {
        return occurrenceStatuses;
    }

    public void setOccurrenceStatuses(Map<String, String> occurrenceStatuses) {
        this.occurrenceStatuses = occurrenceStatuses != null ? occurrenceStatuses : new HashMap<>();
    }

    // ... ostali getteri i setteri ostaju isti ...

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FrequencyType getFrequencyType() { return frequencyType; }
    public void setFrequencyType(FrequencyType frequencyType) { this.frequencyType = frequencyType; }
    public Integer getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(Integer repeatInterval) { this.repeatInterval = repeatInterval; }
    public RepeatUnit getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(RepeatUnit repeatUnit) { this.repeatUnit = repeatUnit; }
    public Long getRepeatStartDate() { return repeatStartDate; }
    public void setRepeatStartDate(Long repeatStartDate) { this.repeatStartDate = repeatStartDate; }
    public Long getRepeatEndDate() { return repeatEndDate; }
    public void setRepeatEndDate(Long repeatEndDate) { this.repeatEndDate = repeatEndDate; }
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public Importance getImportance() { return importance; }
    public void setImportance(Importance importance) { this.importance = importance; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public List<Long> getRecurringDates() { return recurringDates; }
    public void setRecurringDates(List<Long> recurringDates) { this.recurringDates = recurringDates; }

    public Long getNextOccurrence(long currentTime) {
        if (recurringDates == null || recurringDates.isEmpty()) {
            return null;
        }
        for (Long date : recurringDates) {
            if (date >= currentTime) {
                return date;
            }
        }
        return null;
    }
}