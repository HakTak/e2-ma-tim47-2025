package com.example.projekatmobilne.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String id;
    private String username;
    private String email;
    private String avatar;
    private int level;
    private String title;
    private int pp;
    private int xp;
    private int coins;

    // Statistika
    private int activeDays; // Broj dana aktivnosti
    private int tasksCreated; // Ukupno kreiranih taskova
    private int tasksCompleted; // Ukupno završenih taskova
    private int tasksCancelled; // Ukupno otkazanih
    private int longestStreak; // Najduži niz
    private int currentStreak; // Trenutni niz
    private long lastActivityDate; // Zadnji dan aktivnosti (timestamp u milisekundama)

    // ===== NOVO: XP History (poslednjih 7 dana) =====
    private Map<String, Integer> xpHistory; // key: "yyyy-MM-dd", value: XP za taj dan

    private List<String> badges;
    private List<String> equipment;
    private long createdAt;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public User() {
        this.badges = new ArrayList<>();
        this.equipment = new ArrayList<>();
        this.xpHistory = new HashMap<>(); // NOVO
        this.activeDays = 0;
        this.tasksCreated = 0;
        this.tasksCompleted = 0;
        this.tasksCancelled = 0;
        this.longestStreak = 0;
        this.currentStreak = 0;
        this.lastActivityDate = 0;
    }

    // Konstruktor za kreiranje novog usera
    public User(String id, String username, String email, String avatar) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.level = 0;
        this.title = "Početnik";
        this.pp = 0;
        this.xp = 0;
        this.coins = 0;
        this.badges = new ArrayList<>();
        this.equipment = new ArrayList<>();
        this.xpHistory = new HashMap<>(); // NOVO
        this.createdAt = System.currentTimeMillis();

        // Statistika
        this.activeDays = 0;
        this.tasksCreated = 0;
        this.tasksCompleted = 0;
        this.tasksCancelled = 0;
        this.longestStreak = 0;
        this.currentStreak = 0;
        this.lastActivityDate = 0;
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("email", email);
        map.put("avatar", avatar);
        map.put("level", level);
        map.put("title", title);
        map.put("pp", pp);
        map.put("xp", xp);
        map.put("coins", coins);
        map.put("badges", badges);
        map.put("equipment", equipment);
        map.put("createdAt", createdAt);
        map.put("activeDays", activeDays);
        map.put("tasksCreated", tasksCreated);
        map.put("tasksCompleted", tasksCompleted);
        map.put("tasksCancelled", tasksCancelled);
        map.put("longestStreak", longestStreak);
        map.put("currentStreak", currentStreak);
        map.put("lastActivityDate", lastActivityDate);
        map.put("xpHistory", xpHistory); // NOVO

        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPp() { return pp; }
    public void setPp(int pp) { this.pp = pp; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }

    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Statistika getters & setters
    public int getActiveDays() { return activeDays; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }

    public int getTasksCreated() { return tasksCreated; }
    public void setTasksCreated(int tasksCreated) { this.tasksCreated = tasksCreated; }

    public int getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public int getTasksCancelled() { return tasksCancelled; }
    public void setTasksCancelled(int tasksCancelled) { this.tasksCancelled = tasksCancelled; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public long getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(long lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    // ===== NOVO: XP History =====
    public Map<String, Integer> getXpHistory() {
        return xpHistory != null ? xpHistory : new HashMap<>();
    }
    public void setXpHistory(Map<String, Integer> xpHistory) {
        this.xpHistory = xpHistory;
    }
}