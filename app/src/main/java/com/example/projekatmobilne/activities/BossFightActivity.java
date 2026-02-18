package com.example.projekatmobilne.activities;

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
        prefsManager      = new SharedPrefsManager(this);
        bossViewModel     = new ViewModelProvider(this).get(BossViewModel.class);
        userViewModel     = new ViewModelProvider(this).get(UserViewModel.class);
        equipmentViewModel= new ViewModelProvider(this).get(EquipmentViewModel.class);
        equipmentService  = new EquipmentService();
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
                // Filtriramo samo aktivnu opremu
                activeEquipment.clear();
                for (Equipment e : equipmentList) {
                    if (e.isActive()) activeEquipment.add(e);
                }
                equipmentLoaded = true;
                Log.d(TAG, "Oprema učitana | Aktivna: " + activeEquipment.size());
                tryStartFight();
            }
        });

        // 4. Učitaj zadatke za šansu pogotka
        loadHitChance(userId);
    }

    /**
     * Kreće borbu tek kada su korisnik, boss i oprema svi učitani.
     */
    private void tryStartFight() {
        if (userLoaded && bossLoaded && equipmentLoaded) {
            setupBossFight();
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

    private void loadHitChance(String userId) {
        TaskRepository taskRepository = new TaskRepository();
        taskRepository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                double baseChance = calculateBaseHitChance(tasks);
                // Shield dodaje bonus na šansu
                hitChance = equipmentService.calculateAttackChance(
                        baseChance * 100, activeEquipment) / 100.0;
                tvHitChance.setText("Šansa pogotka: " + (int)(hitChance * 100) + "%");
                Log.d(TAG, "Šansa pogotka (sa opremom): " + (int)(hitChance * 100) + "%");
            }

            @Override
            public void onError(String error) {
                hitChance = 0.5;
                tvHitChance.setText("Šansa pogotka: 50% (default)");
                Log.e(TAG, "Greška pri učitavanju zadataka: " + error);
            }
        });
    }

    private double calculateBaseHitChance(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0.5;

        int done = 0, failed = 0, cancelled = 0;

        for (Task task : tasks) {
            if (task.getFrequencyType() == FrequencyType.ONE_TIME) {
                TaskStatus s = task.getStatus();
                if (s == TaskStatus.DONE)           done++;
                else if (s == TaskStatus.FAILED)    failed++;
                else if (s == TaskStatus.CANCELLED) cancelled++;
            } else {
                if (task.getOccurrenceStatuses() != null) {
                    for (String statusStr : task.getOccurrenceStatuses().values()) {
                        try {
                            TaskStatus s = TaskStatus.valueOf(statusStr);
                            if (s == TaskStatus.DONE)           done++;
                            else if (s == TaskStatus.FAILED)    failed++;
                            else if (s == TaskStatus.CANCELLED) cancelled++;
                        } catch (Exception e) {
                            Log.e(TAG, "Greška parsiranja statusa: " + statusStr);
                        }
                    }
                }
            }
        }

        int total = done + failed + cancelled;
        if (total == 0) return 0.5;

        double chance = (double) done / total;
        Log.d(TAG, "Uspešnost zadataka: " + done + "/" + total
                + " = " + (int)(chance * 100) + "%");
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
        // Coins sa luk multiplikatorom
        double coinMultiplier = equipmentService.calculateCoinMultiplier(activeEquipment);
        int coinsEarned = (int)(currentBoss.getCoins() * coinMultiplier);

        addToBattleLog("🏆 POBEDA! Boss je poražen!");
        addToBattleLog("💰 Osvajate: " + coinsEarned + " coins"
                + (coinMultiplier > 1.0 ? " (luk bonus!)" : ""));

        String userId = prefsManager.getUserId();

        // 1. Dodeli coins korisniku
        userViewModel.addCoins(userId, coinsEarned);

        // 2. Označi bossa kao poraženog
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

        // 3. Šansa za opremu (20%) — koristi EquipmentService.grantBossLootEquipment
        equipmentService.grantBossLootEquipment(currentUser, activeEquipment,
                new EquipmentService.LootCallback() {
                    @Override
                    public void onLoot(EquipmentSubtype received, boolean wasUpgrade) {
                        String lootMsg = wasUpgrade
                                ? "🗡 Oružje unapređeno: " + received.name()
                                : "🎁 Nova oprema: " + received.name();
                        addToBattleLog(lootMsg);
                        Log.d(TAG, "Loot dobijen: " + received.name());
                        finalizeFight(userId);
                    }
                    @Override
                    public void onNoLoot() {
                        addToBattleLog("🎲 Nisi dobio opremu ovog puta.");
                        finalizeFight(userId);
                    }
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Greška pri loot-u: " + error);
                        finalizeFight(userId);
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

            // Polovina šanse za opremu (10%)
            if (new Random().nextInt(100) < 10) {
                equipmentService.grantBossLootEquipment(currentUser, activeEquipment,
                        new EquipmentService.LootCallback() {
                            @Override
                            public void onLoot(EquipmentSubtype received, boolean wasUpgrade) {
                                addToBattleLog("🎁 Ipak si dobio opremu: " + received.name());
                                finalizeFight(userId);
                            }
                            @Override
                            public void onNoLoot() { finalizeFight(userId); }
                            @Override
                            public void onError(String error) { finalizeFight(userId); }
                        });
            } else {
                finalizeFight(userId);
            }
        } else {
            addToBattleLog("💀 PORAZ! Nisi uspio umanjiti 50% HP bossa.");
            addToBattleLog("💰 Osvojeno: 0 coins");
            finalizeFight(userId);
        }
    }

    /**
     * Poziva se na kraju svake borbe — ažurira opremu (trajanje, potrošeni napici itd.)
     */
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
}