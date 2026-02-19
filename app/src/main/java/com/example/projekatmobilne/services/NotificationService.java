package com.example.projekatmobilne.services;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.projekatmobilne.BuildConfig;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationService {

    private static final String ONESIGNAL_APP_ID = "6ba28894-d610-4818-ad77-48326ce71cfd";
    private static final String ONESIGNAL_REST_API_KEY = BuildConfig.ONESIGNAL_REST_API_KEY; // ← DODAJ OVO iz OneSignal dashboard-a
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";

    private final OkHttpClient client;

    public NotificationService() {
        this.client = new OkHttpClient();
    }

    // ============ SEND NOTIFICATIONS ============

    // Pošalji notifikaciju za Friend Request
    public void sendFriendRequestNotification(String targetPlayerId, String fromUsername) {
        String title = "Novi zahtev za prijateljstvo";
        String message = fromUsername + " želi da bude tvoj prijatelj!";
        String data = "{\"type\":\"friend_request\"}";

        sendNotification(targetPlayerId, title, message, data);
    }

    // Pošalji notifikaciju za Friend Accepted
    public void sendFriendAcceptedNotification(String targetPlayerId, String username) {
        String title = "Zahtev prihvaćen";
        String message = username + " je prihvatio/la tvoj zahtev za prijateljstvo!";
        String data = "{\"type\":\"friend_accepted\"}";

        sendNotification(targetPlayerId, title, message, data);
    }

    // Pošalji notifikaciju za Alliance Invitation
    public void sendAllianceInvitationNotification(String targetPlayerId, String fromUsername, String allianceName) {
        String title = "Poziv u savez";
        String message = fromUsername + " te poziva u savez '" + allianceName + "'";
        String data = "{\"type\":\"alliance_invitation\"}";

        sendNotification(targetPlayerId, title, message, data);
    }

    // Pošalji notifikaciju za Member Joined Alliance
    public void sendMemberJoinedNotification(String targetPlayerId, String username, String allianceName) {
        String title = "Novi član";
        String message = username + " se pridružio/la savezu '" + allianceName + "'";
        String data = "{\"type\":\"member_joined\"}";

        sendNotification(targetPlayerId, title, message, data);
    }

    // Pošalji notifikaciju za New Message
    public void sendNewMessageNotification(String targetPlayerId, String senderUsername, String allianceName) {
        String title = "Nova poruka u savezu";
        String message = senderUsername + " je poslao/la poruku u " + allianceName;
        String data = "{\"type\":\"new_message\"}";

        sendNotification(targetPlayerId, title, message, data);
    }

    // ============ GENERIC SEND METHOD ============

    private void sendNotification(String targetPlayerId, String title, String message, String data) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("app_id", ONESIGNAL_APP_ID);

            // Target korisnik (preko Player ID)
            JSONArray playerIds = new JSONArray();
            playerIds.put(targetPlayerId);
            notification.put("include_player_ids", playerIds);

            // Sadržaj notifikacije
            JSONObject headings = new JSONObject();
            headings.put("en", title);
            notification.put("headings", headings);

            JSONObject contents = new JSONObject();
            contents.put("en", message);
            notification.put("contents", contents);

            // Custom data (za routing u app-u)
            notification.put("data", new JSONObject(data));

            // Android notification channel
//            notification.put("android_channel_id", "habit_rpg_channel");
            // HTTP POST request
            RequestBody body = RequestBody.create(
                    notification.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ONESIGNAL_API_URL)
                    .addHeader("Authorization", "Basic " + ONESIGNAL_REST_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("NotificationService", "Failed to send notification: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("NotificationService", "Notification sent successfully");
                    } else {
                        Log.e("NotificationService", "Notification failed: " + response.body().string());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("NotificationService", "JSON Error: " + e.getMessage());
        }
    }
}