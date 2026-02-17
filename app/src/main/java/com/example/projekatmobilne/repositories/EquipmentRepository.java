package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.EquipmentType;
import com.example.projekatmobilne.models.Equipment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EquipmentRepository {

    private final FirestoreManager firestoreManager;

    public EquipmentRepository() {
        this.firestoreManager = new FirestoreManager();
    }

    // ===== DODAJ OPREMU =====
    public void addEquipment(Equipment equipment, FirestoreManager.FirestoreCallback callback) {
        String equipmentId = UUID.randomUUID().toString();
        equipment.setId(equipmentId);
        firestoreManager.addEquipment(
                equipment.getUserId(), equipmentId, equipment.toMap(), callback);
    }

    // ===== DOHVATI SVU OPREMU =====
    public void getEquipmentList(String userId, EquipmentListCallback callback) {
        firestoreManager.getEquipmentList(userId, new FirestoreManager.EquipmentListCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                List<Equipment> equipmentList = new ArrayList<>();
                for (DocumentSnapshot doc : documents) {
                    Equipment e = mapDocumentToEquipment(doc);
                    if (e != null && !e.isUsed()) {
                        equipmentList.add(e);
                    }
                }
                callback.onSuccess(equipmentList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===== AŽURIRAJ OPREMU =====
    public void updateEquipment(String userId, String equipmentId,
                                Map<String, Object> updates,
                                FirestoreManager.FirestoreCallback callback) {
        firestoreManager.updateEquipment(userId, equipmentId, updates, callback);
    }

    // ===== OBRIŠI OPREMU =====
    public void deleteEquipment(String userId, String equipmentId,
                                FirestoreManager.FirestoreCallback callback) {
        firestoreManager.deleteEquipment(userId, equipmentId, callback);
    }

    // ===== HELPER: MAP DOCUMENT TO EQUIPMENT =====
    private Equipment mapDocumentToEquipment(DocumentSnapshot doc) {
        try {
            Equipment e = new Equipment();
            e.setId(doc.getId());
            e.setUserId(doc.getString("userId"));

            String typeStr = doc.getString("type");
            String subtypeStr = doc.getString("subtype");
            if (typeStr != null) e.setType(EquipmentType.valueOf(typeStr));
            if (subtypeStr != null) e.setSubtype(EquipmentSubtype.valueOf(subtypeStr));

            Boolean active = doc.getBoolean("active");
            e.setActive(active != null && active);

            Long fightsRemaining = doc.getLong("fightsRemaining");
            e.setFightsRemaining(fightsRemaining != null ? fightsRemaining.intValue() : 0);

            Boolean used = doc.getBoolean("used");
            e.setUsed(used != null && used);

            Long ppBonus = doc.getLong("ppBonusApplied");
            e.setPpBonusApplied(ppBonus != null ? ppBonus.intValue() : 0);

            Double weaponBonus = doc.getDouble("weaponBonus");
            e.setWeaponBonus(weaponBonus != null ? weaponBonus : 0.0);

            Long purchasedAt = doc.getLong("purchasedAt");
            e.setPurchasedAt(purchasedAt != null ? purchasedAt : 0);

            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    // ===== CALLBACKS =====
    public interface EquipmentListCallback {
        void onSuccess(List<Equipment> equipmentList);
        void onError(String error);
    }
}