package com.example.projekatmobilne.fragments;

import android.content.Intent;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.activities.BossFightActivity;
import com.example.projekatmobilne.adapters.EquipmentAdapter;
import com.example.projekatmobilne.enums.EquipmentType;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.EquipmentViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class EquipmentFragment extends Fragment {

    // Argument koji šalje kolega kada otvara fragment pre boss fighta
    public static final String ARG_FROM_BOSS_FIGHT = "fromBossFight";

    private EquipmentViewModel equipmentViewModel;
    private SharedPrefsManager prefsManager;
    private User currentUser;

    private TextView tvEquipmentPP;
    private TextView tvEmptyEquipment;
    private Button btnGoToBossFight;
    private RecyclerView rvEquipment;
    private TabLayout tabLayout;

    private EquipmentAdapter adapter;
    private List<Equipment> allEquipment = new ArrayList<>();
    private boolean fromBossFight = false;

    // Trenutno selektovani tab: 0=Napici, 1=Odeća, 2=Oružje
    private int selectedTab = 0;

    // ===== FACTORY METODA =====
    public static EquipmentFragment newInstance(boolean fromBossFight) {
        EquipmentFragment fragment = new EquipmentFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FROM_BOSS_FIGHT, fromBossFight);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment, container, false);

        // Proveri da li je otvoren pre boss fighta
        if (getArguments() != null) {
            fromBossFight = getArguments().getBoolean(ARG_FROM_BOSS_FIGHT, false);
        }

        prefsManager = new SharedPrefsManager(requireContext());
        equipmentViewModel = new ViewModelProvider(this).get(EquipmentViewModel.class);

        initViews(view);
        setupTabs();
        observeViewModel();

        String userId = prefsManager.getUserId();
        if (userId != null) {
            equipmentViewModel.loadUser(userId);
            equipmentViewModel.loadEquipment(userId);
        }

        return view;
    }

    private void initViews(View view) {
        tvEquipmentPP    = view.findViewById(R.id.tvEquipmentPP);
        tvEmptyEquipment = view.findViewById(R.id.tvEmptyEquipment);
        btnGoToBossFight = view.findViewById(R.id.btnGoToBossFight);
        rvEquipment      = view.findViewById(R.id.rvEquipment);
        tabLayout        = view.findViewById(R.id.tabLayoutEquipment);

        // Prikaži dugme "Kreni u borbu" samo ako je otvoren pre boss fighta
        if (fromBossFight) {
            btnGoToBossFight.setVisibility(View.VISIBLE);
            btnGoToBossFight.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), BossFightActivity.class);
                startActivity(intent);
            });
        }

        // Setup RecyclerView
        rvEquipment.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EquipmentAdapter(
                requireContext(),
                new ArrayList<>(),
                new EquipmentAdapter.OnEquipmentActionListener() {
                    @Override
                    public void onActivate(Equipment equipment) {
                        if (currentUser != null) {
                            equipmentViewModel.activateEquipment(currentUser, equipment);
                        }
                    }

                    @Override
                    public void onUpgrade(Equipment equipment) {
                        if (currentUser != null) {
                            showUpgradeConfirmation(equipment);
                        }
                    }
                },
                0, 0
        );
        rvEquipment.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        String userId = prefsManager.getUserId();
        if (userId != null) {
            equipmentViewModel.loadUser(userId);
            equipmentViewModel.loadEquipment(userId);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("🧪 Napici"));
        tabLayout.addTab(tabLayout.newTab().setText("👕 Odeća"));
        tabLayout.addTab(tabLayout.newTab().setText("⚔️ Oružje"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                filterAndShowEquipment();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeViewModel() {
        equipmentViewModel.userData.observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;
            currentUser = user;
            tvEquipmentPP.setText("⚔️ PP: " + user.getPp());
        });

        equipmentViewModel.equipmentList.observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            allEquipment = list;
            filterAndShowEquipment();
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

    /**
     * Filtrira opremu prema selektovanom tabu i prikazuje u RecyclerView.
     */
    private void filterAndShowEquipment() {
        EquipmentType filterType;
        switch (selectedTab) {
            case 1:  filterType = EquipmentType.CLOTHING; break;
            case 2:  filterType = EquipmentType.WEAPON;   break;
            default: filterType = EquipmentType.POTION;   break;
        }

        List<Equipment> filtered = new ArrayList<>();
        for (Equipment e : allEquipment) {
            if (e.getType() == filterType) {
                filtered.add(e);
            }
        }

        adapter.updateList(filtered);

        // Prikaži poruku za praznu listu
        if (filtered.isEmpty()) {
            rvEquipment.setVisibility(View.GONE);
            tvEmptyEquipment.setVisibility(View.VISIBLE);
        } else {
            rvEquipment.setVisibility(View.VISIBLE);
            tvEmptyEquipment.setVisibility(View.GONE);
        }
    }

    /**
     * Dialog za potvrdu unapređenja oružja.
     */
    private void showUpgradeConfirmation(Equipment weapon) {
        if (currentUser == null) return;

        int price = equipmentViewModel.getUpgradePrice(currentUser.getLevel());
        String msg = "Unapređenje košta " + price + " 🪙\n"
                + "Imaš " + currentUser.getCoins() + " 🪙\n\n"
                + "Da li želiš da unaprediš " + weapon.getDisplayName() + "?";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Unapredi oružje")
                .setMessage(msg)
                .setPositiveButton("Unapredi", (dialog, which) ->
                        equipmentViewModel.upgradeWeapon(currentUser, weapon))
                .setNegativeButton("Otkaži", null)
                .show();
    }
}