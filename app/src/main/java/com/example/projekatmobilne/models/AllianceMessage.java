package com.example.projekatmobilne.models;

import java.util.HashMap;
import java.util.Map;

public class AllianceMessage {
    private String id;
    private String allianceId;
    private String senderId;
    private String senderUsername;
    private String message;
    private long timestamp;

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public AllianceMessage() {}

    public AllianceMessage(String allianceId, String senderId, String senderUsername, String message) {
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("allianceId", allianceId);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("message", message);
        map.put("timestamp", timestamp);
        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}