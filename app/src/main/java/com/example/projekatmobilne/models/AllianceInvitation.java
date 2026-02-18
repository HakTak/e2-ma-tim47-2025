package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class AllianceInvitation {
    private String id;
    private String allianceId;
    private String allianceName; // Za lakši prikaz
    private String fromUserId;
    private String fromUsername; // Ko je poslao poziv (vođa)
    private String toUserId;
    private String status; // "pending", "accepted", "declined"
    private long timestamp;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public AllianceInvitation() {}

    public AllianceInvitation(String allianceId, String allianceName, String fromUserId,
                              String fromUsername, String toUserId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("allianceId", allianceId);
        map.put("allianceName", allianceName);
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

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getAllianceName() { return allianceName; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }

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