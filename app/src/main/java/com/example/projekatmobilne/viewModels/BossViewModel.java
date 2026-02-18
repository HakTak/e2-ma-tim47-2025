package com.example.projekatmobilne.viewModels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.repositories.BossRepository;
import com.example.projekatmobilne.utils.SharedPrefsManager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BossViewModel extends AndroidViewModel {

    private static final String TAG = "BOSS_VIEWMODEL";

    private final BossRepository repository;
    private final SharedPrefsManager prefsManager;

    private final MutableLiveData<List<Boss>> bossesLiveData = new MutableLiveData<>();
    // Boss koji čeka na borbu (najstariji neporaženi)
    private final MutableLiveData<Boss> pendingBossLiveData = new MutableLiveData<>();

    public BossViewModel(@NonNull Application application) {
        super(application);
        this.repository = new BossRepository();
        this.prefsManager = new SharedPrefsManager(application);
    }

    // ===================================================
    // LOAD
    // ===================================================

    public MutableLiveData<List<Boss>> getAllBosses() {
        return bossesLiveData;
    }

    public MutableLiveData<Boss> getPendingBoss() {
        return pendingBossLiveData;
    }

    public void loadAllBosses() {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "UserId je NULL!");
            return;
        }

        repository.getBossesByUser(userId, new BossRepository.BossesCallback() {
            @Override
            public void onBossesLoaded(List<Boss> bosses) {
                bossesLiveData.postValue(bosses);
                resolvePendingBoss(bosses);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "loadAllBosses() greška: " + error);
            }
        });
    }

    /**
     * Pronalazi najstarijeg neporaženog bossa (najmanji level).
     * Ako nema neporaženih, pendingBossLiveData dobija null.
     */
    private void resolvePendingBoss(List<Boss> bosses) {
        List<Boss> undefeated = bosses.stream()
                .filter(b -> !b.isDefeated())
                .sorted(Comparator.comparingInt(Boss::getLevel))
                .collect(Collectors.toList());

        if (!undefeated.isEmpty()) {
            Boss next = undefeated.get(0);
            Log.d(TAG, "Sledeći boss za borbu: level " + next.getLevel());
            pendingBossLiveData.postValue(next);
        } else {
            Log.d(TAG, "Nema neporaženih bossova");
            pendingBossLiveData.postValue(null);
        }
    }

    // ===================================================
    // KREIRANJE BOSSA ZA NOVI LEVEL
    // ===================================================

    /**
     * Poziva se kada korisnik pređe novi level.
     * Kreira Boss zapis u Firestore-u ako već ne postoji.
     */
    public void ensureBossExistsForLevel(int level, BossReadyCallback callback) {
        String userId = prefsManager.getUserId();
        if (userId == null) return;

        repository.getBossByLevel(userId, level, new BossRepository.BossCallback() {
            @Override
            public void onBossLoaded(Boss boss) {
                Log.d(TAG, "Boss za level " + level + " već postoji");
                callback.onReady(boss);
            }

            @Override
            public void onBossNotFound() {
                // Boss ne postoji — kreiraj ga
                Boss newBoss = new Boss(userId, level);
                repository.insertBoss(newBoss, new BossRepository.InsertCallback() {
                    @Override
                    public void onSuccess(String bossId) {
                        newBoss.setId(bossId);
                        Log.d(TAG, "Boss kreiran za level " + level + " HP: " + newBoss.getMaxHp());
                        callback.onReady(newBoss);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Greška pri kreiranju bossa: " + error);
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "ensureBossExistsForLevel() greška: " + error);
                callback.onError(error);
            }
        });
    }

    // ===================================================
    // OZNAČAVANJE BOSSA KAO PORAŽENOG
    // ===================================================

    public void markBossDefeated(Boss boss, UpdateDoneCallback callback) {
        boss.setDefeated(true);
        repository.updateBoss(boss, new BossRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Boss level " + boss.getLevel() + " označen kao poražen");
                loadAllBosses();
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "markBossDefeated() greška: " + error);
                callback.onError(error);
            }
        });
    }

    // ===================================================
    // CALLBACK INTERFEJSI
    // ===================================================

    public interface BossReadyCallback {
        void onReady(Boss boss);
        void onError(String error);
    }

    public interface UpdateDoneCallback {
        void onSuccess();
        void onError(String error);
    }
}