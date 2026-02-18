package com.example.projekatmobilne.repositories;

import com.example.projekatmobilne.models.FriendRequest;
import com.example.projekatmobilne.models.Friendship;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRepository {

    private final FirebaseFirestore db;

    public FriendRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ============ FRIEND REQUESTS ============

    // Pošalji friend request
    public void sendFriendRequest(FriendRequest request, SendCallback callback) {
        db.collection("friendRequests")
                .add(request.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati primljene zahteve (pending)
    public void getReceivedRequests(String userId, RequestsCallback callback) {
        db.collection("friendRequests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        req.setId(doc.getId());
                        requests.add(req);
                    }
                    callback.onRequestsLoaded(requests);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Dohvati poslate zahteve (pending)
    public void getSentRequests(String userId, RequestsCallback callback) {
        db.collection("friendRequests")
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        req.setId(doc.getId());
                        requests.add(req);
                    }
                    callback.onRequestsLoaded(requests);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Prihvati friend request
    public void acceptFriendRequest(String requestId, String user1Id, String user2Id, AcceptCallback callback) {
        // 1. Ažuriraj status na "accepted"
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");

        db.collection("friendRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 2. Kreiraj Friendship
                    Friendship friendship = new Friendship(user1Id, user2Id);
                    db.collection("friendships")
                            .add(friendship.toMap())
                            .addOnSuccessListener(docRef -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Odbij friend request
    public void declineFriendRequest(String requestId, DeclineCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "declined");

        db.collection("friendRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ FRIENDSHIPS ============

    // Dohvati listu prijatelja
    public void getFriends(String userId, FriendsCallback callback) {
        db.collection("friendships")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> friendIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Friendship friendship = doc.toObject(Friendship.class);

                        // Dodaj ID drugog korisnika (ne sebe)
                        if (friendship.getUser1Id().equals(userId)) {
                            friendIds.add(friendship.getUser2Id());
                        } else if (friendship.getUser2Id().equals(userId)) {
                            friendIds.add(friendship.getUser1Id());
                        }
                    }
                    callback.onFriendsLoaded(friendIds);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Proveri da li su korisnici prijatelji
    public void areFriends(String user1Id, String user2Id, CheckFriendshipCallback callback) {
        db.collection("friendships")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Friendship friendship = doc.toObject(Friendship.class);

                        if ((friendship.getUser1Id().equals(user1Id) && friendship.getUser2Id().equals(user2Id)) ||
                                (friendship.getUser1Id().equals(user2Id) && friendship.getUser2Id().equals(user1Id))) {
                            callback.onResult(true);
                            return;
                        }
                    }
                    callback.onResult(false);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ CALLBACKS ============

    public interface SendCallback {
        void onSuccess(String requestId);
        void onError(String error);
    }

    public interface RequestsCallback {
        void onRequestsLoaded(List<FriendRequest> requests);
        void onError(String error);
    }

    public interface AcceptCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DeclineCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface FriendsCallback {
        void onFriendsLoaded(List<String> friendIds);
        void onError(String error);
    }

    public interface CheckFriendshipCallback {
        void onResult(boolean areFriends);
        void onError(String error);
    }
}