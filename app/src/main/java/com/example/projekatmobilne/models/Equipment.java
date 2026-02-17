package com.example.projekatmobilne.models;

import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.EquipmentType;

import java.util.HashMap;
import java.util.Map;

public class Equipment {

    private String id;
    private String userId;
    private EquipmentType type;
    private EquipmentSubtype subtype;
    private boolean active;
    private int fightsRemaining;  // samo za CLOTHING (0, 1, 2)
    private boolean used;         // samo za jednokratne napitke
    private int ppBonusApplied;   // koliko PP smo dodali (za oduzimanje kad istekne)
    private double weaponBonus;   // trenutni bonus oružja (raste unapređenjem)
    private long purchasedAt;

    // Prazan konstruktor (obavezan za Firestore)
    public Equipment() {}

    public Equipment(String userId, EquipmentType type, EquipmentSubtype subtype) {
        this.userId = userId;
        this.type = type;
        this.subtype = subtype;
        this.active = false;
        this.fightsRemaining = (type == EquipmentType.CLOTHING) ? 2 : 0;
        this.used = false;
        this.ppBonusApplied = 0;
        this.weaponBonus = getBaseWeaponBonus(subtype);
        this.purchasedAt = System.currentTimeMillis();
    }

    // Bazni bonus oružja
    private double getBaseWeaponBonus(EquipmentSubtype subtype) {
        if (subtype == EquipmentSubtype.SWORD) return 0.05;
        if (subtype == EquipmentSubtype.BOW) return 0.05;
        return 0.0;
    }

    // Konverzija u Map (za Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("type", type != null ? type.name() : null);
        map.put("subtype", subtype != null ? subtype.name() : null);
        map.put("active", active);
        map.put("fightsRemaining", fightsRemaining);
        map.put("used", used);
        map.put("ppBonusApplied", ppBonusApplied);
        map.put("weaponBonus", weaponBonus);
        map.put("purchasedAt", purchasedAt);
        return map;
    }

    // ===== GETTERS & SETTERS =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }

    public EquipmentSubtype getSubtype() { return subtype; }
    public void setSubtype(EquipmentSubtype subtype) { this.subtype = subtype; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getFightsRemaining() { return fightsRemaining; }
    public void setFightsRemaining(int fightsRemaining) { this.fightsRemaining = fightsRemaining; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public int getPpBonusApplied() { return ppBonusApplied; }
    public void setPpBonusApplied(int ppBonusApplied) { this.ppBonusApplied = ppBonusApplied; }

    public double getWeaponBonus() { return weaponBonus; }
    public void setWeaponBonus(double weaponBonus) { this.weaponBonus = weaponBonus; }

    public long getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(long purchasedAt) { this.purchasedAt = purchasedAt; }

    // ===== HELPER METODE =====

    public boolean isPermanent() {
        return subtype == EquipmentSubtype.POTION_PERM_5
                || subtype == EquipmentSubtype.POTION_PERM_10
                || type == EquipmentType.WEAPON;
    }

    public boolean isSingleUse() {
        return subtype == EquipmentSubtype.POTION_20
                || subtype == EquipmentSubtype.POTION_40;
    }

    public String getDisplayName() {
        switch (subtype) {
            case POTION_20:     return "Napitak snage +20%";
            case POTION_40:     return "Napitak snage +40%";
            case POTION_PERM_5: return "Napitak trajne snage +5%";
            case POTION_PERM_10:return "Napitak trajne snage +10%";
            case GLOVES:        return "Rukavice";
            case SHIELD:        return "Štit";
            case BOOTS:         return "Čizme";
            case SWORD:         return "Mač";
            case BOW:           return "Luk i strela";
            default:            return "Nepoznata oprema";
        }
    }
}