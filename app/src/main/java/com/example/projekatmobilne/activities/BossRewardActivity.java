package com.example.projekatmobilne.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projekatmobilne.R;

public class BossRewardActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BOSS_REWARD_ACTIVITY";

    // Extras ključevi
    public static final String EXTRA_COINS        = "extra_coins";
    public static final String EXTRA_EQUIPMENT    = "extra_equipment";
    public static final String EXTRA_HAS_EQUIPMENT= "extra_has_equipment";

    // Shake parametri
    private static final float SHAKE_THRESHOLD = 12f;
    private static final int   SHAKE_WAIT_MS   = 1000;

    // Views
    private ImageView    ivChest;
    private TextView     tvRewardTitle;
    private TextView     tvRewardCoins;
    private TextView     tvRewardEquipment;
    private LinearLayout layoutRewards;
    private Button       btnContinue;

    // Senzor
    private SensorManager sensorManager;
    private Sensor        accelerometer;

    // State
    private boolean chestOpened  = false;
    private long    lastShakeTime = 0;
    private int     coinsEarned;
    private String  equipmentEmoji;
    private boolean hasEquipment;

    // ===================================================
    // FACTORY METODA — za pokretanje iz BossFightActivity
    // ===================================================

    public static Intent createIntent(Context context, int coins,
                                      boolean hasEquipment, String equipmentEmoji) {
        Intent intent = new Intent(context, BossRewardActivity.class);
        intent.putExtra(EXTRA_COINS, coins);
        intent.putExtra(EXTRA_HAS_EQUIPMENT, hasEquipment);
        intent.putExtra(EXTRA_EQUIPMENT, equipmentEmoji != null ? equipmentEmoji : "");
        return intent;
    }

    // ===================================================
    // LIFECYCLE
    // ===================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_reward);

        // Sakrij ActionBar na ovom ekranu
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        readExtras();
        initSensor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // ===================================================
    // INICIJALIZACIJA
    // ===================================================

    private void initViews() {
        ivChest           = findViewById(R.id.ivChest);
        tvRewardTitle     = findViewById(R.id.tvRewardTitle);
        tvRewardCoins     = findViewById(R.id.tvRewardCoins);
        tvRewardEquipment = findViewById(R.id.tvRewardEquipment);
        layoutRewards     = findViewById(R.id.layoutRewards);
        btnContinue       = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            // Vrati se na HomeActivity i obriši backstack borbe
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void readExtras() {
        coinsEarned    = getIntent().getIntExtra(EXTRA_COINS, 0);
        hasEquipment   = getIntent().getBooleanExtra(EXTRA_HAS_EQUIPMENT, false);
        equipmentEmoji = getIntent().getStringExtra(EXTRA_EQUIPMENT);
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (accelerometer == null) {
            Log.w(TAG, "Akcelerometar nije dostupan — auto otvaram kovčeg");
            // Na uređajima bez senzora odmah otvorimo
            openChest();
        }
    }

    // ===================================================
    // SHAKE DETEKCIJA
    // ===================================================

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (chestOpened) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Uklonjamo gravitaciju (9.8 m/s²) da dobijemo čisto ubrzanje
        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > SHAKE_WAIT_MS) {
                lastShakeTime = now;
                Log.d(TAG, "Shake detektovan! Ubrzanje: " + acceleration);
                openChest();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nije potrebno
    }

    // ===================================================
    // ANIMACIJA I PRIKAZ NAGRADE
    // ===================================================

    private void openChest() {
        if (chestOpened) return;
        chestOpened = true;

        Log.d(TAG, "Otvaranje kovčega!");
        tvRewardTitle.setText("🎉 Nagrade!");

        // Postavi animaciju i pokreni
        ivChest.setImageResource(R.drawable.chest_animation);
        AnimationDrawable animation = (AnimationDrawable) ivChest.getDrawable();

        // Animacija mora startovati nakon što je View prikazan
        ivChest.post(() -> {
            animation.start();
            // Nakon što animacija završi (15 frejmova x 60ms = 900ms), prikaži nagrade
            ivChest.postDelayed(this::showRewards, 950);
        });
    }

    private void showRewards() {
        layoutRewards.setVisibility(View.VISIBLE);

        // Coins
        tvRewardCoins.setText("💰 " + coinsEarned + " coins");

        // Oprema
        if (hasEquipment && equipmentEmoji != null && !equipmentEmoji.isEmpty()) {
            tvRewardEquipment.setText(equipmentEmoji);
            tvRewardEquipment.setVisibility(View.VISIBLE);
        }

        // Prikaži dugme
        btnContinue.setVisibility(View.VISIBLE);

        Log.d(TAG, "Nagrade prikazane | Coins: " + coinsEarned
                + " | Oprema: " + (hasEquipment ? equipmentEmoji : "nema"));
    }
}