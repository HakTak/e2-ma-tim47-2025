package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserRepository {

    private final FirestoreManager firestoreManager;

    public UserRepository() {
        this.firestoreManager = new FirestoreManager();
    }

    // CREATE USER
    public void createUser(User user, FirestoreManager.FirestoreCallback callback) {
        firestoreManager.createUser(user.getId(), user.toMap(), callback);
    }

    // GET USER
    public void getUser(String userId, UserCallback callback) {
        firestoreManager.getUser(userId, new FirestoreManager.UserCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                User user = new User();
                user.setId(snapshot.getString("id"));
                user.setUsername(snapshot.getString("username"));
                user.setEmail(snapshot.getString("email"));
                user.setAvatar(snapshot.getString("avatar"));
                user.setLevel(snapshot.getLong("level").intValue());
                user.setTitle(snapshot.getString("title"));
                user.setPp(snapshot.getLong("pp").intValue());
                user.setXp(snapshot.getLong("xp").intValue());
                user.setCoins(snapshot.getLong("coins").intValue());
                callback.onUserLoaded(user);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // CALLBACK
    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(String error);
    }
}