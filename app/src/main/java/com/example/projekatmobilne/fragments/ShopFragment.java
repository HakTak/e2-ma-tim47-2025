package com.example.projekatmobilne.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.EquipmentViewModel;

public class ShopFragment extends Fragment {

    private EquipmentViewModel equipmentViewModel;
    private SharedPrefsManager prefsManager;
    private User currentUser;

    // Cene
    private TextView tvShopCoins;
    private TextView tvPricePotion20, tvPricePotion40;
    private TextView tvPricePotionPerm5, tvPricePotionPerm10;
    private TextView tvPriceGloves, tvPriceShield, tvPriceBoots;

    // Dugmad
    private Button btnBuyPotion20, btnBuyPotion40;
    private Button btnBuyPotionPerm5, btnBuyPotionPerm10;
    private Button btnBuyGloves, btnBuyShield, btnBuyBoots;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        prefsManager = new SharedPrefsManager(requireContext());
        equipmentViewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);

        initViews(view);
        observeViewModel();

        String userId = prefsManager.getUserId();
        if (userId != null) {
            equipmentViewModel.loadUser(userId);
        }

        return view;
    }

    private void initViews(View view) {
        tvShopCoins = view.findViewById(R.id.tvShopCoins);

        tvPricePotion20    = view.findViewById(R.id.tvPricePotion20);
        tvPricePotion40    = view.findViewById(R.id.tvPricePotion40);
        tvPricePotionPerm5 = view.findViewById(R.id.tvPricePotionPerm5);
        tvPricePotionPerm10= view.findViewById(R.id.tvPricePotionPerm10);
        tvPriceGloves      = view.findViewById(R.id.tvPriceGloves);
        tvPriceShield      = view.findViewById(R.id.tvPriceShield);
        tvPriceBoots       = view.findViewById(R.id.tvPriceBoots);

        btnBuyPotion20     = view.findViewById(R.id.btnBuyPotion20);
        btnBuyPotion40     = view.findViewById(R.id.btnBuyPotion40);
        btnBuyPotionPerm5  = view.findViewById(R.id.btnBuyPotionPerm5);
        btnBuyPotionPerm10 = view.findViewById(R.id.btnBuyPotionPerm10);
        btnBuyGloves       = view.findViewById(R.id.btnBuyGloves);
        btnBuyShield       = view.findViewById(R.id.btnBuyShield);
        btnBuyBoots        = view.findViewById(R.id.btnBuyBoots);
    }

    private void observeViewModel() {
        equipmentViewModel.userData.observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;
            currentUser = user;
            updateUI(user);
        });

        equipmentViewModel.successMessage.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                equipmentViewModel.successMessage.setValue(null);
            }
        });

        equipmentViewModel.errorMessage.observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                equipmentViewModel.errorMessage.setValue(null);
            }
        });
    }

    private void updateUI(User user) {
        // Novčići
        tvShopCoins.setText("🪙 " + user.getCoins());

        // Ako korisnik nije porazio ni jednog bosa, zaključaj prodavnicu
        boolean shopLocked = user.getLevel() < 1;

        if (shopLocked) {
            lockShop();
            return;
        }

        // Prikaži cene
        int level = user.getLevel();
        tvPricePotion20.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.POTION_20, level));
        tvPricePotion40.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.POTION_40, level));
        tvPricePotionPerm5.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.POTION_PERM_5, level));
        tvPricePotionPerm10.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.POTION_PERM_10, level));
        tvPriceGloves.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.GLOVES, level));
        tvPriceShield.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.SHIELD, level));
        tvPriceBoots.setText("🪙 " + equipmentViewModel.getPrice(EquipmentSubtype.BOOTS, level));

        // Postavi listenere za kupovinu
        btnBuyPotion20.setOnClickListener(v -> buy(EquipmentSubtype.POTION_20));
        btnBuyPotion40.setOnClickListener(v -> buy(EquipmentSubtype.POTION_40));
        btnBuyPotionPerm5.setOnClickListener(v -> buy(EquipmentSubtype.POTION_PERM_5));
        btnBuyPotionPerm10.setOnClickListener(v -> buy(EquipmentSubtype.POTION_PERM_10));
        btnBuyGloves.setOnClickListener(v -> buy(EquipmentSubtype.GLOVES));
        btnBuyShield.setOnClickListener(v -> buy(EquipmentSubtype.SHIELD));
        btnBuyBoots.setOnClickListener(v -> buy(EquipmentSubtype.BOOTS));
    }

    private void buy(EquipmentSubtype subtype) {
        if (currentUser == null) return;
        equipmentViewModel.buyEquipment(currentUser, subtype);
    }

    private void lockShop() {
        // Onemogući sva dugmad i prikaži poruku
        btnBuyPotion20.setEnabled(false);
        btnBuyPotion40.setEnabled(false);
        btnBuyPotionPerm5.setEnabled(false);
        btnBuyPotionPerm10.setEnabled(false);
        btnBuyGloves.setEnabled(false);
        btnBuyShield.setEnabled(false);
        btnBuyBoots.setEnabled(false);

        tvPricePotion20.setText("🔒");
        tvPricePotion40.setText("🔒");
        tvPricePotionPerm5.setText("🔒");
        tvPricePotionPerm10.setText("🔒");
        tvPriceGloves.setText("🔒");
        tvPriceShield.setText("🔒");
        tvPriceBoots.setText("🔒");

        Toast.makeText(requireContext(),
                "Prodavnica se otključava nakon prve pobede nad bosom!",
                Toast.LENGTH_LONG).show();
    }
}