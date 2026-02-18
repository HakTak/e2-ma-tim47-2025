package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import androidx.navigation.Navigation;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.FrequencyType;
import com.example.projekatmobilne.enums.TaskStatus;
import com.example.projekatmobilne.fragments.EquipmentFragment;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.Task;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.TaskRepository;
import com.example.projekatmobilne.services.EquipmentService;
import com.example.projekatmobilne.services.TaskService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.utils.SpriteAnimationView;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.EquipmentViewModel;
import com.example.projekatmobilne.viewModels.UserViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BossFightActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BOSS_FIGHT_ACTIVITY";
    private static final int BASE_MAX_ATTACKS = 5;

    // Views
    private TextView tvBossName, tvBossHp, tvPlayerPP, tvHitChance;
    private TextView tvAttacksLeft, tvActiveEquipment, tvPotentialRewards, tvBattleLog;
    private ProgressBar progressBossHp, progressPlayerPP;
    private Button btnAttack;

    // State
    private Boss currentBoss;
    private User currentUser;
    private List<Equipment> activeEquipment = new ArrayList<>();
    private int currentBossHp;
    private int maxAttacks = BASE_MAX_ATTACKS;
    private int attacksLeft;
    private static final float SHAKE_THRESHOLD = 8.0f;
    private static final long SHAKE_WAIT_MS = 1000;
    private long lastShakeTime = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
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
    // Sprite
    private SpriteAnimationView spriteViewBoss;
    private Bitmap hitSheet;
    private Bitmap dodgeSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        initViews();
        initServices();
        loadData();
        showEquipmentDialog();
    }

    // ===================================================
    // INICIJALIZACIJA
    // ===================================================

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Borba sa Bosom");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ← koristi field, ne lokalna varijabla
        spriteViewBoss = findViewById(R.id.spriteViewBoss);

        Bitmap idleSheet = BitmapFactory.decodeResource(getResources(), R.drawable.idle);
        hitSheet = BitmapFactory.decodeResource(getResources(), R.drawable.hit);   // ← field
        dodgeSheet = BitmapFactory.decodeResource(getResources(), R.drawable.dodge); // ← field

        spriteViewBoss.setScaleFactor(2.5f);
        spriteViewBoss.setIdleSheet(idleSheet, 4);
        spriteViewBoss.startIdleAnimation();

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
        progressPlayerPP   = findViewById(R.id.progressPlayerPP);

        btnAttack.setEnabled(false);
        btnAttack.setOnClickListener(v -> performAttack());
    }

    private void initServices() {
        prefsManager       = new SharedPrefsManager(this);
        bossViewModel      = new ViewModelProvider(this).get(BossViewModel.class);
        userViewModel      = new ViewModelProvider(this).get(UserViewModel.class);
        equipmentViewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);
        equipmentService   = new EquipmentService();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
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
            if (user != null) {
                currentUser = user;
                playerPP = user.getPp(); // ← uvijek osvježi PP

                if (!userLoaded) {
                    userLoaded = true;
                    Log.d(TAG, "Korisnik učitan | PP: " + playerPP);
                    tryStartFight();
                } else {
                    // PP ažuriran (npr. nakon aktivacije opreme)
                    updateAttacksDisplay();
                }
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (fightFinished) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > SHAKE_WAIT_MS) {
                lastShakeTime = now;
                if (btnAttack.isEnabled()) {
                    performAttack();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
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

        // Prvo izračunaj
      //  playerPP = equipmentService.calculateEffectivePP(currentUser.getPp(), activeEquipment);
// Tek onda prikaži
        playerPP = currentUser.getPp(); // ← direktno, bez calculateEffectivePP

        Log.d(TAG, "=== SETUP BOSS FIGHT ===");
        Log.d(TAG, "currentUser.getPp() = " + currentUser.getPp());
        Log.d(TAG, "currentUser.getBasePP() = " + currentUser.getBasePP());
        Log.d(TAG, "Broj aktivnih opreme: " + activeEquipment.size());
        for (Equipment e : activeEquipment) {
            Log.d(TAG, "  Oprema: " + e.getSubtype().name()
                    + " | isActive: " + e.isActive()
                    + " | ppBonusApplied: " + e.getPpBonusApplied()
                    + " | isSingleUse: " + e.isSingleUse()
                    + " | type: " + e.getType().name());
        }
        // ========================

        playerPP = currentUser.getPp(); // trenutno nakon fixa
        Log.d(TAG, "playerPP koji se koristi: " + playerPP);

        tvPlayerPP.setText("⚔ Tvoja snaga (PP): " + playerPP);

        addToBattleLog("Borba počinje! Boss ima " + currentBoss.getMaxHp() + " HP.");
        if (maxAttacks > BASE_MAX_ATTACKS) {
            addToBattleLog("🥾 Čizme su aktivne! Imaš " + maxAttacks + " napada.");
        }


        progressPlayerPP.setProgress(playerPP);

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
            Log.d(TAG, "Nema zadataka — default šansa 20%");
            return 0.2;
        }

        int done   = 0;
        int active = 0;
        int failed = 0;

        for (Task task : tasks) {

            // >>> RECURRING TASK: Prolazi kroz SVE datume iz recurringDates <
            if (task.getFrequencyType() == FrequencyType.RECURRING
                    && task.getRecurringDates() != null
                    && !task.getRecurringDates().isEmpty()) {

                Log.d(TAG, "Processing RECURRING task: " + task.getTitle()
                        + " | Broj datuma: " + task.getRecurringDates().size());

                for (Long timestamp : task.getRecurringDates()) {
                    String dateKey = TaskService.timestampToDateKey(timestamp);

                    // Proveri da li ima status u occurrenceStatuses mapi
                    TaskStatus status;
                    if (task.getOccurrenceStatuses() != null
                            && task.getOccurrenceStatuses().containsKey(dateKey)) {
                        try {
                            status = TaskStatus.valueOf(task.getOccurrenceStatuses().get(dateKey));
                        } catch (Exception e) {
                            Log.e(TAG, "Greška parsiranja statusa za " + dateKey + ": " + e.getMessage());
                            status = TaskStatus.ACTIVE; // fallback
                        }
                    } else {
                        // Nema unosa u mapi → ACTIVE po defaultu
                        status = TaskStatus.ACTIVE;
                    }

                    // Broji samo DONE, ACTIVE, FAILED
                    switch (status) {
                        case DONE:
                            done++;
                            Log.d(TAG, "  ✅ " + dateKey + " → DONE");
                            break;
                        case ACTIVE:
                            active++;
                            Log.d(TAG, "  ⭕ " + dateKey + " → ACTIVE");
                            break;
                        case FAILED:
                            failed++;
                            Log.d(TAG, "  ❌ " + dateKey + " → FAILED");
                            break;
                        default:
                            Log.d(TAG, "  ⏸️ " + dateKey + " → " + status + " (ne broji se)");
                            break;
                    }
                }

            }
            // >>> ONE_TIME TASK: STARA LOGIKA SA occurrenceStatuses <
            else {
                // Taskovi koji imaju occurrenceStatuses — koristimo ih
                if (task.getOccurrenceStatuses() != null && !task.getOccurrenceStatuses().isEmpty()) {
                    for (Map.Entry<String, String> entry : task.getOccurrenceStatuses().entrySet()) {
                        try {
                            TaskStatus s = TaskStatus.valueOf(entry.getValue());
                            switch (s) {
                                case DONE:   done++;   break;
                                case ACTIVE: active++; break;
                                case FAILED: failed++; break;
                                // CANCELLED, PAUSED, UPCOMING — ignorišemo
                            }
                            Log.d(TAG, "Task (ONE_TIME): " + task.getTitle()
                                    + " | datum: " + entry.getKey()
                                    + " | status: " + s
                                    + " | broji se: " + (s == TaskStatus.DONE || s == TaskStatus.ACTIVE || s == TaskStatus.FAILED));
                        } catch (Exception e) {
                            Log.e(TAG, "Greška parsiranja statusa: " + entry.getValue());
                        }
                    }
                } else {
                    // Taskovi koji nemaju occurrenceStatuses — koristimo task.getStatus()
                    TaskStatus s = task.getStatus();
                    if (s == null) continue;
                    switch (s) {
                        case DONE:   done++;   break;
                        case ACTIVE: active++; break;
                        case FAILED: failed++; break;
                    }
                    Log.d(TAG, "Task (ONE_TIME, no occurrences): " + task.getTitle()
                            + " | status: " + s
                            + " | broji se: " + (s == TaskStatus.DONE || s == TaskStatus.ACTIVE || s == TaskStatus.FAILED));
                }
            }
        }

        int total = done + active + failed;

        Log.d(TAG, "=== ŠANSA POGOTKA ===");
        Log.d(TAG, "DONE: " + done + " | ACTIVE: " + active + " | FAILED: " + failed);
        Log.d(TAG, "Ukupno (DONE+ACTIVE+FAILED): " + total);

        if (total == 0) {
            Log.d(TAG, "Nema relevantnih zadataka — default šansa 50%");
            return 0.5;
        }

        double chance = (double) done / total;
        Log.d(TAG, "Šansa: " + done + "/" + total + " = " + (int)(chance * 100) + "%");

        return chance;
    }

    // ===================================================
    // LOGIKA NAPADA
    // ===================================================

    private void performAttack() {
        if (attacksLeft <= 0 || fightFinished) return;
        btnAttack.setEnabled(false);

        attacksLeft--;
        boolean hit = new Random().nextDouble() < hitChance;

        if (hit) {
            currentBossHp -= playerPP;
            if (currentBossHp < 0) currentBossHp = 0;
            addToBattleLog("⚔ Pogodak! Naneo si " + playerPP + " štete. Boss HP: " + currentBossHp);

            spriteViewBoss.playOneShotAnimation(hitSheet, 4, () -> {
                updateHpDisplay();
                updateAttacksDisplay();
                if (currentBossHp <= 0) {
                    endFight(true);
                } else if (attacksLeft <= 0) {
                    endFight(false);
                } else {
                    btnAttack.setEnabled(true);
                }
            });

        } else {
            addToBattleLog("💨 Promašaj! Boss se izmaknuo.");

            spriteViewBoss.playOneShotAnimation(dodgeSheet, 8, () -> { // ← 8 frejmova za dodge
                updateHpDisplay();
                updateAttacksDisplay();
                if (attacksLeft <= 0) {
                    endFight(false);
                } else {
                    btnAttack.setEnabled(true);
                }
            });
        }
    }

    // ===================================================
    // KRAJ BORBE
    // ===================================================

    private void endFight(boolean bossDefeated) {
        fightFinished = true;
        btnAttack.setEnabled(false);

        Log.d(TAG, "endFight: JEL SAM POZVAN");

        if (bossDefeated) {
            Log.d(TAG, "endFight: USAO U IF");
            Bitmap deadSheet = BitmapFactory.decodeResource(getResources(), R.drawable.death);
            spriteViewBoss.playFinalAnimation(deadSheet, 4, () -> {
                spriteViewBoss.postDelayed(() -> {
                    Log.d(TAG, "endFight: Treba se pozvati pobeda");
                    handleVictory();
                }, 1000);
            });
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
    private void showEquipmentDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚔️ Pripremi se za borbu!")
                .setMessage("Pre borbe možeš aktivirati opremu koja će ti pomoći.\n\nŽeliš li otvoriti stranicu sa opremom?")
                .setPositiveButton("Opremi se!", (dialog, which) -> {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("navigateTo", "equipment");
                    intent.putExtra("fromBossFight", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Idi u borbu", (dialog, which) -> {
                    // Zatvori dialog i ostani na BossFightActivity
                    dialog.dismiss();
                })
                .setCancelable(false) // korisnik mora da odabere
                .show();
    }

    private void updateAttacksDisplay() {
        tvAttacksLeft.setText("Preostali napadi: " + attacksLeft + " / " + maxAttacks);
       // playerPP = equipmentService.calculateEffectivePP(currentUser.getPp(), activeEquipment);
        playerPP = currentUser.getPp(); // ← direktno, bez calculateEffectivePP
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
                case GLOVES:         emojis.append("🥊 "); break;
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
            case GLOVES:         return "🥊";
            case SHIELD:         return "🛡️";
            case BOOTS:          return "👢";
            case SWORD:          return "⚔️";
            case BOW:            return "🏹";
            default:             return "🎁";
        }
    }
}