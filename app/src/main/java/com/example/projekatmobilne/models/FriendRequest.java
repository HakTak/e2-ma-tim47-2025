package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class FriendRequest {
    private String id;
    private String fromUserId;
    private String fromUsername; // Za lakši prikaz
    private String toUserId;
    private String status; // "pending", "accepted", "declined"
    private long timestamp;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public FriendRequest() {}

    public FriendRequest(String fromUserId, String fromUsername, String toUserId) {
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fromUserId", fromUserId);
        map.put("fromUsername", fromUsername);
        map.put("toUserId", toUserId);
        map.put("status", status);
        map.put("timestamp", timestamp);
        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}