package com.example.projekatmobilne.database;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    private final FirebaseAuth auth;

    public FirebaseAuthManager() {
        this.auth = FirebaseAuth.getInstance();
    }

    // REGISTER
    public void register(String email, String password, RegisterCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Pošalji email verifikaciju
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            callback.onSuccess(user.getUid());
                                        } else {
                                            callback.onError("Email nije poslat: " + emailTask.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        callback.onError("Registracija neuspešna: " + task.getException().getMessage());
                    }
                });
    }

    // LOGIN
    public void login(String email, String password, LoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Proveri da li je email verifikovan
                            if (user.isEmailVerified()) {
                                callback.onSuccess(user.getUid());
                            } else {
                                callback.onError("Email nije verifikovan!");
                            }
                        }
                    } else {
                        callback.onError("Login neuspešan: " + task.getException().getMessage());
                    }
                });
    }

    // LOGOUT
    public void logout() {
        auth.signOut();
    }

    // GET CURRENT USER
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // CALLBACKS
    public interface RegisterCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    public interface LoginCallback {
        void onSuccess(String userId);
        void onError(String error);
    }
}