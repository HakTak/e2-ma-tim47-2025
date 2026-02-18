package com.example.projekatmobilne.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.UserViewModel;

import java.util.List;
import java.util.Random;

public class BossFightActivity extends AppCompatActivity {

    private static final String TAG = "BOSS_FIGHT_ACTIVITY";
    private static final int MAX_ATTACKS = 5;

    // Views
    private TextView tvBossName, tvBossHp, tvPlayerPP, tvHitChance;
    private TextView tvAttacksLeft, tvActiveEquipment, tvPotentialRewards, tvBattleLog;
    private ProgressBar progressBossHp;
    private Button btnAttack;

    // State
    private Boss currentBoss;
    private int currentBossHp;
    private int attacksLeft = MAX_ATTACKS;
    private int playerPP;
    private double hitChance;
    private StringBuilder battleLog = new StringBuilder();

    // Services
    private BossViewModel bossViewModel;
    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;

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
        // Koristi postojeći ActionBar umesto Toolbar-a
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
        prefsManager  = new SharedPrefsManager(this);
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    private void loadData() {
        String userId = prefsManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Greška: korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Učitaj korisnika (PP + oprema)
        userViewModel.loadUser(userId);
        userViewModel.userData.observe(this, user -> {
            if (user != null) {
                playerPP = user.getPp();
                updateEquipmentDisplay(user);
                Log.d(TAG, "Korisnik učitan | PP: " + playerPP);
            }
        });

        // Učitaj bossove i pronađi sledećeg
        bossViewModel.loadAllBosses();
        bossViewModel.getPendingBoss().observe(this, boss -> {
            if (boss != null && currentBoss == null) {
                currentBoss   = boss;
                currentBossHp = boss.getMaxHp();
                setupBossFight();
            } else if (boss == null) {
                Toast.makeText(this, "Nema bossova za borbu!", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // Učitaj zadatke za računanje šanse pogotka
        loadHitChance(userId);
    }

    // ===================================================
    // SETUP BORBE
    // ===================================================

    private void setupBossFight() {
        tvBossName.setText("Boss — Nivo " + currentBoss.getLevel());
        updateHpDisplay();
        updateAttacksDisplay();
        updateRewardsDisplay();
        btnAttack.setEnabled(true);
        addToBattleLog("Borba počinje! Boss ima " + currentBoss.getMaxHp() + " HP.");
        Log.d(TAG, "Borba postavljena | Boss level: " + currentBoss.getLevel()
                + " | HP: " + currentBossHp);
    }

    // ===================================================
    // RAČUNANJE ŠANSE POGOTKA IZ ZADATAKA
    // ===================================================

    private void loadHitChance(String userId) {
        TaskRepository taskRepository = new TaskRepository();
        taskRepository.getAllTasks(userId, new TaskRepository.TasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                hitChance = calculateHitChance(tasks);
                tvHitChance.setText("Šansa pogotka: " + (int)(hitChance * 100) + "%");
                Log.d(TAG, "Šansa pogotka: " + (int)(hitChance * 100) + "%");
            }

            @Override
            public void onError(String error) {
                // Ako ne možemo učitati zadatke, koristimo 50% kao default
                hitChance = 0.5;
                tvHitChance.setText("Šansa pogotka: 50% (default)");
                Log.e(TAG, "Greška pri učitavanju zadataka: " + error);
            }
        });
    }

    /**
     * Računa šansu pogotka na osnovu uspešnosti zadataka u poslednjoj etapi.
     * Etapa = svi zadaci kreirani između prethodnog i trenutnog levela korisnika.
     * Uspešnost = DONE / (DONE + FAILED + CANCELLED) — bez PAUSED i UPCOMING
     */
    private double calculateHitChance(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0.5;

        int done      = 0;
        int failed    = 0;
        int cancelled = 0;

        for (Task task : tasks) {
            // ONE_TIME taskovi — gledamo direktan status
            if (task.getFrequencyType() ==
                    com.example.projekatmobilne.enums.FrequencyType.ONE_TIME) {
                TaskStatus s = task.getStatus();
                if (s == TaskStatus.DONE)      done++;
                else if (s == TaskStatus.FAILED)    failed++;
                else if (s == TaskStatus.CANCELLED) cancelled++;

            } else {
                // RECURRING taskovi — gledamo svaki occurrence posebno
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
        Log.d(TAG, "Uspešnost: " + done + "/" + total + " = " + (int)(chance * 100) + "%");
        return chance;
    }

    // ===================================================
    // LOGIKA NAPADA
    // ===================================================

    private void performAttack() {
        if (attacksLeft <= 0 || currentBoss == null) return;

        attacksLeft--;
        boolean hit = new Random().nextDouble() < hitChance;

        if (hit) {
            currentBossHp -= playerPP;
            if (currentBossHp < 0) currentBossHp = 0;
            addToBattleLog("⚔ Pogodak! Naneo si " + playerPP + " štete. "
                    + "Boss HP: " + currentBossHp);
        } else {
            addToBattleLog("💨 Promašaj! Boss se izmaknuo.");
        }

        updateHpDisplay();
        updateAttacksDisplay();

        // Proveri kraj borbe
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
        btnAttack.setEnabled(false);

        if (bossDefeated) {
            int coinsEarned = currentBoss.getCoins();
            addToBattleLog("🏆 POBEDA! Boss je poražen!");
            addToBattleLog("💰 Osvojeno: " + coinsEarned + " coins");

            bossViewModel.markBossDefeated(currentBoss,
                    new BossViewModel.UpdateDoneCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Boss označen kao poražen");
                            // TODO: Korak 4 — dodeli coins korisniku + šansa za opremu
                        }
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Greška: " + error);
                        }
                    });

        } else {
            // Boss nije poražen — provjeri koliko HP je ostalo
            boolean halfDefeated = currentBossHp <= (currentBoss.getMaxHp() / 2);

            if (halfDefeated) {
                int halfCoins = currentBoss.getCoins() / 2;
                addToBattleLog("⚠ Boss nije poražen, ali si umanjio 50% HP!");
                addToBattleLog("💰 Osvojeno: " + halfCoins + " coins (polovično)");
                // TODO: Korak 4 — dodeli polovinu coins-a
            } else {
                addToBattleLog("💀 PORAZ! Nisi uspio umanjiti 50% HP bossa.");
                addToBattleLog("💰 Osvojeno: 0 coins");
            }
        }
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
        tvAttacksLeft.setText("Preostali napadi: " + attacksLeft + " / " + MAX_ATTACKS);
        tvPlayerPP.setText("⚔ Tvoja snaga (PP): " + playerPP);
    }

    private void updateRewardsDisplay() {
        tvPotentialRewards.setText("💰 " + currentBoss.getCoins()
                + " coins  |  20% šansa za opremu");
    }

    private void updateEquipmentDisplay(User user) {
        // Kolega čuva aktivnu opremu — za sada prikazujemo PP kao potvrdu
        // TODO: Korak — kada kolega preda strukturu aktivne opreme, prikazati emoji listu
        tvActiveEquipment.setText("PP: " + user.getPp());
        tvPlayerPP.setText("⚔ Tvoja snaga (PP): " + user.getPp());
    }

    private void addToBattleLog(String message) {
        battleLog.insert(0, message + "\n");
        tvBattleLog.setText(battleLog.toString());
    }
}