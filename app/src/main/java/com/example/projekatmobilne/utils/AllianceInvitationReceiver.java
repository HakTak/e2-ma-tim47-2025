// AllianceInvitationReceiver.java
package com.example.projekatmobilne.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.projekatmobilne.services.FriendService;
import com.example.projekatmobilne.utils.SharedPrefsManager;

public class AllianceInvitationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String invitationId = intent.getStringExtra(AllianceNotificationHelper.EXTRA_INVITATION_ID);
        int notificationId = intent.getIntExtra(AllianceNotificationHelper.EXTRA_NOTIFICATION_ID, 0);

        FriendService friendService = new FriendService();
        SharedPrefsManager prefs = new SharedPrefsManager(context);

        if (AllianceNotificationHelper.ACTION_ACCEPT.equals(action)) {
            String allianceId = intent.getStringExtra(AllianceNotificationHelper.EXTRA_ALLIANCE_ID);
            String fromUserId = intent.getStringExtra(AllianceNotificationHelper.EXTRA_FROM_USER_ID);
            String currentUserId = prefs.getUserId();
            String currentUsername = prefs.getUsername();

            friendService.acceptAllianceInvitation(invitationId, allianceId,
                    currentUserId, fromUserId, currentUsername,
                    new FriendService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            AllianceNotificationHelper.dismissNotification(context, notificationId);
                            Toast.makeText(context, "Pridružio si se savezu!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            AllianceNotificationHelper.dismissNotification(context, notificationId);
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });

        } else if (AllianceNotificationHelper.ACTION_DECLINE.equals(action)) {
            friendService.declineAllianceInvitation(invitationId, new FriendService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    AllianceNotificationHelper.dismissNotification(context, notificationId);
                }

                @Override
                public void onError(String error) {
                    AllianceNotificationHelper.dismissNotification(context, notificationId);
                }
            });
        }
    }
}