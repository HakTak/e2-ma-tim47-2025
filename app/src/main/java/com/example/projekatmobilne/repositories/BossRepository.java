package com.example.projekatmobilne.repositories;

import android.util.Log;

import com.example.projekatmobilne.models.Boss;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BossRepository {

    private static final String TAG = "BOSS_REPO";
    private static final String COLLECTION = "bosses";

    private final FirebaseFirestore db;

    public BossRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ===================================================
    // CRUD OPERACIJE
    // ===================================================

    /**
     * Dohvata sve Boss-ove za korisnika
     */
    public void getBossesByUser(String userId, BossesCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Boss> bosses = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            bosses.add(parseDocument(doc));
                        } catch (Exception e) {
                            Log.e(TAG, "Greška pri parsiranju: " + e.getMessage());
                        }
                    }
                    callback.onBossesLoaded(bosses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getBossesByUser() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Dohvata Boss-a za određeni level
     */
    public void getBossByLevel(String userId, int level, BossCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("level", level)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        try {
                            Boss boss = parseDocument(querySnapshot.getDocuments().get(0));
                            callback.onBossLoaded(boss);
                        } catch (Exception e) {
                            Log.e(TAG, "Greška pri parsiranju: " + e.getMessage());
                            callback.onError(e.getMessage());
                        }
                    } else {
                        callback.onBossNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getBossByLevel() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Kreira novog Boss-a u Firestore-u
     */
    public void insertBoss(Boss boss, InsertCallback callback) {
        db.collection(COLLECTION)
                .add(boss.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Boss dodat sa ID: " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "insertBoss() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Ažurira Boss-a (npr. označava kao poraženog)
     */
    public void updateBoss(Boss boss, UpdateCallback callback) {
        db.collection(COLLECTION).document(boss.getId())
                .set(boss.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Boss ažuriran: level " + boss.getLevel());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateBoss() greška: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ===================================================
    // PARSIRANJE
    // ===================================================

    private Boss parseDocument(DocumentSnapshot doc) {
        Boss boss = new Boss();
        boss.setId(doc.getId());
        boss.setUserId(doc.getString("userId"));

        Long level = doc.getLong("level");
        boss.setLevel(level != null ? level.intValue() : 1);

        Long maxHp = doc.getLong("maxHp");
        boss.setMaxHp(maxHp != null ? maxHp.intValue() : 200);

        Long coins = doc.getLong("coins");
        boss.setCoins(coins != null ? coins.intValue() : 200);

        Boolean defeated = doc.getBoolean("defeated");
        boss.setDefeated(defeated != null && defeated);

        return boss;
    }

    // ===================================================
    // CALLBACK INTERFEJSI
    // ===================================================

    public interface BossesCallback {
        void onBossesLoaded(List<Boss> bosses);
        void onError(String error);
    }

    public interface BossCallback {
        void onBossLoaded(Boss boss);
        void onBossNotFound();
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(String bossId);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }
}