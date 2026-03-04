package com.example.projekatmobilne;

import android.app.Application;
import android.util.Log;

import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

import java.util.HashMap;
import java.util.Map;

public class HabitRPGApplication extends Application {

    // ZAMENI SA TVOJIM APP ID-em iz OneSignal dashboard-a
    private static final String ONESIGNAL_APP_ID = "6ba28894-d610-4818-ad77-48326ce71cfd";

    @Override
    public void onCreate() {
        super.onCreate();

        // Verbose logging za debug
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // OneSignal inicijalizacija
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        OneSignal.getNotifications().requestPermission(true, com.onesignal.Continue.with(r -> {
            if (r.isSuccess()) {
                Log.d("OneSignal", "Dozvola odobrena");
            }
        }));
        // ===== OBRISANO: requestPermission() - uzrokuje crash =====
        // OneSignal će automatski tražiti dozvolu kad mu treba

        // Listener za Player ID
        OneSignal.getUser().addObserver(state -> {
            String playerId = state.getCurrent().getOnesignalId();

            if (playerId != null && !playerId.isEmpty()) {
                Log.d("OneSignal", "Player ID: " + playerId);
                savePlayerIdToFirestore(playerId);
            }
        });
    }

    private void savePlayerIdToFirestore(String playerId) {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String userId = prefsManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            Log.w("OneSignal", "User nije ulogovan, ne mogu sačuvati Player ID");
            return;
        }

        UserRepository userRepository = new UserRepository();

        Map<String, Object> updates = new HashMap<>();
        updates.put("oneSignalPlayerId", playerId);

        userRepository.updateUser(userId, updates,
                new com.example.projekatmobilne.database.FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("OneSignal", "Player ID sačuvan u Firestore za userId: " + userId);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("OneSignal", "Greška pri čuvanju Player ID: " + error);
                    }
                });
    }
}