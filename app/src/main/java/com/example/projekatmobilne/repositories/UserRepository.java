package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserRepository - Data Access Layer
 *
 * Odgovornosti:
 * - CRUD operacije nad User entitetom u Firestore-u
 * - Konverzija DocumentSnapshot → User objekat
 * - NEMA poslovne logike (to je u UserService)
 */
public class UserRepository {

    private final FirestoreManager firestoreManager;

    public UserRepository() {
        this.firestoreManager = new FirestoreManager();
    }

    // ===== CREATE USER =====
    public void createUser(User user, FirestoreManager.FirestoreCallback callback) {
        firestoreManager.createUser(user.getId(), user.toMap(), callback);
    }

    // ===== GET USER =====
    public void getUser(String userId, UserCallback callback) {
        firestoreManager.getUser(userId, new FirestoreManager.UserCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                User user = mapDocumentToUser(snapshot);
                callback.onUserLoaded(user);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===== UPDATE USER (generic update) =====
    public void updateUser(String userId, Map<String, Object> updates, FirestoreManager.FirestoreCallback callback) {
        firestoreManager.updateUser(userId, updates, callback);
    }

    // ===== UPDATE ACTIVITY TRACKING =====
    public void updateActivityTracking(User user, FirestoreManager.FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("activeDays", user.getActiveDays());
        updates.put("currentStreak", user.getCurrentStreak());
        updates.put("longestStreak", user.getLongestStreak());
        updates.put("lastActivityDate", user.getLastActivityDate());

        firestoreManager.updateUser(user.getId(), updates, callback != null ? callback : new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                // Silent success
            }

            @Override
            public void onError(String error) {
                System.err.println("Error updating activity tracking: " + error);
            }
        });
    }

    // ===== HELPER: MAP DOCUMENT TO USER =====
    private User mapDocumentToUser(DocumentSnapshot snapshot) {
        User user = new User();
        user.setId(snapshot.getString("id"));
        user.setUsername(snapshot.getString("username"));
        user.setEmail(snapshot.getString("email"));
        user.setAvatar(snapshot.getString("avatar"));

        Long level = snapshot.getLong("level");
        user.setLevel(level != null ? level.intValue() : 0);

        user.setTitle(snapshot.getString("title"));

        Long pp = snapshot.getLong("pp");
        user.setPp(pp != null ? pp.intValue() : 0);

        Long xp = snapshot.getLong("xp");
        user.setXp(xp != null ? xp.intValue() : 0);

        Long coins = snapshot.getLong("coins");
        user.setCoins(coins != null ? coins.intValue() : 0);

        // Statistika
        Long activeDays = snapshot.getLong("activeDays");
        user.setActiveDays(activeDays != null ? activeDays.intValue() : 0);

        Long tasksCreated = snapshot.getLong("tasksCreated");
        user.setTasksCreated(tasksCreated != null ? tasksCreated.intValue() : 0);

        Long tasksCompleted = snapshot.getLong("tasksCompleted");
        user.setTasksCompleted(tasksCompleted != null ? tasksCompleted.intValue() : 0);

        Long tasksCancelled = snapshot.getLong("tasksCancelled");
        user.setTasksCancelled(tasksCancelled != null ? tasksCancelled.intValue() : 0);

        Long longestStreak = snapshot.getLong("longestStreak");
        user.setLongestStreak(longestStreak != null ? longestStreak.intValue() : 0);

        Long currentStreak = snapshot.getLong("currentStreak");
        user.setCurrentStreak(currentStreak != null ? currentStreak.intValue() : 0);

        Long lastActivityDate = snapshot.getLong("lastActivityDate");
        user.setLastActivityDate(lastActivityDate != null ? lastActivityDate : 0);

        // XP History
        Object xpHistoryObj = snapshot.get("xpHistory");
        if (xpHistoryObj instanceof Map) {
            Map<String, Object> rawMap = (Map<String, Object>) xpHistoryObj;
            Map<String, Integer> xpHistory = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    xpHistory.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                }
            }
            user.setXpHistory(xpHistory);
        } else {
            user.setXpHistory(new HashMap<>());
        }

        // Badges i Equipment
        user.setBadges((List<String>) snapshot.get("badges"));
        user.setEquipment((List<String>) snapshot.get("equipment"));

        return user;
    }

    // ===== CALLBACK =====
    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(String error);
    }
}