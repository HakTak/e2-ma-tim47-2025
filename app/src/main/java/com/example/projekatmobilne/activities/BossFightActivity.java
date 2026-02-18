package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.services.EquipmentService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.EquipmentViewModel;
import com.example.projekatmobilne.viewModels.UserViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BossFightActivity extends AppCompatActivity {

    private static final String TAG = "BOSS_FIGHT_ACTIVITY";
    private static final int BASE_MAX_ATTACKS = 5;

    // Views
    private TextView tvBossName, tvBossHp, tvPlayerPP, tvHitChance;
    private TextView tvAttacksLeft, tvActiveEquipment, tvPotentialRewards, tvBattleLog;
    private ProgressBar progressBossHp;
    private Button btnAttack;

    // State
    private Boss currentBoss;
    private User currentUser;
    private List<Equipment> activeEquipment = new ArrayList<>();
    private int currentBossHp;
    private int maxAttacks = BASE_MAX_ATTACKS;
    private int attacksLeft;
    private int playerPP;
    private double hitChance = 0.5;
    private boolean fightFinished = false;
    private boolean hitChanceLoaded = false; // NOVO
    private StringBuilder battleLog = new StringBuilder();

    // ViewModels i servisi
    private BossViewModel bossViewModel;
    private UserViewModel userViewModel;
    private EquipmentViewModel equipmentViewModel;
    private EquipmentService equipmentService;
    private SharedPrefsManager prefsManager;

    // Zastavice za učitavanje — borba kreće tek kad su SVE tri učitane
    private boolean userLoaded      = false;
    private boolean bossLoaded      = false;
    private boolean equipmentLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        initViews();
        initServices();
        loadData();
    }

    // ===================================================
    // INICIJALIZACIJA
    // ===================================================

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Borba sa Bosom");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvBossName         = findViewById(R.id.tvBossName);
        tvBossHp           = findViewById(R.id.tvBossHp);
        tvPlayerPP         = findViewById(R.id.tvPlayerPP);
        tvHitChance        = findViewById(R.id.tvHitChance);
        tvAttacksLeft      = findViewById(R.id.tvAttacksLeft);
        tvActiveEquipment  = findViewById(R.id.tvActiveEquipment);
        tvPotentialRewards = findViewById(R.id.tvPotentialRewards);
        tvBattleLog        = findViewById(R.id.tvBattleLog);
        progressBossHp     = findViewById(R.id.progressBossHp);
        btnAttack          = findViewById(R.id.btnAttack);

        btnAttack.setEnabled(false);
        btnAttack.setOnClickListener(v -> performAttack());
    }

    private void initServices() {
        prefsManager       = new SharedPrefsManager(this);
        bossViewModel      = new ViewModelProvider(this).get(BossViewModel.class);
        userViewModel      = new ViewModelProvider(this).get(UserViewModel.class);
        equipmentViewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);
        equipmentService   = new EquipmentService();
    }

    // ===================================================
    // UČITAVANJE PODATAKA
    // ===================================================

    private void loadData() {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Greška: korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1. Učitaj korisnika
        userViewModel.loadUser(userId);
        userViewModel.userData.observe(this, user -> {
            if (user != null && !userLoaded) {
                currentUser = user;
                playerPP    = user.getPp();
                userLoaded  = true;
                Log.d(TAG, "Korisnik učitan | PP: " + playerPP);
                tryStartFight();
            }
        });

        // 2. Učitaj bossove
        bossViewModel.loadAllBosses();
        bossViewModel.getPendingBoss().observe(this, boss -> {
            if (boss != null && !bossLoaded) {
                currentBoss   = boss;
                currentBossHp = boss.getMaxHp();
                bossLoaded    = true;
                Log.d(TAG, "Boss učitan | Level: " + boss.getLevel() + " HP: " + boss.getMaxHp());
                tryStartFight();
            } else if (boss == null && !bossLoaded) {
                Toast.makeText(this, "Nema bossova za borbu!", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // 3. Učitaj aktivnu opremu
        equipmentViewModel.loadEquipment(userId);
        equipmentViewModel.equipmentList.observe(this, equipmentList -> {
            if (equipmentList != null && !equipmentLoaded) {
                activeEquipment.clear();
                for (Equipment e : equipmentList) {
                    if (e.isActive()) activeEquipment.add(e);
                }
                equipmentLoaded = true;
                Log.d(TAG, "Oprema učitana | Aktivna: " + activeEquipment.size());
                tryStartFight();
            }
        });

        // NAPOMENA: loadHitChance se NE poziva ovde — poziva se iz tryStartFight
        // tek kada je currentUser sigurno učitan
    }

    /**
     * Kreće loadHitChance tek kada su korisnik, boss i oprema svi učitani.
     * setupBossFight se poziva iz loadHitChance kada i šansa bude spremna.
     */
    private void tryStartFight() {
        if (userLoaded && bossLoaded && equipmentLoaded && !hitChanceLoaded) {
            Log.d(TAG, "Svi podaci učitani — računam šansu pogotka...");
            loadHitChance(prefsManager.getUserId());
        }
    }

    // ===================================================
    // SETUP BORBE
    // ===================================================

    private void setupBossFight() {
        // Računaj broj napada — čizme mogu dati +1
        maxAttacks  = equipmentService.calculateTotalAttacks(activeEquipment);
        attacksLeft = maxAttacks;

        // Prikaz aktivne opreme (emoji lista)
        updateEquipmentDisplay();

        tvBossName.setText("Boss — Nivo " + currentBoss.getLevel());
        updateHpDisplay();
        updateAttacksDisplay();
        updateRewardsDisplay();
        btnAttack.setEnabled(true);

        addToBattleLog("Borba počinje! Boss ima " + currentBoss.getMaxHp() + " HP.");
        if (maxAttacks > BASE_MAX_ATTACKS) {
            addToBattleLog("🥾 Čizme su aktivne! Imaš " + maxAttacks + " napada.");
        }
    }

    // ===================================================
    // ŠANSA POGOTKA
    // ===================================================

    /**
     * Učitava zadatke i računa šansu pogotka.
     * Poziva se SAMO iz tryStartFight — kada je currentUser sigurno učitan.
     * Na kraju poziva setupBossFight.
     */
    private void loadHitChance(String userId) {
        TaskRepository taskRepository = new TaskRepository();
        taskRepository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                double baseChance = calculateBaseHitChance(tasks);
                // Shield dodaje bonus na šansu
                hitChance = equipmentService.calculateAttackChance(
                        baseChance * 100, activeEquipment) / 100.0;
                Log.d(TAG, "Šansa pogotka konačna (sa opremom): " + (int)(hitChance * 100) + "%");
                tvHitChance.setText("Šansa pogotka: " + (int)(hitChance * 100) + "%");
                hitChanceLoaded = true;
                setupBossFight();
            }

            @Override
            public void onError(String error) {
                hitChance = 0.5;
                tvHitChance.setText("Šansa pogotka: 50% (default)");
                Log.e(TAG, "Greška pri učitavanju zadataka — default 50%: " + error);
                hitChanceLoaded = true;
                setupBossFight();
            }
        });
    }

    private double calculateBaseHitChance(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            Log.d(TAG, "Nema zadataka — default šansa 50%");
            return 0.5;
        }

        if (currentUser == null) {
            Log.d(TAG, "Korisnik nije učitan — default šansa 50%");
            return 0.5;
        }

        // Odredi vremenski opseg etape
        List<Long> timestamps = currentUser.getLevelUpTimestamps();
        int currentLevel = currentUser.getLevel();

        long etapaStart;
        long etapaEnd = System.currentTimeMillis();

        if (timestamps == null || timestamps.isEmpty()) {
            etapaStart = 0;
            Log.d(TAG, "Nema level-up timestamps — koristim sve zadatke");
        } else if (currentLevel <= 1 || timestamps.size() < 2) {
            etapaStart = 0;
            etapaEnd   = timestamps.get(timestamps.size() - 1);
            Log.d(TAG, "Prva etapa: od početka do " + etapaEnd);
        } else {
            etapaStart = timestamps.get(timestamps.size() - 2);
            etapaEnd   = timestamps.get(timestamps.size() - 1);
            Log.d(TAG, "Etapa: " + etapaStart + " → " + etapaEnd);
        }

        int done = 0, failed = 0, total = 0;

        for (Task task : tasks) {
            if (task.getOccurrenceStatuses() == null || task.getOccurrenceStatuses().isEmpty()) continue;

            for (Map.Entry<String, String> entry : task.getOccurrenceStatuses().entrySet()) {
                // Konvertuj datum ključ (npr. "2026-02-18") u timestamp
                long dateTimestamp;
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    dateTimestamp = sdf.parse(entry.getKey()).getTime();
                } catch (Exception e) {
                    Log.e(TAG, "Greška parsiranja datuma: " + entry.getKey());
                    continue;
                }

                // Provjeri da li datum pada u etapu
                if (dateTimestamp < etapaStart || dateTimestamp > etapaEnd) continue;

                try {
                    TaskStatus s = TaskStatus.valueOf(entry.getValue());
                    Log.d(TAG, "Task: " + task.getTitle() + " | datum: " + entry.getKey() + " | status: " + s);
                    if (s == TaskStatus.DONE) {
                        done++;
                        total++;
                    } else if (s == TaskStatus.FAILED) {
                        failed++;
                        total++;
                    }
                    // ACTIVE, PAUSED, CANCELLED, UPCOMING se ignorišu
                } catch (Exception e) {
                    Log.e(TAG, "Greška parsiranja statusa: " + entry.getValue());
                }
            }
        }

        if (total == 0) {
            Log.d(TAG, "Nema zadataka u etapi — default šansa 50%");
            return 0.5;
        }

        double chance = (double) done / total;
        Log.d(TAG, "=== ŠANSA POGOTKA ===");
        Log.d(TAG, "Etapa: " + etapaStart + " → " + etapaEnd);
        Log.d(TAG, "Urađeno: " + done + " | Neuspešno: " + failed + " | Ukupno: " + total);
        Log.d(TAG, "Šansa: " + done + "/" + total + " = " + (int)(chance * 100) + "%");

        return chance;
    }

    // ===================================================
    // LOGIKA NAPADA
    // ===================================================

    private void performAttack() {
        if (attacksLeft <= 0 || fightFinished) return;

        attacksLeft--;
        boolean hit = new Random().nextDouble() < hitChance;

        if (hit) {
            currentBossHp -= playerPP;
            if (currentBossHp < 0) currentBossHp = 0;
            addToBattleLog("⚔ Pogodak! Naneo si " + playerPP
                    + " štete. Boss HP: " + currentBossHp);
        } else {
            addToBattleLog("💨 Promašaj! Boss se izmaknuo.");
        }

        updateHpDisplay();
        updateAttacksDisplay();

        if (currentBossHp <= 0) {
            endFight(true);
        } else if (attacksLeft <= 0) {
            endFight(false);
        }
    }

    // ===================================================
    // KRAJ BORBE
    // ===================================================

    private void endFight(boolean bossDefeated) {
        fightFinished = true;
        btnAttack.setEnabled(false);

        if (bossDefeated) {
            handleVictory();
        } else {
            boolean halfDefeated = currentBossHp <= (currentBoss.getMaxHp() / 2);
            handleDefeat(halfDefeated);
        }
    }

    private void handleVictory() {
        double coinMultiplier = equipmentService.calculateCoinMultiplier(activeEquipment);
        int coinsEarned = (int)(currentBoss.getCoins() * coinMultiplier);

        addToBattleLog("🏆 POBEDA! Boss je poražen!");
        addToBattleLog("💰 Osvajate: " + coinsEarned + " coins"
                + (coinMultiplier > 1.0 ? " (luk bonus!)" : ""));

        String userId = prefsManager.getUserId();

        userViewModel.addCoins(userId, coinsEarned);

        bossViewModel.markBossDefeated(currentBoss, new BossViewModel.UpdateDoneCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Boss označen kao poražen");
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Greška markBossDefeated: " + error);
            }
        });

        equipmentService.grantBossLootEquipment(currentUser, activeEquipment,
                new EquipmentService.LootCallback() {
                    @Override
                    public void onLoot(EquipmentSubtype received, boolean wasUpgrade) {
                        Log.d(TAG, "Loot dobijen: " + received.name());
                        finalizeFight(userId);
                        launchRewardScreen(coinsEarned, true, getEquipmentEmoji(received));
                    }
                    @Override
                    public void onNoLoot() {
                        finalizeFight(userId);
                        launchRewardScreen(coinsEarned, false, null);
                    }
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Greška pri loot-u: " + error);
                        finalizeFight(userId);
                        launchRewardScreen(coinsEarned, false, null);
                    }
                });
    }

    private void handleDefeat(boolean halfDefeated) {
        String userId = prefsManager.getUserId();

        if (halfDefeated) {
            int halfCoins = (int)(currentBoss.getCoins() / 2.0
                    * equipmentService.calculateCoinMultiplier(activeEquipment));
            addToBattleLog("⚠ Boss nije poražen, ali si umanjio 50% HP!");
            addToBattleLog("💰 Osvojeno: " + halfCoins + " coins (polovično)");
            userViewModel.addCoins(userId, halfCoins);

            if (new Random().nextInt(100) < 10) {
                equipmentService.grantBossLootEquipment(currentUser, activeEquipment,
                        new EquipmentService.LootCallback() {
                            @Override
                            public void onLoot(EquipmentSubtype received, boolean wasUpgrade) {
                                finalizeFight(userId);
                                launchRewardScreen(halfCoins, true, getEquipmentEmoji(received));
                            }
                            @Override
                            public void onNoLoot() {
                                finalizeFight(userId);
                                launchRewardScreen(halfCoins, false, null);
                            }
                            @Override
                            public void onError(String error) {
                                finalizeFight(userId);
                                launchRewardScreen(halfCoins, false, null);
                            }
                        });
            } else {
                finalizeFight(userId);
                launchRewardScreen(halfCoins, false, null);
            }
        } else {
            addToBattleLog("💀 PORAZ! Nisi uspio umanjiti 50% HP bossa.");
            addToBattleLog("💰 Osvojeno: 0 coins");
            finalizeFight(userId);
            // Kod poraza nema kovčega — samo finish
            new android.os.Handler().postDelayed(this::finish, 2000);
        }
    }

    private void launchRewardScreen(int coins, boolean hasEquipment, String equipmentEmoji) {
        Intent intent = BossRewardActivity.createIntent(
                this, coins, hasEquipment, equipmentEmoji);
        startActivity(intent);
        finish();
    }

    private void finalizeFight(String userId) {
        equipmentViewModel.onBossFightFinished(currentUser);
        Log.d(TAG, "Borba završena, oprema ažurirana");
    }

    // ===================================================
    // UI HELPERS
    // ===================================================

    private void updateHpDisplay() {
        tvBossHp.setText(currentBossHp + " / " + currentBoss.getMaxHp() + " HP");
        int progress = (int)((double) currentBossHp / currentBoss.getMaxHp() * 100);
        progressBossHp.setProgress(progress);
    }

    private void updateAttacksDisplay() {
        tvAttacksLeft.setText("Preostali napadi: " + attacksLeft + " / " + maxAttacks);
        tvPlayerPP.setText("⚔ Tvoja snaga (PP): " + playerPP);
    }

    private void updateRewardsDisplay() {
        double multiplier = equipmentService.calculateCoinMultiplier(activeEquipment);
        int displayCoins  = (int)(currentBoss.getCoins() * multiplier);
        tvPotentialRewards.setText("💰 " + displayCoins + " coins  |  20% šansa za opremu");
    }

    private void updateEquipmentDisplay() {
        if (activeEquipment.isEmpty()) {
            tvActiveEquipment.setText("Nema aktivne opreme");
            return;
        }

        StringBuilder emojis = new StringBuilder();
        for (Equipment e : activeEquipment) {
            switch (e.getSubtype()) {
                case POTION_20:      emojis.append("🧪 "); break;
                case POTION_40:      emojis.append("⚗️ "); break;
                case POTION_PERM_5:  emojis.append("💧 "); break;
                case POTION_PERM_10: emojis.append("💦 "); break;
                case GLOVES:         emojis.append("🧤 "); break;
                case SHIELD:         emojis.append("🛡️ "); break;
                case BOOTS:          emojis.append("👢 "); break;
                case SWORD:          emojis.append("⚔️ "); break;
                case BOW:            emojis.append("🏹 "); break;
            }
        }
        tvActiveEquipment.setText(emojis.toString().trim());
    }

    private void addToBattleLog(String message) {
        battleLog.insert(0, message + "\n");
        tvBattleLog.setText(battleLog.toString());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getEquipmentEmoji(EquipmentSubtype subtype) {
        switch (subtype) {
            case POTION_20:      return "🧪";
            case POTION_40:      return "⚗️";
            case POTION_PERM_5:  return "💧";
            case POTION_PERM_10: return "💦";
            case GLOVES:         return "🧤";
            case SHIELD:         return "🛡️";
            case BOOTS:          return "👢";
            case SWORD:          return "⚔️";
            case BOW:            return "🏹";
            default:             return "🎁";
        }
    }
}