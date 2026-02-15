package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.database.FirebaseAuthManager;
import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.utils.SharedPrefsManager;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuthManager authManager;
    private final FirestoreManager firestoreManager;
    private final SharedPrefsManager prefsManager;

    public MutableLiveData<String> authStatus = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authManager = new FirebaseAuthManager();
        this.firestoreManager = new FirestoreManager();
        this.prefsManager = new SharedPrefsManager(application);
    }

    // REGISTER
    public void register(String email, String password, String username, String avatar) {
        authManager.register(email, password, new FirebaseAuthManager.RegisterCallback() {
            @Override
            public void onSuccess(String userId) {
                // Kreiraj User objekat
                User newUser = new User(userId, username, email, avatar);

                // Spremi u Firestore
                firestoreManager.createUser(userId, newUser.toMap(), new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        authStatus.postValue("registration_success");
                    }

                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // LOGIN
    public void login(String email, String password) {
        authManager.login(email, password, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onSuccess(String userId) {
                // Učitaj user podatke iz Firestore
                firestoreManager.getUser(userId, new FirestoreManager.UserCallback() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentSnapshot snapshot) {
                        String username = snapshot.getString("username");
                        String avatar = snapshot.getString("avatar");
                        int level = snapshot.getLong("level").intValue();

                        // Spremi session
                        prefsManager.saveSession(userId, username, avatar, level);
                        authStatus.postValue("login_success");
                    }

                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // LOGOUT
    public void logout() {
        authManager.logout();
        prefsManager.clearSession();
        authStatus.postValue("logged_out");
    }
}