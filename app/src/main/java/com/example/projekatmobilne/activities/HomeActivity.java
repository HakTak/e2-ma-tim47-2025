package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.models.Boss;
import com.example.projekatmobilne.services.UserService;
import com.example.projekatmobilne.utils.AllianceNotificationHelper;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.example.projekatmobilne.viewModels.BossViewModel;
import com.example.projekatmobilne.viewModels.UserViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NavController navController;
    private SharedPrefsManager prefsManager;
    private UserService userService;
    private ListenerRegistration allianceInvitationListener;
    private ListenerRegistration messageNotificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initServices();
        initToolbar();
        initDrawer();
        initNavigation();
        initBackPressHandler();
    }

    private void initServices() {
        prefsManager = new SharedPrefsManager(this);
        userService = new UserService();

        String userId = prefsManager.getUserId();
        if (userId != null && !userId.isEmpty()) {
            userService.checkDailyActivity(userId);
            startAllianceInvitationListener(userId);
            syncOneSignalPlayerId(userId);
            startMessageNotificationListener(userId);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initNavigation() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            handleDrawerItemSelected(item.getItemId());
            return true;
        });

        handleNavigationIntent(getIntent());
        //Korisniku se uvek ucitava event prelaska na novi nivo
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        BossViewModel bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);

        userViewModel.levelUpEvent.observe(this, event -> {
            if (event != null) {

                userViewModel.levelUpEvent.setValue(null);
            }
        });
    }

    private void initBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void handleDrawerItemSelected(int id) {
        if (id == R.id.nav_logout) {
            showLogoutDialog();

        } else if (id == R.id.nav_tasks) {
            startActivity(new Intent(this, TasksActivity.class));

        } else if (id == R.id.nav_boss_fight) {
            startActivity(new Intent(this, BossFightActivity.class));

        } else {
            navController.navigate(id);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Toast.makeText(this, "Podešavanja - dolazi uskoro", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.drawer_logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        prefsManager.clearSession();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null) return;
        String navigateTo = intent.getStringExtra("navigateTo");
        if ("equipment".equals(navigateTo)) {
            boolean fromBossFight = intent.getBooleanExtra("fromBossFight", false);
            Bundle args = new Bundle();
            args.putBoolean("fromBossFight", fromBossFight);
            navController.navigate(R.id.nav_equipment, args);
        }
    }
    private void startAllianceInvitationListener(String userId) {
        final boolean[] isFirstLoad = {true}; // preskoči inicijalni snapshot

        allianceInvitationListener = FirebaseFirestore.getInstance()
                .collection("allianceInvitations")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    if (isFirstLoad[0]) {
                        isFirstLoad[0] = false;
                        return; // ne prikazuj notifikaciju za već postojeće pozivnice
                    }

                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            com.example.projekatmobilne.models.AllianceInvitation inv =
                                    dc.getDocument().toObject(
                                            com.example.projekatmobilne.models.AllianceInvitation.class);
                            inv.setId(dc.getDocument().getId());
                            AllianceNotificationHelper.showAllianceInvitationNotification(
                                    this,
                                    inv.getId(),
                                    inv.getAllianceId(),
                                    inv.getAllianceName(),
                                    inv.getFromUsername(),
                                    inv.getFromUserId()
                            );
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (allianceInvitationListener != null) allianceInvitationListener.remove();
        if (messageNotificationListener != null) messageNotificationListener.remove();
    }
    private void syncOneSignalPlayerId(String userId) {
        String playerId = com.onesignal.OneSignal.getUser().getOnesignalId();

        if (playerId == null || playerId.isEmpty()) {
            // Ako još nije spreman, postavi observer
            com.onesignal.OneSignal.getUser().addObserver(state -> {
                String id = state.getCurrent().getOnesignalId();
                if (id != null && !id.isEmpty()) {
                    savePlayerIdToFirestore(userId, id);
                }
            });
        } else {
            savePlayerIdToFirestore(userId, playerId);
        }
    }

    private void savePlayerIdToFirestore(String userId, String playerId) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("oneSignalPlayerId", playerId);

        new com.example.projekatmobilne.repositories.UserRepository()
                .updateUser(userId, updates,
                        new com.example.projekatmobilne.database.FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                android.util.Log.d("OneSignal", "Player ID sačuvan: " + playerId);
                            }
                            @Override
                            public void onError(String error) {
                                android.util.Log.e("OneSignal", "Greška: " + error);
                            }
                        });
    }
    private void startMessageNotificationListener(String userId) {
        // Prvo dohvati savez korisnika
        FirebaseFirestore.getInstance()
                .collection("alliances")
                .whereArrayContains("memberIds", userId)
                .limit(1)
                .addSnapshotListener((allianceSnap, e) -> {
                    if (e != null || allianceSnap == null || allianceSnap.isEmpty()) return;

                    String allianceId = allianceSnap.getDocuments().get(0).getId();

                    if (messageNotificationListener != null) {
                        messageNotificationListener.remove();
                    }

                    final boolean[] firstLoad = {true};

                    messageNotificationListener = FirebaseFirestore.getInstance()
                            .collection("allianceMessages")
                            .whereEqualTo("allianceId", allianceId)
                            .orderBy("timestamp")
                            .addSnapshotListener((msgSnap, err) -> {
                                if (err != null || msgSnap == null) return;

                                if (firstLoad[0]) {
                                    firstLoad[0] = false;
                                    return;
                                }

                                for (com.google.firebase.firestore.DocumentChange dc : msgSnap.getDocumentChanges()) {
                                    if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                        String senderId = dc.getDocument().getString("senderId");
                                        if (senderId != null && senderId.equals(userId)) continue; // ne notifikuj samog sebe

                                        String senderUsername = dc.getDocument().getString("senderUsername");
                                        String messageText = dc.getDocument().getString("message");

                                        showMessageNotification(senderUsername, messageText);
                                    }
                                }
                            });
                });
    }

    private void showMessageNotification(String senderUsername, String messageText) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "alliance_messages", "Poruke saveza",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            nm.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, "alliance_messages")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Nova poruka u savezu")
                        .setContentText(senderUsername + ": " + messageText)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        androidx.core.app.NotificationManagerCompat manager =
                androidx.core.app.NotificationManagerCompat.from(this);

        // Provjeri dozvolu za Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                manager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } else {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}