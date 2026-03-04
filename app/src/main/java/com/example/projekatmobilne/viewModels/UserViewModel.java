package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.BossRepository;
import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.services.UserService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

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

    private final BossRepository bossRepository; // ← DODAJ
    private final SharedPrefsManager prefsManager; // ← DODAJ

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.userService = new UserService();

        this.bossRepository = new BossRepository(); // ← DODAJ
        this.prefsManager = new SharedPrefsManager(application); // ← DODAJ

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
        Log.d("USER_VIEWMODEL", "addXP() pozvan | userId: " + userId + " | xp: " + xpAmount);
        userService.addXP(userId, xpAmount, new UserService.XPCallback() {
            @Override
            public void onSuccess(boolean leveledUp, int newLevel, int ppGained) {
                Log.d("USER_VIEWMODEL", "addXP() pozvan | userId: " + userId + " | xp: " + xpAmount);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    android.widget.Toast.makeText(
                            getApplication(),
                            "🎉 Prešao si nivo " + newLevel + "! Novi boss je otključan!",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                });

                loadUser(userId);

                if (leveledUp) {
                    levelUpEvent.postValue(new LevelUpEvent(newLevel, ppGained));

                    // ← DODAJ — boss se kreira ovdje, centralno za sve slučajeve
                    Boss newBoss = new Boss(userId, newLevel);
                    bossRepository.getBossByLevel(userId, newLevel, new BossRepository.BossCallback() {
                        @Override
                        public void onBossLoaded(Boss boss) {
                            Log.d("USER_VIEWMODEL", "Boss za level " + newLevel + " već postoji");
                        }

                        @Override
                        public void onBossNotFound() {
                            bossRepository.insertBoss(newBoss, new BossRepository.InsertCallback() {
                                @Override
                                public void onSuccess(String bossId) {
                                    Log.d("USER_VIEWMODEL", "Boss kreiran za level " + newLevel);
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("USER_VIEWMODEL", "Greška boss: " + error);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("USER_VIEWMODEL", "Greška provjera boss: " + error);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e("USER_VIEWMODEL", "addXP greška: " + error);

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
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("coins", com.google.firebase.firestore.FieldValue.increment(coinsAmount))
                .addOnSuccessListener(aVoid -> {
                    Log.d("USER_VIEWMODEL", "Coins dodati: +" + coinsAmount);
                    loadUser(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("USER_VIEWMODEL", "Greška pri dodavanju coins: " + e.getMessage());
                    errorMessage.postValue(e.getMessage());
                });
    }
}