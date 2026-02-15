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

    public void updateUser(String userId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
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
}