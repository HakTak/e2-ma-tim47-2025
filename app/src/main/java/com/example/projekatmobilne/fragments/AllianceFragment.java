package com.example.projekatmobilne.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.adapters.MessageAdapter;
import com.example.projekatmobilne.models.Alliance;
import com.example.projekatmobilne.models.AllianceMessage;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.AllianceRepository;
import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.services.FriendService;
import com.example.projekatmobilne.utils.SharedPrefsManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AllianceFragment extends Fragment {

    private FriendService friendService;
    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private SharedPrefsManager prefsManager;

    private String currentUserId;
    private String currentUsername;
    private String allianceId;
    private Alliance currentAlliance;

    // UI
    private TextView tvAllianceName, tvAllianceLeader, tvMembers;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvNoMessages;

    // Adapter & listener
    private MessageAdapter messageAdapter;
    private ListenerRegistration messageListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new SharedPrefsManager(requireContext());
        friendService = new FriendService();
        allianceRepository = new AllianceRepository();
        userRepository = new UserRepository();

        currentUserId = prefsManager.getUserId();
        currentUsername = prefsManager.getUsername();

        // Uzmi allianceId iz Bundle argumenata
        if (getArguments() != null) {
            allianceId = getArguments().getString("allianceId");
        }

        if (allianceId == null) {
            Toast.makeText(requireContext(), "Greška: ID saveza nije pronađen", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        initViews(view);
        setupAdapter();
        loadAllianceInfo();
    }

    private void initViews(View view) {
        tvAllianceName = view.findViewById(R.id.tvAllianceDetailName);
        tvAllianceLeader = view.findViewById(R.id.tvAllianceDetailLeader);
        tvMembers = view.findViewById(R.id.tvAllianceMembers);
        rvMessages = view.findViewById(R.id.rvAllianceMessages);
        etMessage = view.findViewById(R.id.etAllianceMessage);
        btnSend = view.findViewById(R.id.btnSendMessage);
        tvNoMessages = view.findViewById(R.id.tvNoMessages);
    }

    private void setupAdapter() {
        messageAdapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Nove poruke na dnu
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadAllianceInfo() {
        allianceRepository.getAllianceById(allianceId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                if (!isAdded()) return;
                currentAlliance = alliance;
                tvAllianceName.setText(alliance.getName());
                tvAllianceLeader.setText("Vođa: " + alliance.getLeaderUsername());
                loadMemberNames(alliance.getMemberIds());
                startListeningToMessages();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMemberNames(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            tvMembers.setText("Članovi: /");
            return;
        }

        List<String> names = new ArrayList<>();
        int[] count = {0};

        for (String memberId : memberIds) {
            userRepository.getUser(memberId, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    names.add(user.getUsername());
                    count[0]++;
                    if (count[0] == memberIds.size()) {
                        if (isAdded()) {
                            tvMembers.setText("Članovi: " + TextUtils.join(", ", names));
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    count[0]++;
                    if (count[0] == memberIds.size()) {
                        if (isAdded()) {
                            tvMembers.setText("Članovi: " + TextUtils.join(", ", names));
                        }
                    }
                }
            });
        }
    }

    private void startListeningToMessages() {
        messageListener = friendService.listenToMessages(allianceId,
                new FriendService.MessagesCallback() {
                    @Override
                    public void onMessagesLoaded(List<AllianceMessage> messages) {
                        if (!isAdded()) return;
                        if (messages.isEmpty()) {
                            tvNoMessages.setVisibility(View.VISIBLE);
                            rvMessages.setVisibility(View.GONE);
                        } else {
                            tvNoMessages.setVisibility(View.GONE);
                            rvMessages.setVisibility(View.VISIBLE);
                            messageAdapter.updateMessages(messages);
                            // Skroluj na poslednju poruku
                            rvMessages.post(() -> rvMessages.scrollToPosition(messages.size() - 1));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Greška poruka: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage() {
        if (currentAlliance == null) return;

        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessage.setEnabled(false);
        btnSend.setEnabled(false);

        friendService.sendMessage(allianceId, currentUserId, currentUsername, text,
                currentAlliance.getName(), currentAlliance.getMemberIds(),
                new FriendService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        etMessage.setText("");
                        etMessage.setEnabled(true);
                        btnSend.setEnabled(true);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        etMessage.setEnabled(true);
                        btnSend.setEnabled(true);
                        Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Zaustavi real-time listener
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }
}