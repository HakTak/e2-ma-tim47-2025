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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.EquipmentRepository;
import com.example.projekatmobilne.services.LevelService;
import com.example.projekatmobilne.services.UserService;
import com.example.projekatmobilne.utils.QRCodeGenerator;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.UserViewModel;

import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvPP, tvXP, tvCoins;
    private TextView tvProfileBadges;
    private ProgressBar progressXP;
    private Button btnChangePassword;
    private LinearLayout layoutEquipmentContainer;  // NOVO - zamjenjuje RecyclerView

    private UserViewModel userViewModel;
    private SharedPrefsManager prefsManager;
    private UserService userService;
    private LevelService levelService;
    private EquipmentRepository equipmentRepository;

    private String viewedUserId;
    private boolean isOwnProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        prefsManager = new SharedPrefsManager(requireContext());
        String currentUserId = prefsManager.getUserId();

        if (getArguments() != null) {
            viewedUserId = getArguments().getString("userId");
        }
        if (viewedUserId == null) {
            viewedUserId = currentUserId;
        }

        isOwnProfile = viewedUserId.equals(currentUserId);

        View view;
        if (isOwnProfile) {
            view = inflater.inflate(R.layout.fragment_profile, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_profile_public, container, false);
        }

        ivAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvLevel = view.findViewById(R.id.tvProfileLevel);
        tvTitle = view.findViewById(R.id.tvProfileTitle);
        tvXP = view.findViewById(R.id.tvProfileXP);
        progressXP = view.findViewById(R.id.progressXP);

        if (isOwnProfile) {
            tvPP = view.findViewById(R.id.tvProfilePP);
            tvCoins = view.findViewById(R.id.tvProfileCoins);
            tvProfileBadges = view.findViewById(R.id.tvProfileBadges);
            layoutEquipmentContainer = view.findViewById(R.id.layoutEquipmentContainer);
            btnChangePassword = view.findViewById(R.id.btnChangePassword);
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        } else {
            tvProfileBadges = view.findViewById(R.id.tvProfileBadges);
        }

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userService = new UserService();
        levelService = new LevelService();
        equipmentRepository = new EquipmentRepository();

        userViewModel.loadUser(viewedUserId);
        userViewModel.userData.observe(getViewLifecycleOwner(), this::displayUserData);

        return view;
    }

    private void displayUserData(User user) {
        tvUsername.setText(user.getUsername());
        tvLevel.setText(getString(R.string.level) + ": " + user.getLevel());
        tvTitle.setText(user.getTitle());

        // XP Progress bar
        int currentLevel = user.getLevel();
        int currentXP = user.getXp();
        int currentLevelThreshold = levelService.calculateXPThreshold(currentLevel);
        int nextLevelThreshold = levelService.calculateXPThreshold(currentLevel + 1);
        int xpInCurrentLevel = currentXP - currentLevelThreshold;
        int xpNeededForNextLevel = nextLevelThreshold - currentLevelThreshold;

        tvXP.setText(getString(R.string.xp_label) + ": " + currentXP + " / " + nextLevelThreshold);
        progressXP.setMax(xpNeededForNextLevel > 0 ? xpNeededForNextLevel : 1);
        progressXP.setProgress(Math.max(0, xpInCurrentLevel));

        // Avatar
        int avatarResId = getResources().getIdentifier(
                user.getAvatar(), "drawable", requireContext().getPackageName());
        ivAvatar.setImageResource(avatarResId);

        // QR Code
        if (ivQRCode != null) {
            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(user.getId(), 300, 300);
            if (qrBitmap != null) {
                ivQRCode.setImageBitmap(qrBitmap);
            }
        }

        // Bedževi
        if (tvProfileBadges != null) {
            int badgeCount = user.getBadges() != null ? user.getBadges().size() : 0;
            tvProfileBadges.setText("Broj bedževa: " + badgeCount);
        }

        // Privatni profil
        if (isOwnProfile) {
            tvPP.setText(getString(R.string.pp_label) + ": " + user.getPp());
            tvCoins.setText(getString(R.string.coins_label) + ": " + user.getCoins());
            loadEquipment(user.getId());
        }
    }

    private void loadEquipment(String userId) {
        equipmentRepository.getEquipmentList(userId, new EquipmentRepository.EquipmentListCallback() {
            @Override
            public void onSuccess(List<Equipment> equipmentList) {
                if (!isAdded() || layoutEquipmentContainer == null) return;

                layoutEquipmentContainer.removeAllViews();

                if (equipmentList.isEmpty()) {
                    TextView tv = new TextView(requireContext());
                    tv.setText("Nema kupljene opreme");
                    tv.setTextSize(14);
                    tv.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                    tv.setTypeface(null, android.graphics.Typeface.ITALIC);
                    layoutEquipmentContainer.addView(tv);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(requireContext());
                for (Equipment e : equipmentList) {
                    View itemView = inflater.inflate(R.layout.item_equipment,
                            layoutEquipmentContainer, false);

                    TextView tvIcon   = itemView.findViewById(R.id.tvEquipmentIcon);
                    TextView tvName   = itemView.findViewById(R.id.tvEquipmentName);
                    TextView tvBonus  = itemView.findViewById(R.id.tvEquipmentBonus);
                    TextView tvStatus = itemView.findViewById(R.id.tvEquipmentStatus);
                    Button btnAction  = itemView.findViewById(R.id.btnEquipmentAction);

                    btnAction.setVisibility(View.GONE);
                    tvName.setText(e.getDisplayName());
                    tvBonus.setText(getBonusDescription(e));
                    tvIcon.setText(getEquipmentIcon(e.getSubtype()));

                    if (e.isActive()) {
                        tvStatus.setText("✅ Aktivna");
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                    } else if (e.isUsed()) {
                        tvStatus.setText("❌ Potrošena");
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"));
                    } else {
                        tvStatus.setText("💤 Neaktivna");
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#757575"));
                    }

                    layoutEquipmentContainer.addView(itemView);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || layoutEquipmentContainer == null) return;
                TextView tv = new TextView(requireContext());
                tv.setText("Greška pri učitavanju opreme");
                layoutEquipmentContainer.addView(tv);
            }
        });
    }

    private String getEquipmentIcon(com.example.projekatmobilne.enums.EquipmentSubtype subtype) {
        switch (subtype) {
            case POTION_20:
            case POTION_40:      return "🧪";
            case POTION_PERM_5:
            case POTION_PERM_10: return "⚗️";
            case GLOVES:         return "🥊";
            case SHIELD:         return "🛡️";
            case BOOTS:          return "👢";
            case SWORD:          return "⚔️";
            case BOW:            return "🏹";
            default:             return "❓";
        }
    }

    private String getBonusDescription(Equipment e) {
        switch (e.getSubtype()) {
            case POTION_20:      return "+20% PP (jednokratno)";
            case POTION_40:      return "+40% PP (jednokratno)";
            case POTION_PERM_5:  return "+5% PP (trajno)";
            case POTION_PERM_10: return "+10% PP (trajno)";
            case GLOVES:         return "+10% PP • 2 borbe";
            case SHIELD:         return "+10% šansa napada • 2 borbe";
            case BOOTS:          return "40% šansa za +1 napad • 2 borbe";
            case SWORD:
                return String.format("+%.2f%% PP (trajno)", e.getWeaponBonus() * 100);
            case BOW:
                return String.format("+%.2f%% novčići (trajno)", e.getWeaponBonus() * 100);
            default: return "";
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);
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
                Toast.makeText(getContext(),
                        "Nova lozinka mora imati minimum 6 karaktera!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(),
                        "Nove lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
                return;
            }

            userService.changePassword(oldPassword, newPassword, new UserService.PasswordCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(),
                            "Lozinka uspešno promenjena!", Toast.LENGTH_SHORT).show();
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