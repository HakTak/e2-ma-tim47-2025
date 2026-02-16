package com.example.projekatmobilne.services;

import com.example.projekatmobilne.database.FirebaseAuthManager;
import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * AuthService - Business Logic Layer za autentifikaciju
 *
 * Odgovornosti:
 * - Registracija korisnika (validacija + kreiranje u Auth + Firestore)
 * - Login korisnika (validacija + provera email verifikacije + sesija)
 * - Logout korisnika
 * - Validacija input podataka
 */
public class AuthService {

    private final FirebaseAuthManager authManager;
    private final FirestoreManager firestoreManager;
    private final SharedPrefsManager prefsManager;

    public AuthService(SharedPrefsManager prefsManager) {
        this.authManager = new FirebaseAuthManager();
        this.firestoreManager = new FirestoreManager();
        this.prefsManager = prefsManager;
    }

    // ===== REGISTER =====
    /**
     * Registruje novog korisnika.
     *
     * Business Rules:
     * - Validira email, password, username
     * - Kreira Firebase Auth nalog
     * - Šalje email verifikaciju
     * - Kreira User dokument u Firestore
     */
    public void register(String email, String password, String username, String avatar, AuthCallback callback) {
        // Validacija
        String validationError = validateRegistrationInput(email, password, username);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        // Firebase Auth registracija
        authManager.register(email, password, new FirebaseAuthManager.RegisterCallback() {
            @Override
            public void onSuccess(String userId) {
                // Kreiraj User objekat
                User newUser = new User(userId, username, email, avatar);

                // Spremi u Firestore
                firestoreManager.createUser(userId, newUser.toMap(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess("registration_success");
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===== LOGIN =====
    /**
     * Loguje korisnika.
     *
     * Business Rules:
     * - Validira email i password
     * - Proverava email verifikaciju
     * - Učitava User podatke iz Firestore
     * - Sprema sesiju u SharedPreferences
     */
    public void login(String email, String password, AuthCallback callback) {
        // Validacija
        if (email.isEmpty() || password.isEmpty()) {
            callback.onError("Popuni sva polja!");
            return;
        }

        // Firebase Auth login
        authManager.login(email, password, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onSuccess(String userId) {
                // Učitaj user podatke iz Firestore
                firestoreManager.getUser(userId, new FirestoreManager.UserCallback() {
                    @Override
                    public void onSuccess(DocumentSnapshot snapshot) {
                        String username = snapshot.getString("username");
                        String avatar = snapshot.getString("avatar");
                        int level = snapshot.getLong("level").intValue();

                        // Spremi session
                        prefsManager.saveSession(userId, username, avatar, level);
                        callback.onSuccess("login_success");
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===== LOGOUT =====
    /**
     * Odjavljuje korisnika.
     */
    public void logout() {
        authManager.logout();
        prefsManager.clearSession();
    }

    // ===== VALIDATION (Private Helper) =====

    private String validateRegistrationInput(String email, String password, String username) {
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            return "Popuni sva polja!";
        }
        if (password.length() < 6) {
            return "Lozinka mora imati minimum 6 karaktera!";
        }
        if (username.length() < 3) {
            return "Korisničko ime mora imati minimum 3 karaktera!";
        }
        return null; // Nema greške
    }

    // ===== CALLBACK =====

    public interface AuthCallback {
        void onSuccess(String status);
        void onError(String error);
    }
}