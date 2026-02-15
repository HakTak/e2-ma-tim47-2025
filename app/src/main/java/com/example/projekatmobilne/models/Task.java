package com.example.projekatmobilne.models;

import com.example.projekatmobilne.enums.Difficulty;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.Importance;
import com.example.projekatmobilne.enums.RepeatUnit;

import java.util.HashMap;
import java.util.Map;

public class Task {

    private String id; // Firestore ID (String)
    private String userId; // DODATO - ko je kreirao task
    private String categoryId; // Firestore Category ID (String)

    private String title;
    private String description;

    private FrequencyType frequencyType;
    private Integer repeatInterval;
    private RepeatUnit repeatUnit;
    private Long repeatStartDate;
    private Long repeatEndDate;

    private long executionTime;

    private Difficulty difficulty;
    private Importance importance;
    private int totalXp;
    private boolean completed;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Task() {}

    public Task(String userId, String categoryId, String title, String description,
                FrequencyType frequencyType, Integer repeatInterval, RepeatUnit repeatUnit,
                Long repeatStartDate, Long repeatEndDate, long executionTime,
                Difficulty difficulty, Importance importance) {

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
        this.totalXp = difficulty.getXp() + importance.getXp();
        this.completed = false;
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
        map.put("difficulty", difficulty.name());
        map.put("importance", importance.name());
        map.put("totalXp", totalXp);
        map.put("completed", completed);
        return map;
    }

    // GETTERS & SETTERS (promeni long u String gde treba)
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

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}