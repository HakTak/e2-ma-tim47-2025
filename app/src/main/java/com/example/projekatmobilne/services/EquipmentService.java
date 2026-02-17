package com.example.projekatmobilne.services;

import com.example.projekatmobilne.database.FirestoreManager;
import com.example.projekatmobilne.enums.EquipmentSubtype;
import com.example.projekatmobilne.enums.EquipmentType;
import com.example.projekatmobilne.models.Equipment;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.EquipmentRepository;
import com.example.projekatmobilne.repositories.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final LevelService levelService;

    public EquipmentService() {
        this.equipmentRepository = new EquipmentRepository();
        this.userRepository = new UserRepository();
        this.levelService = new LevelService();
    }

    // ===================================================================
    // CENE OPREME
    // Cena zavisi od nagrade prethodnog bosa (calculateBossReward(level-1))
    // ===================================================================

    /**
     * Računa nagradu bosa za dati nivo.
     * Nivo 1 = 200 novčića, svaki sledeći +20%
     */
    public int calculateBossReward(int level) {
        if (level <= 0) return 0;
        if (level == 1) return 200;
        return (int) (calculateBossReward(level - 1) * 1.2);
    }

    /**
     * Vraća cenu opreme u novčićima za korisnika na datom nivou.
     */
    public int getPrice(EquipmentSubtype subtype, int userLevel) {
        // Cena se računa na osnovu nagrade prethodnog bosa
        int prevBossReward = calculateBossReward(userLevel - 1);
        if (prevBossReward == 0) prevBossReward = 200; // fallback za nivo 1

        switch (subtype) {
            case POTION_20:      return (int) (prevBossReward * 0.50);
            case POTION_40:      return (int) (prevBossReward * 0.70);
            case POTION_PERM_5:  return (int) (prevBossReward * 2.00);
            case POTION_PERM_10: return (int) (prevBossReward * 10.00);
            case GLOVES:         return (int) (prevBossReward * 0.60);
            case SHIELD:         return (int) (prevBossReward * 0.60);
            case BOOTS:          return (int) (prevBossReward * 0.80);
            default:             return 0; // oružje se ne kupuje
        }
    }

    /**
     * Vraća cenu unapređenja oružja.
     */
    public int getUpgradePrice(int userLevel) {
        int prevBossReward = calculateBossReward(userLevel - 1);
        if (prevBossReward == 0) prevBossReward = 200;
        return (int) (prevBossReward * 0.60);
    }

    // ===================================================================
    // KUPOVINA OPREME
    // ===================================================================

    /**
     * Kupovina napitka ili odeće iz prodavnice.
     * Oružje se ne može kupiti — jedino se dobija od bosa.
     */
    public void buyEquipment(User user, EquipmentSubtype subtype, SimpleCallback callback) {
        // Provera: korisnik mora biti bar nivo 1
        if (user.getLevel() < 1) {
            callback.onError("Moraš pobediti bar jednog bosa pre kupovine!");
            return;
        }

        // Provera: samo napici i odeća se kupuju
        EquipmentType type = getTypeForSubtype(subtype);
        if (type == EquipmentType.WEAPON) {
            callback.onError("Oružje se ne može kupiti — osvaja se u borbi!");
            return;
        }

        // Provera cene
        int price = getPrice(subtype, user.getLevel());
        if (user.getCoins() < price) {
            callback.onError("Nemaš dovoljno novčića! Potrebno: " + price);
            return;
        }

        // Za odeću — proveri da li već poseduje isti tip
        // (ako poseduje, stackuje se bonus, ali dodajemo novi objekat)
        Equipment equipment = new Equipment(user.getId(), type, subtype);

        equipmentRepository.addEquipment(equipment, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                // Oduzmi novčiće
                Map<String, Object> updates = new HashMap<>();
                updates.put("coins", user.getCoins() - price);
                userRepository.updateUser(user.getId(), updates, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ===================================================================
    // AKTIVACIJA OPREME
    // ===================================================================

    /**
     * Aktivira opremu pre borbe sa bosom.
     * Dodaje PP bonus na user.pp i čuva ppBonusApplied u Equipment objektu.
     */
    public void activateEquipment(User user, Equipment equipment,
                                  List<Equipment> allActiveEquipment,
                                  ActivationCallback callback) {
        // Jednokratni napitak koji je već potrošen ne može se aktivirati
        if (equipment.isUsed()) {
            callback.onError("Ovaj napitak je već potrošen!");
            return;
        }

        // Već aktivna oprema
        if (equipment.isActive()) {
            callback.onError("Ova oprema je već aktivna!");
            return;
        }

        if (equipment.getType() == EquipmentType.CLOTHING) {
            int activeCount = 0;
            if (allActiveEquipment != null) {
                for (Equipment e : allActiveEquipment) {
                    if (e.isActive()
                            && e.getSubtype() == equipment.getSubtype()
                            && e.getFightsRemaining() > 0) {
                        activeCount++;
                    }
                }
            }
            if (activeCount >= 2) {
                callback.onError("Već imaš 2 aktivna komada iste odeće ("
                        + equipment.getDisplayName() + ")!");
                return;
            }
        }
        int ppBonus = 0;
        int newPP = user.getPp();
        int newBasePP = user.getBasePP();

        switch (equipment.getSubtype()) {
            case POTION_20:
                // Jednokratno +20% od basePP
                ppBonus = (int) (user.getBasePP() * 0.20);
                newPP += ppBonus;
                break;

            case POTION_40:
                // Jednokratno +40% od basePP
                ppBonus = (int) (user.getBasePP() * 0.40);
                newPP += ppBonus;
                break;

            case POTION_PERM_5:
                // Trajno +5% od trenutnog PP (i postaje deo basePP)
                ppBonus = (int) (user.getPp() * 0.05);
                newPP += ppBonus;
                newBasePP += ppBonus;
                break;

            case POTION_PERM_10:
                // Trajno +10% od trenutnog PP
                ppBonus = (int) (user.getPp() * 0.10);
                newPP += ppBonus;
                newBasePP += ppBonus;
                break;

            case GLOVES:
                // +10% PP od basePP, traje 2 borbe
                // Ako korisnik već ima aktivne rukavice, bonusi se sabiraju
                ppBonus = (int) (user.getBasePP() * 0.10);
                newPP += ppBonus;
                break;

            case SHIELD:
            case BOOTS:
                // Shield i Boots ne utiču na PP
                ppBonus = 0;
                break;

            case SWORD:
                // Trajno +5% PP (weapon bonus)
                ppBonus = (int) (user.getBasePP() * equipment.getWeaponBonus());
                newPP += ppBonus;
                newBasePP += ppBonus;
                break;

            case BOW:
                // Luk ne utiče na PP
                ppBonus = 0;
                break;
        }

        // Sačuvaj ppBonusApplied u Equipment i postavi active = true
        Map<String, Object> equipUpdates = new HashMap<>();
        equipUpdates.put("active", true);
        equipUpdates.put("ppBonusApplied", ppBonus);

        // Permanentna oprema — odmah označi kao "used" jer efekt je trajan
        if (equipment.isPermanent()) {
            equipUpdates.put("used", true);
        }

        int finalNewPP = newPP;
        int finalNewBasePP = newBasePP;
        int finalPPBonus = ppBonus;

        equipmentRepository.updateEquipment(user.getId(), equipment.getId(),
                equipUpdates, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // Ažuriraj PP kod korisnika
                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("pp", finalNewPP);
                        userUpdates.put("basePP", finalNewBasePP);

                        userRepository.updateUser(user.getId(), userUpdates,
                                new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess(finalNewPP, finalPPBonus);
                                    }
                                    @Override
                                    public void onError(String error) {
                                        callback.onError(error);
                                    }
                                });
                    }
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    // ===================================================================
    // ZAVRŠETAK BORBE SA BOSOM
    // Poziva kolega nakon svake borbe (pobeda ili poraz)
    // ===================================================================

    /**
     * Ažurira opremu nakon završene borbe:
     * - Jednokratne napitke označava kao used i oduzima PP
     * - Odeći smanjuje fightsRemaining, briše je kad dođe do 0 i oduzima PP
     */
    public void onBossFightFinished(User user, List<Equipment> activeEquipment,
                                    SimpleCallback callback) {
        if (activeEquipment == null || activeEquipment.isEmpty()) {
            callback.onSuccess();
            return;
        }

        int[] ppToRemove = {0};
        int[] processed = {0};
        int total = activeEquipment.size();

        for (Equipment e : activeEquipment) {
            if (!e.isActive()) {
                processed[0]++;
                if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                continue;
            }

            if (e.isSingleUse() && !e.isUsed()) {
                // Jednokratni napitak — oduzmi PP i označi kao potrošen
                ppToRemove[0] += e.getPpBonusApplied();

                Map<String, Object> updates = new HashMap<>();
                updates.put("used", true);
                updates.put("active", false);

                equipmentRepository.updateEquipment(user.getId(), e.getId(), updates, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        processed[0]++;
                        if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                    }
                    @Override
                    public void onError(String error) {
                        processed[0]++;
                        if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                    }
                });

            } else if (e.getType() == EquipmentType.CLOTHING) {
                // Odeća — smanji fightsRemaining
                int newFights = e.getFightsRemaining() - 1;

                if (newFights <= 0) {
                    // Odeća je istekla — oduzmi PP i obriši
                    ppToRemove[0] += e.getPpBonusApplied();
                    equipmentRepository.deleteEquipment(user.getId(), e.getId(), new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            processed[0]++;
                            if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                        }
                        @Override
                        public void onError(String error) {
                            processed[0]++;
                            if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                        }
                    });
                } else {
                    // Odeća još traje
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fightsRemaining", newFights);
                    equipmentRepository.updateEquipment(user.getId(), e.getId(), updates, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            processed[0]++;
                            if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                        }
                        @Override
                        public void onError(String error) {
                            processed[0]++;
                            if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
                        }
                    });
                }
            } else {
                // Permanentna oprema — ne radi se ništa
                processed[0]++;
                if (processed[0] == total) finalizeEquipmentUpdate(user, ppToRemove[0], callback);
            }
        }
    }

    /**
     * Oduzima privremeni PP od korisnika nakon borbe.
     */
    private void finalizeEquipmentUpdate(User user, int ppToRemove, SimpleCallback callback) {
        if (ppToRemove == 0) {
            callback.onSuccess();
            return;
        }

        int newPP = Math.max(0, user.getPp() - ppToRemove);
        Map<String, Object> updates = new HashMap<>();
        updates.put("pp", newPP);

        userRepository.updateUser(user.getId(), updates, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() { callback.onSuccess(); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    // ===================================================================
    // UNAPREĐENJE ORUŽJA
    // ===================================================================

    public void upgradeWeapon(User user, Equipment weapon, SimpleCallback callback) {
        if (weapon.getType() != EquipmentType.WEAPON) {
            callback.onError("Samo oružje se može unaprediti!");
            return;
        }

        int price = getUpgradePrice(user.getLevel());
        if (user.getCoins() < price) {
            callback.onError("Nemaš dovoljno novčića! Potrebno: " + price);
            return;
        }

        // Uvećaj weapon bonus za 0.01%
        double newBonus = weapon.getWeaponBonus() + 0.0001;

        Map<String, Object> weaponUpdates = new HashMap<>();
        weaponUpdates.put("weaponBonus", newBonus);

        equipmentRepository.updateEquipment(user.getId(), weapon.getId(), weaponUpdates,
                new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("coins", user.getCoins() - price);
                        userRepository.updateUser(user.getId(), userUpdates, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() { callback.onSuccess(); }
                            @Override
                            public void onError(String error) { callback.onError(error); }
                        });
                    }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                });
    }

    // ===================================================================
    // DOBIJANJE OPREME OD BOSA (poziva kolega)
    // ===================================================================

    /**
     * 20% šanse za opremu: 95% odeća, 5% oružje.
     * Ako dobije isto oružje koje već poseduje, dodaje 0.02% na weapon bonus.
     */
    public void grantBossLootEquipment(User user, List<Equipment> existingEquipment,
                                       LootCallback callback) {
        Random random = new Random();

        // 20% šansa za opremu
        if (random.nextInt(100) >= 20) {
            callback.onNoLoot();
            return;
        }

        // 95% odeća, 5% oružje
        boolean isWeapon = random.nextInt(100) < 5;

        if (isWeapon) {
            // Random oružje: mač ili luk
            EquipmentSubtype weaponType = random.nextBoolean()
                    ? EquipmentSubtype.SWORD : EquipmentSubtype.BOW;

            // Proveri da li već poseduje isto oružje
            Equipment existing = findWeaponOfType(existingEquipment, weaponType);
            if (existing != null) {
                // Dodaj 0.02% na postojeće oružje
                double newBonus = existing.getWeaponBonus() + 0.0002;
                Map<String, Object> updates = new HashMap<>();
                updates.put("weaponBonus", newBonus);
                equipmentRepository.updateEquipment(user.getId(), existing.getId(),
                        updates, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() { callback.onLoot(weaponType, true); }
                            @Override
                            public void onError(String error) { callback.onError(error); }
                        });
            } else {
                // Novo oružje
                Equipment newWeapon = new Equipment(user.getId(), EquipmentType.WEAPON, weaponType);
                equipmentRepository.addEquipment(newWeapon, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() { callback.onLoot(weaponType, false); }
                    @Override
                    public void onError(String error) { callback.onError(error); }
                });
            }
        } else {
            // Random odeća
            EquipmentSubtype[] clothingTypes = {
                    EquipmentSubtype.GLOVES,
                    EquipmentSubtype.SHIELD,
                    EquipmentSubtype.BOOTS
            };
            EquipmentSubtype clothingType = clothingTypes[random.nextInt(3)];
            Equipment newClothing = new Equipment(user.getId(), EquipmentType.CLOTHING, clothingType);
            equipmentRepository.addEquipment(newClothing, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess() { callback.onLoot(clothingType, false); }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
        }
    }

    // ===================================================================
    // METODE ZA KOLEGU (Borba sa bosom)
    // ===================================================================

    /**
     * Računa šansu uspešnog napada.
     * baseChance = procenat uspešno rešenih zadataka u etapi (0-100)
     * Shield dodaje +10%
     */
    public double calculateAttackChance(double baseChance, List<Equipment> activeEquipment) {
        double bonus = 0;
        for (Equipment e : activeEquipment) {
            if (e.isActive() && e.getSubtype() == EquipmentSubtype.SHIELD) {
                bonus += 10.0; // +10% po paru štita
            }
        }
        return Math.min(100.0, baseChance + bonus);
    }

    /**
     * Računa ukupan broj napada.
     * Baza = 5, čizme daju 40% šansu za +1 napad po paru čizama.
     */
    public int calculateTotalAttacks(List<Equipment> activeEquipment) {
        int attacks = 5;
        Random random = new Random();
        for (Equipment e : activeEquipment) {
            if (e.isActive() && e.getSubtype() == EquipmentSubtype.BOOTS) {
                if (random.nextInt(100) < 40) {
                    attacks += 1;
                }
            }
        }
        return attacks;
    }

    /**
     * Računa multiplikator novčića.
     * Luk i strela daju +5% trajno (ili više ako je unapređen).
     */
    public double calculateCoinMultiplier(List<Equipment> activeEquipment) {
        double multiplier = 1.0;
        for (Equipment e : activeEquipment) {
            if (e.getSubtype() == EquipmentSubtype.BOW) {
                multiplier += e.getWeaponBonus();
            }
        }
        return multiplier;
    }

    // ===================================================================
    // HELPER METODE
    // ===================================================================

    public EquipmentType getTypeForSubtype(EquipmentSubtype subtype) {
        switch (subtype) {
            case GLOVES:
            case SHIELD:
            case BOOTS:
                return EquipmentType.CLOTHING;
            case SWORD:
            case BOW:
                return EquipmentType.WEAPON;
            default:
                return EquipmentType.POTION;
        }
    }

    private Equipment findWeaponOfType(List<Equipment> equipmentList, EquipmentSubtype subtype) {
        if (equipmentList == null) return null;
        for (Equipment e : equipmentList) {
            if (e.getSubtype() == subtype) return e;
        }
        return null;
    }

    // ===================================================================
    // CALLBACKS
    // ===================================================================

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface ActivationCallback {
        void onSuccess(int newPP, int ppBonus);
        void onError(String error);
    }

    public interface LootCallback {
        void onLoot(EquipmentSubtype received, boolean wasUpgrade);
        void onNoLoot();
        void onError(String error);
    }
}