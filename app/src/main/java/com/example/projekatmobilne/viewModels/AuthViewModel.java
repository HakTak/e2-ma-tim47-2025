package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.services.AuthService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

/**
 * AuthViewModel - Presentation Logic Layer za autentifikaciju
 *
 * Odgovornosti:
 * - Izlaže podatke UI sloju (Fragment/Activity)
 * - Poziva AuthService za poslovnu logiku
 * - Upravlja LiveData objektima
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthService authService;
    public MutableLiveData<String> authStatus = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        SharedPrefsManager prefsManager = new SharedPrefsManager(application);
        this.authService = new AuthService(prefsManager);
    }

    // REGISTER
    public void register(String email, String password, String username, String avatar) {
        authService.register(email, password, username, avatar, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String status) {
                authStatus.postValue(status);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // LOGIN
    public void login(String email, String password) {
        authService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String status) {
                authStatus.postValue(status);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // LOGOUT
    public void logout() {
        authService.logout();
        authStatus.postValue("logged_out");
    }
}