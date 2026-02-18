package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.services.LevelService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.UserViewModel;

public class LevelProgressFragment extends Fragment {

    private static final String TAG = "LEVEL_PROGRESS_FRAGMENT";

    private TextView tvCurrentLevel;
    private TextView tvCurrentTitle;
    private TextView tvCurrentPP;
    private TextView tvXPProgress;
    private TextView tvNextLevelInfo;
    private ProgressBar progressBarXP;
    private Button btnAddTestXP;

    private UserViewModel userViewModel;
    private BossViewModel bossViewModel;
    private LevelService levelService;
    private SharedPrefsManager prefsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_level_progress, container, false);

        // Initialize views
        tvCurrentLevel   = view.findViewById(R.id.tvCurrentLevel);
        tvCurrentTitle   = view.findViewById(R.id.tvCurrentTitle);
        tvCurrentPP      = view.findViewById(R.id.tvCurrentPP);
        tvXPProgress     = view.findViewById(R.id.tvXPProgress);
        tvNextLevelInfo  = view.findViewById(R.id.tvNextLevelInfo);
        progressBarXP    = view.findViewById(R.id.progressBarXP);
        btnAddTestXP     = view.findViewById(R.id.btnAddTestXP);

        // Initialize services
        prefsManager  = new SharedPrefsManager(requireContext());
        levelService  = new LevelService();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);

        // Učitaj korisnika
        String userId = prefsManager.getUserId();
        if (userId != null) {
            userViewModel.loadUser(userId);
        }

        // Observe user data
        userViewModel.userData.observe(getViewLifecycleOwner(), this::displayLevelProgress);

        // Observe level-up event
        userViewModel.levelUpEvent.observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                showLevelUpDialog(event.newLevel, event.ppGained);
            }
        });

        // ===== TEST DUGME (obrisati kad kolega implementira zadatke) =====
        btnAddTestXP.setOnClickListener(v -> {
            String uid = prefsManager.getUserId();
            if (uid != null) {
                userViewModel.addXP(uid, 50);
            }
        });

        return view;
    }

    // ===================================================
    // UI UPDATE
    // ===================================================

    private void displayLevelProgress(User user) {
        int currentLevel         = user.getLevel();
        int currentXP            = user.getXp();
        int currentLevelThreshold = levelService.calculateXPThreshold(currentLevel);
        int nextLevelThreshold   = levelService.calculateXPThreshold(currentLevel + 1);

        int xpInCurrentLevel     = currentXP - currentLevelThreshold;
        int xpNeededForNextLevel = nextLevelThreshold - currentLevelThreshold;
        int xpRemaining          = nextLevelThreshold - currentXP;

        tvCurrentLevel.setText("Nivo " + currentLevel);
        tvCurrentTitle.setText(user.getTitle());
        tvCurrentPP.setText("PP (Snaga): " + user.getPp());
        tvXPProgress.setText("XP: " + currentXP + " / " + nextLevelThreshold);
        tvNextLevelInfo.setText("Još " + xpRemaining + " XP do Nivoa " + (currentLevel + 1));

        progressBarXP.setMax(xpNeededForNextLevel > 0 ? xpNeededForNextLevel : 1);
        progressBarXP.setProgress(Math.max(0, xpInCurrentLevel));
    }

    // ===================================================
    // LEVEL UP DIALOG + KREIRANJE BOSSA
    // ===================================================

    private void showLevelUpDialog(int newLevel, int ppGained) {
        if (getContext() == null) return;

        // Kreiraj bossa za novi level u pozadini
        bossViewModel.ensureBossExistsForLevel(newLevel, new BossViewModel.BossReadyCallback() {
            @Override
            public void onReady(com.example.projekatmobilne.models.Boss boss) {
                Log.d(TAG, "Boss kreiran za level " + newLevel
                        + " | HP: " + boss.getMaxHp()
                        + " | Coins: " + boss.getCoins());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Greška pri kreiranju bossa: " + error);
            }
        });

        // Prikaži dialog
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_level_up, null);

        TextView tvDialogLevel     = dialogView.findViewById(R.id.tvDialogLevel);
        TextView tvDialogTitle     = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogPP        = dialogView.findViewById(R.id.tvDialogPP);
        TextView tvDialogNextLevel = dialogView.findViewById(R.id.tvDialogNextLevel);
        Button btnClose            = dialogView.findViewById(R.id.btnDialogClose);

        String newTitle      = levelService.getTitleForLevel(newLevel);
        int nextThreshold    = levelService.calculateXPThreshold(newLevel + 1);

        tvDialogLevel.setText("Dostigao si Nivo " + newLevel + "!");
        tvDialogTitle.setText("Titula: " + newTitle);
        tvDialogPP.setText("+" + ppGained + " PP");
        tvDialogNextLevel.setText("Sledeći nivo za " + nextThreshold + " XP");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            userViewModel.levelUpEvent.setValue(null);
        });

        dialog.show();
    }
}