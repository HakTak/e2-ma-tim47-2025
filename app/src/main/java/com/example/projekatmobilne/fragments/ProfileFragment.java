package com.example.projekatmobilne.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.utils.QRCodeGenerator;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.UserViewModel;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPP, tvXP, tvCoins;
    private ProgressBar progressXP;
    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvLevel = view.findViewById(R.id.tvProfileLevel);
        tvTitle = view.findViewById(R.id.tvProfileTitle);
        tvPP = view.findViewById(R.id.tvProfilePP);
        tvXP = view.findViewById(R.id.tvProfileXP);
        tvCoins = view.findViewById(R.id.tvProfileCoins);
        progressXP = view.findViewById(R.id.progressXP);

        prefsManager = new SharedPrefsManager(requireContext());
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Load user data
        String userId = prefsManager.getUserId();
        if (userId != null) {
            userViewModel.loadUser(userId);
        }

        // Observe user data
        userViewModel.userData.observe(getViewLifecycleOwner(), this::displayUserData);

        return view;
    }

    private void displayUserData(User user) {
        tvUsername.setText(user.getUsername());
        tvLevel.setText(getString(R.string.level) + ": " + user.getLevel());
        tvTitle.setText(user.getTitle());
        tvPP.setText(getString(R.string.pp_label) + ": " + user.getPp());
        tvXP.setText(getString(R.string.xp_label) + ": " + user.getXp() + " / 200");
        tvCoins.setText(getString(R.string.coins_label) + ": " + user.getCoins());

        // Avatar
        int avatarResId = getResources().getIdentifier(user.getAvatar(), "drawable", requireContext().getPackageName());
        ivAvatar.setImageResource(avatarResId);

        // XP Progress bar
        progressXP.setMax(200); // Level 1 threshold
        progressXP.setProgress(user.getXp());

        // QR Code
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(user.getId(), 300, 300);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        }
    }
}