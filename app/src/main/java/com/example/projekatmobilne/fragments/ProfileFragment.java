package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.services.LevelService;
import com.example.projekatmobilne.services.UserService;
import com.example.projekatmobilne.utils.QRCodeGenerator;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.UserViewModel;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPP, tvXP, tvCoins;
    private TextView tvProfileBadges, tvProfileEquipment;
    private ProgressBar progressXP;
    private Button btnChangePassword;
    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;
    private UserService userService;
    private LevelService levelService;

    private String viewedUserId;
    private boolean isOwnProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        prefsManager = new SharedPrefsManager(requireContext());
        String currentUserId = prefsManager.getUserId();

        // Proveri argumente (ako gledamo tuđi profil)
        if (getArguments() != null) {
            viewedUserId = getArguments().getString("userId");
        }

        // Ako nema argumenta, gledamo sopstveni profil
        if (viewedUserId == null) {
            viewedUserId = currentUserId;
        }

        isOwnProfile = viewedUserId.equals(currentUserId);

        // Učitaj odgovarajući layout
        View view;
        if (isOwnProfile) {
            view = inflater.inflate(R.layout.fragment_profile, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_profile_public, container, false);
        }

        // Initialize views
        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvLevel = view.findViewById(R.id.tvProfileLevel);
        tvTitle = view.findViewById(R.id.tvProfileTitle);
        tvXP = view.findViewById(R.id.tvProfileXP);
        progressXP = view.findViewById(R.id.progressXP);

        // Ova polja postoje samo u privatnom profilu
        if (isOwnProfile) {
            tvPP = view.findViewById(R.id.tvProfilePP);
            tvCoins = view.findViewById(R.id.tvProfileCoins);
            tvProfileBadges = view.findViewById(R.id.tvProfileBadges);
            tvProfileEquipment = view.findViewById(R.id.tvProfileEquipment);
            btnChangePassword = view.findViewById(R.id.btnChangePassword);
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        } else {
            // U javnom profilu
            tvProfileBadges = view.findViewById(R.id.tvProfileBadges);
        }

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userService = new UserService();
        levelService = new LevelService();

        // Load user data
        userViewModel.loadUser(viewedUserId);

        // Observe user data
        userViewModel.userData.observe(getViewLifecycleOwner(), this::displayUserData);

        return view;
    }

    private void displayUserData(User user) {
        tvUsername.setText(user.getUsername());
        tvLevel.setText(getString(R.string.level) + ": " + user.getLevel());
        tvTitle.setText(user.getTitle());
        tvXP.setText(getString(R.string.xp_label) + ": " + user.getXp() + " / " + levelService.calculateXPThreshold(user.getLevel() + 1));

        // Avatar
        int avatarResId = getResources().getIdentifier(user.getAvatar(), "drawable", requireContext().getPackageName());
        ivAvatar.setImageResource(avatarResId);

        // XP Progress bar
        progressXP.setMax(200);
        progressXP.setProgress(user.getXp());

        // QR Code
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(user.getId(), 300, 300);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        }

        // Prikaz bedževa
        int badgeCount = user.getBadges() != null ? user.getBadges().size() : 0;
        tvProfileBadges.setText("Broj bedževa: " + badgeCount);

        // Privatni profil - dodatni podaci
        if (isOwnProfile) {
            tvPP.setText(getString(R.string.pp_label) + ": " + user.getPp());
            tvCoins.setText(getString(R.string.coins_label) + ": " + user.getCoins());

            // Oprema (za sada placeholder)
            if (user.getEquipment() != null && !user.getEquipment().isEmpty()) {
                tvProfileEquipment.setText("Oprema: " + user.getEquipment().size() + " predmeta");
            } else {
                tvProfileEquipment.setText("Oprema dolazi uskoro...");
            }
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmNewPassword = dialogView.findViewById(R.id.etConfirmNewPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmPassword);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmNewPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Popuni sva polja!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Nova lozinka mora imati minimum 6 karaktera!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Nove lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ===== REFAKTORISANO: Koristi UserService =====
            userService.changePassword(oldPassword, newPassword, new UserService.PasswordCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Lozinka uspešno promenjena!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }
}