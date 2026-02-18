// AllianceNotificationHelper.java
package com.example.projekatmobilne.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.projekatmobilne.R;

public class AllianceNotificationHelper {

    private static final String CHANNEL_ID = "alliance_invitations";
    public static final String ACTION_ACCEPT = "ACTION_ACCEPT_ALLIANCE";
    public static final String ACTION_DECLINE = "ACTION_DECLINE_ALLIANCE";
    public static final String EXTRA_INVITATION_ID = "invitation_id";
    public static final String EXTRA_ALLIANCE_ID = "alliance_id";
    public static final String EXTRA_ALLIANCE_NAME = "alliance_name";
    public static final String EXTRA_FROM_USER_ID = "from_user_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pozivnice za savez",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifikacije za pozivnice u savez");
            channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public static void showAllianceInvitationNotification(Context context,
                                                          String invitationId,
                                                          String allianceId,
                                                          String allianceName,
                                                          String fromUsername,
                                                          String fromUserId) {
        createNotificationChannel(context);

        int notificationId = invitationId.hashCode();

        // Accept intent
        Intent acceptIntent = new Intent(context, AllianceInvitationReceiver.class);
        acceptIntent.setAction(ACTION_ACCEPT);
        acceptIntent.putExtra(EXTRA_INVITATION_ID, invitationId);
        acceptIntent.putExtra(EXTRA_ALLIANCE_ID, allianceId);
        acceptIntent.putExtra(EXTRA_ALLIANCE_NAME, allianceName);
        acceptIntent.putExtra(EXTRA_FROM_USER_ID, fromUserId);
        acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent acceptPending = PendingIntent.getBroadcast(context, notificationId,
                acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Decline intent
        Intent declineIntent = new Intent(context, AllianceInvitationReceiver.class);
        declineIntent.setAction(ACTION_DECLINE);
        declineIntent.putExtra(EXTRA_INVITATION_ID, invitationId);
        declineIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent declinePending = PendingIntent.getBroadcast(context, notificationId + 1,
                declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Poziv u savez")
                .setContentText(fromUsername + " te poziva u savez '" + allianceName + "'")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)          // Ne može se swipe-ovati
                .setAutoCancel(false)      // Ne nestaje klikom
                .addAction(0, "Prihvati", acceptPending)
                .addAction(0, "Odbij", declinePending);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    public static void dismissNotification(Context context, int notificationId) {
        NotificationManagerCompat.from(context).cancel(notificationId);
    }
}