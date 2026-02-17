package com.example.projekatmobilne.database;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FirestoreManager {

    private final FirebaseFirestore db;

    public FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ============ USERS ============
    public void createUser(String userId, Map<String, Object> userData, FirestoreCallback callback) {
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUser(String userId, UserCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ CATEGORIES (DODATO za tvoj Task sistem) ============
    public void createCategory(String userId, Map<String, Object> categoryData, FirestoreCallback callback) {
        db.collection("categories")
                .add(categoryData)
                .addOnSuccessListener(docRef -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ CALLBACKS ============
    public interface FirestoreCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UserCallback {
        void onSuccess(DocumentSnapshot user);
        void onError(String error);
    }
    // Dodaj ovu metodu u FirestoreManager.java

    /**
     * Ažurira specifična polja korisnika u Firestore-u
     */
    public void updateUser(String userId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    public void addEquipment(String userId, String equipmentId,
                             Map<String, Object> data, FirestoreCallback callback) {
        db.collection("users").document(userId)
                .collection("equipment").document(equipmentId)
                .set(data)
                .addOnSuccessListener(a -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getEquipmentList(String userId, EquipmentListCallback callback) {
        db.collection("users").document(userId)
                .collection("equipment")
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onSuccess(querySnapshot.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateEquipment(String userId, String equipmentId,
                                Map<String, Object> updates, FirestoreCallback callback) {
        db.collection("users").document(userId)
                .collection("equipment").document(equipmentId)
                .update(updates)
                .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    public void deleteEquipment(String userId, String equipmentId, FirestoreCallback callback) {
        db.collection("users").document(userId)
                .collection("equipment").document(equipmentId)
                .delete()
                .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    public interface EquipmentListCallback {
        void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents);
        void onError(String error);
    }
}