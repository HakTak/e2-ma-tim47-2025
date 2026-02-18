package com.example.projekatmobilne.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Alliance {
    private String id;
    private String name;
    private String leaderId;
    private String leaderUsername; // Za lakši prikaz
    private List<String> memberIds;
    private long createdAt;
    private boolean hasMission; // Da li postoji aktivna misija

    // Prazan konstruktor (OBAVEZAN za Firestore)
    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.hasMission = false;
    }

    public Alliance(String name, String leaderId, String leaderUsername) {
        this.name = name;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(leaderId); // Vođa je automatski član
        this.createdAt = System.currentTimeMillis();
        this.hasMission = false;
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("leaderId", leaderId);
        map.put("leaderUsername", leaderUsername);
        map.put("memberIds", memberIds);
        map.put("createdAt", createdAt);
        map.put("hasMission", hasMission);
        return map;
    }

    // GETTERS & SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public String getLeaderUsername() { return leaderUsername; }
    public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isHasMission() { return hasMission; }
    public void setHasMission(boolean hasMission) { this.hasMission = hasMission; }
}