package Model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import Enum.Difficulty;
import Enum.FrequencyType;
import Enum.Importance;
import Enum.RepeatUnit;

@Entity(tableName = "tasks",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE
        ))
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long categoryId;

    private String title;
    private String description;

    private FrequencyType frequencyType; //1x ili nx

    private Integer repeatInterval;     // npr 1, 2, 3
    private RepeatUnit repeatUnit;      // DAY / week /month/year
    private Long repeatStartDate;
    private Long repeatEndDate;

    private long executionTime; // timestamp

    private Difficulty difficulty;
    private Importance importance;

    private int totalXp;

    private boolean completed;

    public Task(long categoryId,
                String title,
                String description,
                FrequencyType frequencyType,
                Integer repeatInterval,
                RepeatUnit repeatUnit,
                Long repeatStartDate,
                Long repeatEndDate,
                long executionTime,
                Difficulty difficulty,
                Importance importance) {

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FrequencyType getFrequencyType() {
        return frequencyType;
    }

    public void setFrequencyType(FrequencyType frequencyType) {
        this.frequencyType = frequencyType;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public RepeatUnit getRepeatUnit() {
        return repeatUnit;
    }

    public void setRepeatUnit(RepeatUnit repeatUnit) {
        this.repeatUnit = repeatUnit;
    }

    public Long getRepeatStartDate() {
        return repeatStartDate;
    }

    public void setRepeatStartDate(Long repeatStartDate) {
        this.repeatStartDate = repeatStartDate;
    }

    public Long getRepeatEndDate() {
        return repeatEndDate;
    }

    public void setRepeatEndDate(Long repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}