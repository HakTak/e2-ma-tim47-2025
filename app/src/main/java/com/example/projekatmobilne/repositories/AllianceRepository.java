package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.models.Alliance;
import com.example.projekatmobilne.models.AllianceInvitation;
import com.example.projekatmobilne.models.AllianceMessage;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceRepository {

    private final FirebaseFirestore db;

    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ============ ALLIANCE CRUD ============

    // Kreiraj novi savez
    public void createAlliance(Alliance alliance, CreateCallback callback) {
        db.collection("alliances")
                .add(alliance.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati savez po ID
    public void getAllianceById(String allianceId, AllianceCallback callback) {
        db.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Alliance alliance = snapshot.toObject(Alliance.class);
                        if (alliance != null) {
                            alliance.setId(snapshot.getId());
                            callback.onAllianceLoaded(alliance);
                        } else {
                            callback.onError("Alliance not found");
                        }
                    } else {
                        callback.onError("Alliance does not exist");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati savez korisnika
    public void getUserAlliance(String userId, AllianceCallback callback) {
        db.collection("alliances")
                .whereArrayContains("memberIds", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Alliance alliance = doc.toObject(Alliance.class);
                        if (alliance != null) {
                            alliance.setId(doc.getId());
                            callback.onAllianceLoaded(alliance);
                        } else {
                            callback.onError("No alliance found");
                        }
                    } else {
                        callback.onError("No alliance found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dodaj člana u savez
    public void addMemberToAlliance(String allianceId, String userId, UpdateCallback callback) {
        db.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Alliance alliance = snapshot.toObject(Alliance.class);
                    if (alliance != null) {
                        List<String> members = alliance.getMemberIds();
                        if (!members.contains(userId)) {
                            members.add(userId);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("memberIds", members);

                            db.collection("alliances").document(allianceId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        } else {
                            callback.onError("User is already a member");
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Ukloni člana iz saveza
    public void removeMemberFromAlliance(String allianceId, String userId, UpdateCallback callback) {
        db.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Alliance alliance = snapshot.toObject(Alliance.class);
                    if (alliance != null) {
                        List<String> members = alliance.getMemberIds();
                        members.remove(userId);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("memberIds", members);

                        db.collection("alliances").document(allianceId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Obriši savez
    public void deleteAlliance(String allianceId, DeleteCallback callback) {
        db.collection("alliances").document(allianceId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Ažuriraj savez
    public void updateAlliance(String allianceId, Map<String, Object> updates, UpdateCallback callback) {
        db.collection("alliances").document(allianceId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ ALLIANCE INVITATIONS ============

    // Pošalji pozivnicu za savez
    public void sendAllianceInvitation(AllianceInvitation invitation, SendInviteCallback callback) {
        db.collection("allianceInvitations")
                .add(invitation.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati primljene pozivnice (pending)
    public void getReceivedInvitations(String userId, InvitationsCallback callback) {
        db.collection("allianceInvitations")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AllianceInvitation> invitations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AllianceInvitation inv = doc.toObject(AllianceInvitation.class);
                        inv.setId(doc.getId());
                        invitations.add(inv);
                    }
                    callback.onInvitationsLoaded(invitations);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Prihvati pozivnicu za savez
    public void acceptAllianceInvitation(String invitationId, String allianceId, String userId, AcceptInviteCallback callback) {
        // 1. Ažuriraj status pozivnice na "accepted"
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");

        db.collection("allianceInvitations").document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 2. Dodaj korisnika u savez
                    addMemberToAlliance(allianceId, userId, new UpdateCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Odbij pozivnicu za savez
    public void declineAllianceInvitation(String invitationId, DeclineInviteCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "declined");

        db.collection("allianceInvitations").document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ ALLIANCE MESSAGES ============

    // Pošalji poruku
    public void sendMessage(AllianceMessage message, SendMessageCallback callback) {
        db.collection("allianceMessages")
                .add(message.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati poruke (sa real-time listenerom)
    public ListenerRegistration listenToMessages(String allianceId, MessagesCallback callback) {
        return db.collection("allianceMessages")
                .whereEqualTo("allianceId", allianceId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        List<AllianceMessage> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            AllianceMessage msg = doc.toObject(AllianceMessage.class);
                            msg.setId(doc.getId());
                            messages.add(msg);
                        }
                        callback.onMessagesLoaded(messages);
                    }
                });
    }

    // ============ CALLBACKS ============

    public interface CreateCallback {
        void onSuccess(String allianceId);
        void onError(String error);
    }

    public interface AllianceCallback {
        void onAllianceLoaded(Alliance alliance);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface SendInviteCallback {
        void onSuccess(String invitationId);
        void onError(String error);
    }

    public interface InvitationsCallback {
        void onInvitationsLoaded(List<AllianceInvitation> invitations);
        void onError(String error);
    }

    public interface AcceptInviteCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DeclineInviteCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface SendMessageCallback {
        void onSuccess(String messageId);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onMessagesLoaded(List<AllianceMessage> messages);
        void onError(String error);
    }
}