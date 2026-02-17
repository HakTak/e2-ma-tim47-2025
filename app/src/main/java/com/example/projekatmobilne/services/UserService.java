package com.example.projekatmobilne.services;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.UserRepository;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UserService - Business Logic Layer za upravljanje korisnicima
 *
 * Odgovornosti:
 * - Aktivnost tracking (dnevna aktivnost, streak-ovi)
 * - XP management (dodavanje XP, čišćenje istorije)
 * - Level-up logika (automatski prelaz na sledeći nivo)
 * - Promena lozinke
 */
public class UserService {

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;
    private final LevelService levelService;

    public UserService() {
        this.userRepository = new UserRepository();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.levelService = new LevelService();
    }

    // ===== USER LOAD =====
    public void getUser(String userId, UserRepository.UserCallback callback) {
        userRepository.getUser(userId, callback);
    }

    // ===== ACTIVITY TRACKING =====
    /**
     * Provera dnevne aktivnosti korisnika.
     * Poziva se svaki put kad korisnik otvori aplikaciju.
     */
    public void checkDailyActivity(String userId) {
        if (userId == null || userId.isEmpty()) return;

        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                long currentTime = System.currentTimeMillis();
                long lastActivityDate = user.getLastActivityDate();

                if (lastActivityDate == 0) {
                    user.setLastActivityDate(currentTime);
                    user.setActiveDays(1);
                    user.setCurrentStreak(1);
                    user.setLongestStreak(1);
                    userRepository.updateActivityTracking(user, null);
                    return;
                }

                if (isSameDay(currentTime, lastActivityDate)) return;

                if (isYesterday(lastActivityDate)) {
                    user.setActiveDays(user.getActiveDays() + 1);
                    user.setCurrentStreak(user.getCurrentStreak() + 1);
                    if (user.getCurrentStreak() > user.getLongestStreak()) {
                        user.setLongestStreak(user.getCurrentStreak());
                    }
                } else {
                    user.setActiveDays(user.getActiveDays() + 1);
                    user.setCurrentStreak(1);
                }

                user.setLastActivityDate(currentTime);
                userRepository.updateActivityTracking(user, null);
            }

            @Override
            public void onError(String error) {
                System.err.println("ActivityTracker error: " + error);
            }
        });
    }

    // ===== XP MANAGEMENT =====
    /**
     * Dodaje XP korisniku, ažurira istoriju i proverava level-up.
     *
     * @param userId    ID korisnika
     * @param xpAmount  Količina XP koja se dodaje
     * @param callback  Callback sa rezultatom (uključuje level-up info)
     */
    public void addXP(String userId, int xpAmount, XPCallback callback) {
        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                String today = getTodayDateString();
                Map<String, Integer> xpHistory = user.getXpHistory();

                // Dodaj XP za današnji dan u istoriju
                int currentDayXP = xpHistory.getOrDefault(today, 0);
                xpHistory.put(today, currentDayXP + xpAmount);

                // Obriši unose starije od 7 dana
                cleanOldXPHistory(xpHistory);

                // Ažuriraj ukupan XP
                int newXP = user.getXp() + xpAmount;
                user.setXp(newXP);
                user.setXpHistory(xpHistory);

                // Proveri level-up
                checkAndApplyLevelUp(user, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    // ===== LEVEL UP LOGIKA =====
    /**
     * Proverava da li korisnik treba da pređe na sledeći nivo.
     * Ako da, ažurira level, PP, titulu i čuva u bazu.
     */
    private void checkAndApplyLevelUp(User user, XPCallback callback) {
        boolean didLevelUp = false;
        int newLevel = user.getLevel();
        int totalPPGained = 0;

        // Proveravamo u petlji jer korisnik može preskočiti više nivoa odjednom
        while (levelService.shouldLevelUp(user.getXp(), newLevel)) {
            newLevel++;
            int ppReward = levelService.calculatePPReward(newLevel);
            totalPPGained += ppReward;
            didLevelUp = true;
        }

        if (didLevelUp) {
            // Ažuriraj User objekat
            user.setLevel(newLevel);
            user.setPp(user.getPp() + totalPPGained);
            user.setTitle(levelService.getTitleForLevel(newLevel));

            // Sačuvaj SVE promene u Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("xp", user.getXp());
            updates.put("xpHistory", user.getXpHistory());
            updates.put("level", user.getLevel());
            updates.put("pp", user.getPp());
            updates.put("title", user.getTitle());

            int finalNewLevel = newLevel;
            int finalPPGained = totalPPGained;

            userRepository.updateUser(user.getId(), updates, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess(true, finalNewLevel, finalPPGained);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError(error);
                }
            });
        } else {
            // Nema level-up, sačuvaj samo XP
            Map<String, Object> updates = new HashMap<>();
            updates.put("xp", user.getXp());
            updates.put("xpHistory", user.getXpHistory());

            userRepository.updateUser(user.getId(), updates, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess(false, user.getLevel(), 0);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) callback.onError(error);
                }
            });
        }
    }

    // ===== PASSWORD CHANGE =====
    public void changePassword(String oldPassword, String newPassword, PasswordCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            callback.onError("Korisnik nije ulogovan!");
            return;
        }

        if (newPassword.length() < 6) {
            callback.onError("Nova lozinka mora imati minimum 6 karaktera!");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), oldPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        callback.onSuccess();
                                    } else {
                                        callback.onError("Greška pri promeni lozinke!");
                                    }
                                });
                    } else {
                        callback.onError("Stara lozinka nije tačna!");
                    }
                });
    }

    // ===== HELPER METODE =====

    private boolean isSameDay(long timestamp1, long timestamp2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp1)).equals(sdf.format(new Date(timestamp2)));
    }

    private boolean isYesterday(long timestamp) {
        long yesterday = System.currentTimeMillis() - ONE_DAY_MILLIS;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(yesterday)).equals(sdf.format(new Date(timestamp)));
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void cleanOldXPHistory(Map<String, Integer> xpHistory) {
        String sevenDaysAgo = getDateStringDaysAgo(7);
        List<String> keysToRemove = new ArrayList<>();
        for (String dateKey : xpHistory.keySet()) {
            if (dateKey.compareTo(sevenDaysAgo) < 0) {
                keysToRemove.add(dateKey);
            }
        }
        for (String key : keysToRemove) {
            xpHistory.remove(key);
        }
    }

    private String getDateStringDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.getTime());
    }

    // ===== CALLBACKS =====

    /**
     * Callback za dodavanje XP sa informacijom o level-up-u
     */
    public interface XPCallback {
        void onSuccess(boolean leveledUp, int newLevel, int ppGained);
        void onError(String error);
    }

    public interface PasswordCallback {
        void onSuccess();
        void onError(String error);
    }
}