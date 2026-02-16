package com.example.projekatmobilne.services;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.UserRepository;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
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
 * - Promena lozinke
 * - Validacija korisničkih podataka
 */
public class UserService {

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;

    public UserService() {
        this.userRepository = new UserRepository();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    // ===== USER LOAD =====
    /**
     * Učitava korisnika iz baze
     */
    public void getUser(String userId, UserRepository.UserCallback callback) {
        userRepository.getUser(userId, callback);
    }

    // ===== ACTIVITY TRACKING =====
    /**
     * Provera dnevne aktivnosti korisnika.
     * Poziva se svaki put kad korisnik otvori aplikaciju.
     *
     * Business Rules:
     * - Ako je prvi put (lastActivityDate == 0) → aktivni dani = 1, streak = 1
     * - Ako je danas već evidentiran → ne radi ništa
     * - Ako je jučerašnji dan → nastavlja streak
     * - Ako je pauza > 1 dan → resetuje streak na 1
     */
    public void checkDailyActivity(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                long currentTime = System.currentTimeMillis();
                long lastActivityDate = user.getLastActivityDate();

                // Ako je prvi put
                if (lastActivityDate == 0) {
                    user.setLastActivityDate(currentTime);
                    user.setActiveDays(1);
                    user.setCurrentStreak(1);
                    user.setLongestStreak(1);
                    userRepository.updateActivityTracking(user, null);
                    return;
                }

                // Proveri da li je danas već evidentiran
                if (isSameDay(currentTime, lastActivityDate)) {
                    return; // Već je danas bio aktivan
                }

                // Proveri da li je jučerašnji dan
                if (isYesterday(lastActivityDate)) {
                    // Nastavi streak
                    user.setActiveDays(user.getActiveDays() + 1);
                    user.setCurrentStreak(user.getCurrentStreak() + 1);

                    // Ažuriraj najduži niz ako je potrebno
                    if (user.getCurrentStreak() > user.getLongestStreak()) {
                        user.setLongestStreak(user.getCurrentStreak());
                    }
                } else {
                    // Pauza je veća od 1 dana - resetuj streak
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
     * Dodaje XP korisniku i ažurira istoriju.
     *
     * Business Rules:
     * - Dodaje XP za današnji dan u xpHistory
     * - Automatski briše unose starije od 7 dana
     * - Ažurira ukupan XP korisnika
     */
    public void addXP(String userId, int xpAmount, FirestoreManager.FirestoreCallback callback) {
        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                String today = getTodayDateString();
                Map<String, Integer> xpHistory = user.getXpHistory();

                // Dodaj XP za današnji dan
                int currentXP = xpHistory.getOrDefault(today, 0);
                xpHistory.put(today, currentXP + xpAmount);

                // Obriši unose starije od 7 dana
                cleanOldXPHistory(xpHistory);

                // Ažuriraj ukupan XP
                user.setXp(user.getXp() + xpAmount);

                // Sačuvaj u bazu
                Map<String, Object> updates = new HashMap<>();
                updates.put("xpHistory", xpHistory);
                updates.put("xp", user.getXp());

                userRepository.updateUser(userId, updates, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    // ===== PASSWORD CHANGE =====
    /**
     * Menja lozinku korisnika.
     *
     * Business Rules:
     * - Validira staru lozinku (reauthentication)
     * - Validira novu lozinku (minimum 6 karaktera)
     * - Ažurira lozinku u Firebase Auth
     */
    public void changePassword(String oldPassword, String newPassword, PasswordCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            callback.onError("Korisnik nije ulogovan!");
            return;
        }

        // Validacija
        if (newPassword.length() < 6) {
            callback.onError("Nova lozinka mora imati minimum 6 karaktera!");
            return;
        }

        // Reauthenticate
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);

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

    // ===== HELPER METHODS (Private) =====

    private boolean isSameDay(long timestamp1, long timestamp2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date1 = sdf.format(new Date(timestamp1));
        String date2 = sdf.format(new Date(timestamp2));
        return date1.equals(date2);
    }

    private boolean isYesterday(long timestamp) {
        long yesterday = System.currentTimeMillis() - ONE_DAY_MILLIS;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String yesterdayStr = sdf.format(new Date(yesterday));
        String timestampStr = sdf.format(new Date(timestamp));
        return yesterdayStr.equals(timestampStr);
    }

    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void cleanOldXPHistory(Map<String, Integer> xpHistory) {
        String sevenDaysAgo = getDateStringDaysAgo(7);

        List<String> keysToRemove = new java.util.ArrayList<>();
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    // ===== CALLBACKS =====

    public interface PasswordCallback {
        void onSuccess();
        void onError(String error);
    }
}