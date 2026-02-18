package com.example.projekatmobilne.viewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.services.UserService;

/**
 * UserViewModel - Presentation Logic Layer
 *
 * Odgovornosti:
 * - Izlaže podatke UI sloju
 * - Poziva UserService za poslovnu logiku
 * - Upravlja LiveData objektima
 */
public class UserViewModel extends AndroidViewModel {

    private final UserService userService;

    public MutableLiveData<User> userData = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // ===== NOVO: LiveData za level-up notifikaciju =====
    public MutableLiveData<LevelUpEvent> levelUpEvent = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.userService = new UserService();
    }

    // LOAD USER
    public void loadUser(String userId) {
        userService.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                userData.postValue(user);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== NOVO: ADD XP sa level-up podrškom =====
    public void addXP(String userId, int xpAmount) {
        userService.addXP(userId, xpAmount, new UserService.XPCallback() {
            @Override
            public void onSuccess(boolean leveledUp, int newLevel, int ppGained) {
                // Osvježi korisnika
                loadUser(userId);

                // Ako je bio level-up, objavi event
                if (leveledUp) {
                    levelUpEvent.postValue(new LevelUpEvent(newLevel, ppGained));
                }
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== KLASA ZA LEVEL-UP EVENT =====
    public static class LevelUpEvent {
        public final int newLevel;
        public final int ppGained;

        public LevelUpEvent(int newLevel, int ppGained) {
            this.newLevel = newLevel;
            this.ppGained = ppGained;
        }
    }

    // ===== DODAJ COINS =====
    public void addCoins(String userId, int coinsAmount) {
        userService.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                int newCoins = user.getCoins() + coinsAmount;
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("coins", newCoins);

                new com.example.projekatmobilne.repositories.UserRepository()
                        .updateUser(userId, updates,
                                new com.example.projekatmobilne.database.FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        loadUser(userId);
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
}