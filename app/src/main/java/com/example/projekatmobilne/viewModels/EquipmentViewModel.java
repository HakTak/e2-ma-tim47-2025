package com.example.projekatmobilne.viewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.EquipmentRepository;
import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.services.EquipmentService;

import java.util.List;

public class EquipmentViewModel extends AndroidViewModel {

    private final EquipmentService equipmentService;

    // ===== LiveData =====
    public MutableLiveData<List<Equipment>> equipmentList = new MutableLiveData<>();
    public MutableLiveData<User> userData = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public MutableLiveData<String> successMessage = new MutableLiveData<>();

    // Event za prikaz cene u prodavnici
    public MutableLiveData<Integer> itemPrice = new MutableLiveData<>();

    public EquipmentViewModel(@NonNull Application application) {
        super(application);
        this.equipmentService = new EquipmentService();
    }

    // ===== UČITAJ SVU OPREMU =====
    public void loadEquipment(String userId) {
        EquipmentRepository repo = new EquipmentRepository();
        repo.getEquipmentList(userId, new EquipmentRepository.EquipmentListCallback() {
            @Override
            public void onSuccess(List<Equipment> list) {
                equipmentList.postValue(list);
            }
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== UČITAJ KORISNIKA =====
    public void loadUser(String userId) {
        UserRepository userRepository = new UserRepository();
        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                userData.postValue(user);
            }
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== KUPOVINA =====
    public void buyEquipment(User user, EquipmentSubtype subtype) {
        equipmentService.buyEquipment(user, subtype, new EquipmentService.SimpleCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue("Uspešno kupljeno: " + subtype.name());
                // Osvježi listu opreme i korisnika
                loadEquipment(user.getId());
                loadUser(user.getId());
            }
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== AKTIVACIJA =====
    public void activateEquipment(User user, Equipment equipment) {
        List<Equipment> currentList = equipmentList.getValue();
        equipmentService.activateEquipment(user, equipment, currentList,
                new EquipmentService.ActivationCallback() {
                    @Override
                    public void onSuccess(int newPP, int ppBonus) {
                        String msg = ppBonus > 0
                                ? "Oprema aktivirana! +" + ppBonus + " PP"
                                : "Oprema aktivirana!";
                        successMessage.postValue(msg);
                        loadEquipment(user.getId());
                        loadUser(user.getId());
                    }
                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                    }
                });
    }

    // ===== UNAPREĐENJE ORUŽJA =====
    public void upgradeWeapon(User user, Equipment weapon) {
        equipmentService.upgradeWeapon(user, weapon, new EquipmentService.SimpleCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue("Oružje uspešno unapređeno!");
                loadEquipment(user.getId());
                loadUser(user.getId());
            }
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // ===== ZAVRŠETAK BORBE (poziva kolega) =====
    public void onBossFightFinished(User user) {
        List<Equipment> currentList = equipmentList.getValue();
        if (currentList == null) return;

        equipmentService.onBossFightFinished(user, currentList,
                new EquipmentService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        loadEquipment(user.getId());
                        loadUser(user.getId());
                    }
                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                    }
                });
    }

    // ===== DOHVATI CENU (za prikaz u prodavnici) =====
    public int getPrice(EquipmentSubtype subtype, int userLevel) {
        return equipmentService.getPrice(subtype, userLevel);
    }

    public int getUpgradePrice(int userLevel) {
        return equipmentService.getUpgradePrice(userLevel);
    }
}