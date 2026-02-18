package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class Friendship {
    private String id;
    private String user1Id;
    private String user2Id;
    private long createdAt;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Friendship() {}

    public Friendship(String user1Id, String user2Id) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.createdAt = System.currentTimeMillis();
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user1Id", user1Id);
        map.put("user2Id", user2Id);
        map.put("createdAt", createdAt);
        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}