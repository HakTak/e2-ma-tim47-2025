package com.example.projekatmobilne.services;

import android.util.Log;

import com.example.projekatmobilne.models.Alliance;
import com.example.projekatmobilne.models.AllianceInvitation;
import com.example.projekatmobilne.models.AllianceMessage;
import com.example.projekatmobilne.models.FriendRequest;
import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.AllianceRepository;
import com.example.projekatmobilne.repositories.FriendRepository;
import com.example.projekatmobilne.repositories.UserRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendService {

    private final FriendRepository friendRepository;
    private final AllianceRepository allianceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FirebaseFirestore db;

    public FriendService() {
        this.friendRepository = new FriendRepository();
        this.allianceRepository = new AllianceRepository();
        this.userRepository = new UserRepository();
        this.notificationService = new NotificationService();
        this.db = FirebaseFirestore.getInstance();
    }

    // ============ PRETRAGA KORISNIKA ============

    public void searchUsersByUsername(String query, UsersCallback callback) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = new User();
                        user.setId(doc.getString("id"));
                        user.setUsername(doc.getString("username"));
                        user.setAvatar(doc.getString("avatar"));
                        Long level = doc.getLong("level");
                        user.setLevel(level != null ? level.intValue() : 0);
                        user.setTitle(doc.getString("title"));
                        users.add(user);
                    }
                    callback.onUsersLoaded(users);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============ PRIJATELJI ============

    public void sendFriendRequest(String fromUserId, String fromUsername, String toUserId, SimpleCallback callback) {
        // Provjeri da li zahtev već postoji
        db.collection("friendRequests")
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onError("Zahtev već poslat");
                        return;
                    }

                    // Provjeri da li su već prijatelji
                    friendRepository.areFriends(fromUserId, toUserId, new FriendRepository.CheckFriendshipCallback() {
                        @Override
                        public void onResult(boolean areFriends) {
                            if (areFriends) {
                                callback.onError("Već ste prijatelji");
                                return;
                            }

                            FriendRequest request = new FriendRequest(fromUserId, fromUsername, toUserId);
                            friendRepository.sendFriendRequest(request, new FriendRepository.SendCallback() {
                                @Override
                                public void onSuccess(String requestId) {
                                    // Pošalji notifikaciju
                                    getUserPlayerId(toUserId, playerId -> {
                                        if (playerId != null) {
                                            notificationService.sendFriendRequestNotification(playerId, fromUsername);
                                        }
                                    });
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
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void acceptFriendRequest(String requestId, String fromUserId, String fromUsername,
                                    String currentUserId, String currentUsername, SimpleCallback callback) {
        friendRepository.acceptFriendRequest(requestId, fromUserId, currentUserId, new FriendRepository.AcceptCallback() {
            @Override
            public void onSuccess() {
                // Notifikacija pošiljaocu
                getUserPlayerId(fromUserId, playerId -> {
                    if (playerId != null) {
                        notificationService.sendFriendAcceptedNotification(playerId, currentUsername);
                    }
                });
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void declineFriendRequest(String requestId, SimpleCallback callback) {
        friendRepository.declineFriendRequest(requestId, new FriendRepository.DeclineCallback() {
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

    public void getFriendsWithDetails(String userId, UsersCallback callback) {
        friendRepository.getFriends(userId, new FriendRepository.FriendsCallback() {
            @Override
            public void onFriendsLoaded(List<String> friendIds) {
                if (friendIds.isEmpty()) {
                    callback.onUsersLoaded(new ArrayList<>());
                    return;
                }
                List<User> friends = new ArrayList<>();
                int[] count = {0};
                for (String friendId : friendIds) {
                    userRepository.getUser(friendId, new UserRepository.UserCallback() {
                        @Override
                        public void onUserLoaded(User user) {
                            friends.add(user);
                            count[0]++;
                            if (count[0] == friendIds.size()) {
                                callback.onUsersLoaded(friends);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            count[0]++;
                            if (count[0] == friendIds.size()) {
                                callback.onUsersLoaded(friends);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getReceivedRequests(String userId, RequestsCallback callback) {
        friendRepository.getReceivedRequests(userId, new FriendRepository.RequestsCallback() {
            @Override
            public void onRequestsLoaded(List<FriendRequest> requests) {
                callback.onRequestsLoaded(requests);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ============ SAVEZ ============

    public void createAlliance(String name, String leaderId, String leaderUsername, AllianceCallback callback) {
        // Provjeri da li je već u savezu
        allianceRepository.getUserAlliance(leaderId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                callback.onError("Već ste u savezu: " + alliance.getName());
            }

            @Override
            public void onError(String error) {
                // Nije u savezu - može kreirati
                Alliance alliance = new Alliance(name, leaderId, leaderUsername);
                allianceRepository.createAlliance(alliance, new AllianceRepository.CreateCallback() {
                    @Override
                    public void onSuccess(String allianceId) {
                        alliance.setId(allianceId);
                        callback.onAllianceLoaded(alliance);
                    }

                    @Override
                    public void onError(String error2) {
                        callback.onError(error2);
                    }
                });
            }
        });
    }

    public void getUserAlliance(String userId, AllianceCallback callback) {
        allianceRepository.getUserAlliance(userId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                callback.onAllianceLoaded(alliance);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void inviteFriendToAlliance(String allianceId, String allianceName,
                                       String fromUserId, String fromUsername,
                                       String toUserId, SimpleCallback callback) {
        // Provjeri da li je toUser već u savezu
        allianceRepository.getUserAlliance(toUserId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance existingAlliance) {
                // Korisnik je u savezu - pošalji poziv, ali u UI ćemo prikazati upozorenje
                sendInvitation(allianceId, allianceName, fromUserId, fromUsername, toUserId, callback);
            }

            @Override
            public void onError(String error) {
                // Korisnik nije u savezu
                sendInvitation(allianceId, allianceName, fromUserId, fromUsername, toUserId, callback);
            }
        });
    }

    private void sendInvitation(String allianceId, String allianceName,
                                String fromUserId, String fromUsername,
                                String toUserId, SimpleCallback callback) {
        // Provjeri da li već postoji pending pozivnica
        db.collection("allianceInvitations")
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        callback.onError("Pozivnica već poslata");
                        return;
                    }
                    AllianceInvitation inv = new AllianceInvitation(allianceId, allianceName,
                            fromUserId, fromUsername, toUserId);
                    allianceRepository.sendAllianceInvitation(inv, new AllianceRepository.SendInviteCallback() {
                        @Override
                        public void onSuccess(String invitationId) {
                            getUserPlayerId(toUserId, playerId -> {
                                if (playerId != null) {
                                    notificationService.sendAllianceInvitationNotification(
                                            playerId, fromUsername, allianceName);
                                }
                            });
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

    public void acceptAllianceInvitation(String invitationId, String allianceId,
                                         String userId, String leaderId,
                                         String username, SimpleCallback callback) {
        // Ako je korisnik u drugom savezu (a misija nije aktivna), napusti ga
        allianceRepository.getUserAlliance(userId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance currentAlliance) {
                if (currentAlliance.isHasMission()) {
                    callback.onError("Ne možete napustiti savez dok je misija aktivna");
                    return;
                }
                // Napusti stari savez
                if (currentAlliance.getLeaderId().equals(userId)) {
                    // Vođa ne može napustiti, ali ovo ne bi trebalo da se desi jer vođa dobija pozivnicu
                    callback.onError("Vođa ne može napustiti savez");
                    return;
                }
                leaveAndJoin(currentAlliance.getId(), invitationId, allianceId, userId, leaderId, username, callback);
            }

            @Override
            public void onError(String error) {
                // Nije u savezu - direktno prihvati
                doAcceptInvitation(invitationId, allianceId, userId, leaderId, username, callback);
            }
        });
    }

    private void leaveAndJoin(String oldAllianceId, String invitationId, String allianceId,
                              String userId, String leaderId, String username, SimpleCallback callback) {
        allianceRepository.removeMemberFromAlliance(oldAllianceId, userId, new AllianceRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                doAcceptInvitation(invitationId, allianceId, userId, leaderId, username, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void doAcceptInvitation(String invitationId, String allianceId, String userId,
                                    String leaderId, String username, SimpleCallback callback) {
        allianceRepository.acceptAllianceInvitation(invitationId, allianceId, userId,
                new AllianceRepository.AcceptInviteCallback() {
                    @Override
                    public void onSuccess() {
                        // Notifikacija vođi
                        getUserPlayerId(leaderId, playerId -> {
                            if (playerId != null) {
                                allianceRepository.getAllianceById(allianceId, new AllianceRepository.AllianceCallback() {
                                    @Override
                                    public void onAllianceLoaded(Alliance alliance) {
                                        notificationService.sendMemberJoinedNotification(
                                                playerId, username, alliance.getName());
                                    }

                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                        });
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    public void declineAllianceInvitation(String invitationId, SimpleCallback callback) {
        allianceRepository.declineAllianceInvitation(invitationId, new AllianceRepository.DeclineInviteCallback() {
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

    public void disbandAlliance(String allianceId, SimpleCallback callback) {
        allianceRepository.getAllianceById(allianceId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                if (alliance.isHasMission()) {
                    callback.onError("Ne možete raskinuti savez dok je misija aktivna");
                    return;
                }
                allianceRepository.deleteAlliance(allianceId, new AllianceRepository.DeleteCallback() {
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

    public void leaveAlliance(String allianceId, String userId, SimpleCallback callback) {
        allianceRepository.getAllianceById(allianceId, new AllianceRepository.AllianceCallback() {
            @Override
            public void onAllianceLoaded(Alliance alliance) {
                if (alliance.isHasMission()) {
                    callback.onError("Ne možete napustiti savez dok je misija aktivna");
                    return;
                }
                allianceRepository.removeMemberFromAlliance(allianceId, userId,
                        new AllianceRepository.UpdateCallback() {
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

    // ============ PORUKE ============

    public void sendMessage(String allianceId, String senderId, String senderUsername,
                            String messageText, String allianceName,
                            List<String> memberIds, SimpleCallback callback) {
        AllianceMessage message = new AllianceMessage(allianceId, senderId, senderUsername, messageText);
        allianceRepository.sendMessage(message, new AllianceRepository.SendMessageCallback() {
            @Override
            public void onSuccess(String messageId) {
                // Notifikacija svim ostalim članovima
                for (String memberId : memberIds) {
                    if (!memberId.equals(senderId)) {
                        getUserPlayerId(memberId, playerId -> {
                            if (playerId != null) {
                                notificationService.sendNewMessageNotification(
                                        playerId, senderUsername, allianceName);
                            }
                        });
                    }
                }
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public ListenerRegistration listenToMessages(String allianceId, MessagesCallback callback) {
        return allianceRepository.listenToMessages(allianceId, new AllianceRepository.MessagesCallback() {
            @Override
            public void onMessagesLoaded(List<AllianceMessage> messages) {
                callback.onMessagesLoaded(messages);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getReceivedAllianceInvitations(String userId, InvitationsCallback callback) {
        allianceRepository.getReceivedInvitations(userId, new AllianceRepository.InvitationsCallback() {
            @Override
            public void onInvitationsLoaded(List<AllianceInvitation> invitations) {
                callback.onInvitationsLoaded(invitations);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // ============ HELPER ============

    private void getUserPlayerId(String userId, PlayerIdCallback callback) {
        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                callback.onPlayerId(user.getOneSignalPlayerId());
            }

            @Override
            public void onError(String error) {
                callback.onPlayerId(null);
                Log.e("FriendService", "Error getting player ID: " + error);
            }
        });
    }
    public void checkFriendshipStatus(String currentUserId, String targetUserId, FriendStatusCallback callback) {
        // Provjeri da li su prijatelji
        friendRepository.areFriends(currentUserId, targetUserId, new FriendRepository.CheckFriendshipCallback() {
            @Override
            public void onResult(boolean areFriends) {
                if (areFriends) {
                    callback.onStatus(FriendStatus.FRIENDS);
                    return;
                }
                // Provjeri pending request (poslat)
                db.collection("friendRequests")
                        .whereEqualTo("fromUserId", currentUserId)
                        .whereEqualTo("toUserId", targetUserId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener(snap1 -> {
                            if (!snap1.isEmpty()) {
                                callback.onStatus(FriendStatus.REQUEST_SENT);
                                return;
                            }
                            // Provjeri pending request (primljen)
                            db.collection("friendRequests")
                                    .whereEqualTo("fromUserId", targetUserId)
                                    .whereEqualTo("toUserId", currentUserId)
                                    .whereEqualTo("status", "pending")
                                    .get()
                                    .addOnSuccessListener(snap2 -> {
                                        if (!snap2.isEmpty()) {
                                            callback.onStatus(FriendStatus.REQUEST_RECEIVED);
                                        } else {
                                            callback.onStatus(FriendStatus.NONE);
                                        }
                                    })
                                    .addOnFailureListener(e -> callback.onStatus(FriendStatus.NONE));
                        })
                        .addOnFailureListener(e -> callback.onStatus(FriendStatus.NONE));
            }

            @Override
            public void onError(String error) {
                callback.onStatus(FriendStatus.NONE);
            }
        });
    }

    public enum FriendStatus {
        NONE, FRIENDS, REQUEST_SENT, REQUEST_RECEIVED
    }

    public interface FriendStatusCallback {
        void onStatus(FriendStatus status);
    }

    // ============ CALLBACKS ============

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UsersCallback {
        void onUsersLoaded(List<User> users);
        void onError(String error);
    }

    public interface RequestsCallback {
        void onRequestsLoaded(List<FriendRequest> requests);
        void onError(String error);
    }

    public interface AllianceCallback {
        void onAllianceLoaded(Alliance alliance);
        void onError(String error);
    }

    public interface InvitationsCallback {
        void onInvitationsLoaded(List<AllianceInvitation> invitations);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onMessagesLoaded(List<AllianceMessage> messages);
        void onError(String error);
    }

    private interface PlayerIdCallback {
        void onPlayerId(String playerId);
    }
}