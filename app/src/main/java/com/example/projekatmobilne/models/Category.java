package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class Category {

    private String id; // Firestore ID (String umesto long)
    private String userId; // DODATO - ko je kreirao kategoriju
    private String name;
    private String colorHex;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Category() {}

    public Category(String userId, String name, String colorHex) {
        this.userId = userId;
        this.name = name;
        this.colorHex = colorHex;
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("name", name);
        map.put("colorHex", colorHex);
        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}