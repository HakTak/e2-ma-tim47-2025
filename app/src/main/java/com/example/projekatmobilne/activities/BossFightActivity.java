package com.example.projekatmobilne.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.BossViewModel;

public class BossFightActivity extends AppCompatActivity {

    private static final String TAG = "BOSS_FIGHT_ACTIVITY";

    private BossViewModel bossViewModel;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Privremeno koristimo postojeći layout, zamenićemo ga
        setContentView(R.layout.activity_tasks);

        prefsManager  = new SharedPrefsManager(this);
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);

        // Učitaj sve bossove i pronađi sledećeg
        bossViewModel.loadAllBosses();

        bossViewModel.getPendingBoss().observe(this, boss -> {
            if (boss != null) {
                Log.d(TAG, "Sledeći boss: Level " + boss.getLevel()
                        + " | HP: " + boss.getMaxHp()
                        + " | Coins: " + boss.getCoins());
                Toast.makeText(this,
                        "Boss Level " + boss.getLevel() + " | HP: " + boss.getMaxHp(),
                        Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Nema bossova za borbu");
                Toast.makeText(this, "Nema bossova za borbu!", Toast.LENGTH_LONG).show();
            }
        });
    }
}