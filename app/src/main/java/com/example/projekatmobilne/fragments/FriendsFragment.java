package com.example.projekatmobilne.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.AllianceInvitationAdapter;
import com.example.projekatmobilne.adapters.FriendAdapter;
import com.example.projekatmobilne.adapters.FriendRequestAdapter;
import com.example.projekatmobilne.models.Alliance;
import com.example.projekatmobilne.models.AllianceInvitation;
import com.example.projekatmobilne.models.FriendRequest;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.services.FriendService;
import com.example.projekatmobilne.utils.AllianceNotificationHelper;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    // Services & Utils
    private FriendService friendService;
    private SharedPrefsManager prefsManager;
    private String currentUserId;
    private String currentUsername;

    // Current alliance state
    private Alliance currentAlliance = null;

    // UI - Search
    private EditText etSearch;
    private Button btnSearch, btnScanQr;
    private RecyclerView rvSearchResults;
    private TextView tvSearchEmpty;

    // UI - Friend Requests
    private LinearLayout sectionRequests;
    private RecyclerView rvRequests;
    private TextView tvRequestsEmpty;

    // UI - Alliance Invitations
    private LinearLayout sectionAllianceInvitations;
    private RecyclerView rvAllianceInvitations;

    // UI - Friends
    private RecyclerView rvFriends;
    private TextView tvFriendsEmpty;

    // UI - Alliance
    private LinearLayout sectionNoAlliance, sectionHasAlliance;
    private Button btnCreateAlliance, btnGoToAlliance;
    private TextView tvAllianceName, tvAllianceLeader, tvAllianceMemberCount;
    private Button btnDisbandAlliance, btnLeaveAlliance;
    private Button btnInviteFriends;

    // Adapters
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter requestAdapter;
    private AllianceInvitationAdapter invitationAdapter;
    private FriendAdapter searchAdapter;

    // Search results & friends list (for invite button logic)
    private List<User> friendsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new SharedPrefsManager(requireContext());
        friendService = new FriendService();
        currentUserId = prefsManager.getUserId();
        currentUsername = prefsManager.getUsername();

        initViews(view);
        setupAdapters();
        setupListeners();
        loadData();
    }

    private void initViews(View view) {
        // Search
        etSearch = view.findViewById(R.id.etSearchUser);
        btnSearch = view.findViewById(R.id.btnSearchUser);
        btnScanQr = view.findViewById(R.id.btnScanQr);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);

        // Requests
        sectionRequests = view.findViewById(R.id.sectionFriendRequests);
        rvRequests = view.findViewById(R.id.rvFriendRequests);
        tvRequestsEmpty = view.findViewById(R.id.tvNoRequests);

        // Alliance Invitations
        sectionAllianceInvitations = view.findViewById(R.id.sectionAllianceInvitations);
        rvAllianceInvitations = view.findViewById(R.id.rvAllianceInvitations);

        // Friends
        rvFriends = view.findViewById(R.id.rvFriends);
        tvFriendsEmpty = view.findViewById(R.id.tvNoFriends);

        // Alliance
        sectionNoAlliance = view.findViewById(R.id.sectionNoAlliance);
        sectionHasAlliance = view.findViewById(R.id.sectionHasAlliance);
        btnCreateAlliance = view.findViewById(R.id.btnCreateAlliance);
        btnGoToAlliance = view.findViewById(R.id.btnGoToAlliance);
        tvAllianceName = view.findViewById(R.id.tvAllianceName);
        tvAllianceLeader = view.findViewById(R.id.tvAllianceLeader);
        tvAllianceMemberCount = view.findViewById(R.id.tvAllianceMemberCount);
        btnDisbandAlliance = view.findViewById(R.id.btnDisbandAlliance);
        btnLeaveAlliance = view.findViewById(R.id.btnLeaveAlliance);
        btnInviteFriends = view.findViewById(R.id.btnInviteFriendsToAlliance);
    }

    private void setupAdapters() {
        // Friends list
        friendAdapter = new FriendAdapter(friendsList, new FriendAdapter.FriendListener() {
            @Override
            public void onViewProfile(User user) {
                openPublicProfile(user.getId());
            }

            @Override
            public void onInviteToAlliance(User user) {
                inviteFriendToAlliance(user);
            }
        });
        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(friendAdapter);

        // Search results
        searchAdapter = new FriendAdapter(new ArrayList<>(), new FriendAdapter.FriendListener() {
            @Override
            public void onViewProfile(User user) {
                openPublicProfile(user.getId());
            }

            @Override
            public void onInviteToAlliance(User user) {
                sendFriendRequest(user);
            }
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(searchAdapter);

        // Friend requests
        requestAdapter = new FriendRequestAdapter(new ArrayList<>(), new FriendRequestAdapter.RequestListener() {
            @Override
            public void onAccept(FriendRequest request) {
                acceptFriendRequest(request);
            }

            @Override
            public void onDecline(FriendRequest request) {
                declineFriendRequest(request);
            }
        });
        rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRequests.setAdapter(requestAdapter);

        // Alliance invitations
        invitationAdapter = new AllianceInvitationAdapter(new ArrayList<>(),
                new AllianceInvitationAdapter.InvitationListener() {
                    @Override
                    public void onAccept(AllianceInvitation invitation) {
                        acceptAllianceInvitation(invitation);
                    }

                    @Override
                    public void onDecline(AllianceInvitation invitation) {
                        declineAllianceInvitation(invitation);
                    }
                });
        rvAllianceInvitations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllianceInvitations.setAdapter(invitationAdapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        btnScanQr.setOnClickListener(v -> startQrScanner());

        btnCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        btnGoToAlliance.setOnClickListener(v -> navigateToAlliance());
        btnDisbandAlliance.setOnClickListener(v -> showDisbandDialog());
        btnLeaveAlliance.setOnClickListener(v -> showLeaveAllianceDialog());
        btnInviteFriends.setOnClickListener(v -> showInviteFriendsDialog());
    }

    private void loadData() {
        loadFriends();
        loadFriendRequests();
        loadAllianceInvitations();
        loadAlliance();
    }

    // ============ SEARCH ============

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) return;

        rvSearchResults.setVisibility(View.VISIBLE);
        tvSearchEmpty.setVisibility(View.GONE);

        friendService.searchUsersByUsername(query, new FriendService.UsersCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (!isAdded()) return;
                users.removeIf(u -> u.getId().equals(currentUserId));

                if (users.isEmpty()) {
                    tvSearchEmpty.setVisibility(View.VISIBLE);
                    tvSearchEmpty.setText("Nema korisnika sa tim imenom");
                    searchAdapter.updateFriends(new ArrayList<>());
                } else {
                    tvSearchEmpty.setVisibility(View.GONE);
                    searchAdapter.setShowInviteButton(true);
                    searchAdapter.updateFriends(users);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška pri pretrazi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFriendRequest(User targetUser) {
        friendService.sendFriendRequest(currentUserId, currentUsername, targetUser.getId(),
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Zahtev poslat!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============ QR CODE ============

    private void startQrScanner() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Skeniraj QR kod korisnika");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scannedUserId = result.getContents();
            // QR kod sadrži userId - pošalji zahtev
            if (scannedUserId.equals(currentUserId)) {
                Toast.makeText(requireContext(), "Ne možete dodati sebe", Toast.LENGTH_SHORT).show();
                return;
            }
            friendService.sendFriendRequest(currentUserId, currentUsername, scannedUserId,
                    new FriendService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Zahtev poslat!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ============ FRIEND REQUESTS ============

    private void loadFriendRequests() {
        friendService.getReceivedRequests(currentUserId, new FriendService.RequestsCallback() {
            @Override
            public void onRequestsLoaded(List<FriendRequest> requests) {
                if (!isAdded()) return;
                if (requests.isEmpty()) {
                    sectionRequests.setVisibility(View.GONE);
                } else {
                    sectionRequests.setVisibility(View.VISIBLE);
                    requestAdapter.updateRequests(requests);
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void acceptFriendRequest(FriendRequest request) {
        friendService.acceptFriendRequest(request.getId(), request.getFromUserId(),
                request.getFromUsername(), currentUserId, currentUsername,
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Prijatelj dodat!", Toast.LENGTH_SHORT).show();
                        loadFriendRequests();
                        loadFriends();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineFriendRequest(FriendRequest request) {
        friendService.declineFriendRequest(request.getId(), new FriendService.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                loadFriendRequests();
            }

            @Override
            public void onError(String error) {}
        });
    }

    // ============ FRIENDS ============

    private void loadFriends() {
        friendService.getFriendsWithDetails(currentUserId, new FriendService.UsersCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (!isAdded()) return;
                friendsList.clear();
                friendsList.addAll(users);

                if (users.isEmpty()) {
                    tvFriendsEmpty.setVisibility(View.VISIBLE);
                    rvFriends.setVisibility(View.GONE);
                    return;
                }

                tvFriendsEmpty.setVisibility(View.GONE);
                rvFriends.setVisibility(View.VISIBLE);

                boolean isLeader = currentAlliance != null
                        && currentAlliance.getLeaderId().equals(currentUserId);

                if (!isLeader || currentAlliance == null) {
                    // Nije vođa - sakrij dugme za sve
                    List<Boolean> noButtons = new ArrayList<>();
                    for (User u : users) noButtons.add(false);
                    friendAdapter.updateFriendsWithStatus(users, noButtons);
                    return;
                }

                // Vođa je - prikaži dugme samo za prijatelje koji NISU u savezu
                List<String> memberIds = currentAlliance.getMemberIds();
                List<Boolean> showButtons = new ArrayList<>();
                for (User u : users) {
                    showButtons.add(!memberIds.contains(u.getId()));
                }
                friendAdapter.updateFriendsWithStatus(users, showButtons);
            }

            @Override
            public void onError(String error) {}
        });
    }

    // ============ ALLIANCE INVITATIONS ============

    private void loadAllianceInvitations() {
        friendService.getReceivedAllianceInvitations(currentUserId,
                new FriendService.InvitationsCallback() {
                    @Override
                    public void onInvitationsLoaded(List<AllianceInvitation> invitations) {
                        if (!isAdded()) return;
                        if (invitations.isEmpty()) {
                            sectionAllianceInvitations.setVisibility(View.GONE);
                        } else {
                            sectionAllianceInvitations.setVisibility(View.VISIBLE);
                            invitationAdapter.updateInvitations(invitations);
                        }
                    }

                    @Override
                    public void onError(String error) {}
                });
    }

    private void acceptAllianceInvitation(AllianceInvitation invitation) {
        if (currentAlliance != null) {
            // Korisnik je u savezu - pitaj da li želi da ga napusti
            new AlertDialog.Builder(requireContext())
                    .setTitle("Napusti trenutni savez?")
                    .setMessage("Već si u savezu '" + currentAlliance.getName()
                            + "'. Da li želiš da ga napustiš i pristupiš savezu '"
                            + invitation.getAllianceName() + "'?")
                    .setPositiveButton("Da", (d, w) -> doAcceptInvitation(invitation))
                    .setNegativeButton("Ne", null)
                    .show();
        } else {
            doAcceptInvitation(invitation);
        }
    }

    private void doAcceptInvitation(AllianceInvitation invitation) {
        friendService.acceptAllianceInvitation(
                invitation.getId(), invitation.getAllianceId(),
                currentUserId, invitation.getFromUserId(),
                currentUsername,
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Pridružio si se savezu!", Toast.LENGTH_SHORT).show();
                        loadAllianceInvitations();
                        loadAlliance();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineAllianceInvitation(AllianceInvitation invitation) {
        friendService.declineAllianceInvitation(invitation.getId(), new FriendService.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                loadAllianceInvitations();
            }

            @Override
            public void onError(String error) {}
        });
    }

    // ============ ALLIANCE ============

    private void loadAlliance() {
        friendService.getUserAlliance(currentUserId, new FriendService.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                if (!isAdded()) return;
                currentAlliance = alliance;
                showAllianceSection(alliance);
                // Ažuriraj invite dugmad u listi prijatelja
                boolean isLeader = alliance.getLeaderId().equals(currentUserId);
                friendAdapter.setShowInviteButton(isLeader);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                currentAlliance = null;
                sectionNoAlliance.setVisibility(View.VISIBLE);
                sectionHasAlliance.setVisibility(View.GONE);
                friendAdapter.setShowInviteButton(false);
            }
        });
    }

    private void showAllianceSection(Alliance alliance) {
        sectionNoAlliance.setVisibility(View.GONE);
        sectionHasAlliance.setVisibility(View.VISIBLE);

        tvAllianceName.setText(alliance.getName());
        tvAllianceLeader.setText("Vođa: " + alliance.getLeaderUsername());
        tvAllianceMemberCount.setText("Članova: " + alliance.getMemberIds().size());

        boolean isLeader = alliance.getLeaderId().equals(currentUserId);
        btnDisbandAlliance.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        btnLeaveAlliance.setVisibility(!isLeader ? View.VISIBLE : View.GONE);
        btnInviteFriends.setVisibility(isLeader ? View.VISIBLE : View.GONE);
    }

    private void showCreateAllianceDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_alliance, null);
        EditText etAllianceName = dialogView.findViewById(R.id.etAllianceName);

        new AlertDialog.Builder(requireContext())
                .setTitle("Kreiraj savez")
                .setView(dialogView)
                .setPositiveButton("Kreiraj", (dialog, which) -> {
                    String name = etAllianceName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "Unesite naziv saveza", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createAlliance(name);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void createAlliance(String name) {
        friendService.createAlliance(name, currentUserId, currentUsername,
                new FriendService.AllianceCallback() {
                    @Override
                    public void onAllianceLoaded(Alliance alliance) {
                        if (!isAdded()) return;
                        currentAlliance = alliance;
                        Toast.makeText(requireContext(), "Savez kreiran!", Toast.LENGTH_SHORT).show();
                        showAllianceSection(alliance);
                        friendAdapter.setShowInviteButton(true);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToAlliance() {
        if (currentAlliance == null) return;
        Bundle args = new Bundle();
        args.putString("allianceId", currentAlliance.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_friendsFragment_to_allianceFragment, args);
    }

    private void showInviteFriendsDialog() {
        if (currentAlliance == null || friendsList.isEmpty()) {
            Toast.makeText(requireContext(), "Nemaš prijatelja za pozivanje", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[friendsList.size()];
        for (int i = 0; i < friendsList.size(); i++) {
            names[i] = friendsList.get(i).getUsername();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Pozovi u savez")
                .setItems(names, (dialog, which) -> {
                    User friend = friendsList.get(which);
                    inviteFriendToAlliance(friend);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void inviteFriendToAlliance(User friend) {
        if (currentAlliance == null) {
            Toast.makeText(requireContext(), "Nisi u savezu", Toast.LENGTH_SHORT).show();
            return;
        }
        friendService.inviteFriendToAlliance(
                currentAlliance.getId(), currentAlliance.getName(),
                currentUserId, currentUsername, friend.getId(),
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Pozivnica poslata " + friend.getUsername(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDisbandDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Raspusti savez")
                .setMessage("Da li si siguran da želiš da raspustiš savez? Svi članovi će biti izbačeni.")
                .setPositiveButton("Da", (d, w) -> disbandAlliance())
                .setNegativeButton("Ne", null)
                .show();
    }

    private void disbandAlliance() {
        if (currentAlliance == null) return;
        friendService.disbandAlliance(currentAlliance.getId(), new FriendService.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                currentAlliance = null;
                Toast.makeText(requireContext(), "Savez raspušten", Toast.LENGTH_SHORT).show();
                sectionNoAlliance.setVisibility(View.VISIBLE);
                sectionHasAlliance.setVisibility(View.GONE);
                friendAdapter.setShowInviteButton(false);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLeaveAllianceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Napusti savez")
                .setMessage("Da li si siguran da želiš da napustiš savez?")
                .setPositiveButton("Da", (d, w) -> leaveAlliance())
                .setNegativeButton("Ne", null)
                .show();
    }

    private void leaveAlliance() {
        if (currentAlliance == null) return;
        friendService.leaveAlliance(currentAlliance.getId(), currentUserId,
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        currentAlliance = null;
                        Toast.makeText(requireContext(), "Napustio si savez", Toast.LENGTH_SHORT).show();
                        sectionNoAlliance.setVisibility(View.VISIBLE);
                        sectionHasAlliance.setVisibility(View.GONE);
                        friendAdapter.setShowInviteButton(false);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openPublicProfile(String userId) {
        Bundle args = new Bundle();
        args.putString("userId", userId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_friendsFragment_to_profileFragment, args);
    }
}