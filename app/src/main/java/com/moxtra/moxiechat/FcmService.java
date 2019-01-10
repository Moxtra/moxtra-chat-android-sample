package com.moxtra.moxiechat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.moxtra.binder.ui.app.ApplicationDelegate;
import com.moxtra.sdk.notification.NotificationHelper;
import com.moxtra.util.Log;

import java.util.Map;

public class FcmService extends FirebaseMessagingService {

    private static final String TAG = FcmService.class.getSimpleName();
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived...");
        Intent intent = new Intent();
        Map<String, String> data = remoteMessage.getData();
        for (String key : data.keySet()) {
            intent.putExtra(key, data.get(key));
        }
        boolean handled = NotificationHelper.isValidRemoteNotification(intent);
        if (handled) {
            // This is a chat sdk message and it will be handled by chat sdk
            String title = NotificationHelper.getNotificationMessageText(this, intent);
            sendChatSDKNotification(title, null, intent);
        } else {
            // Not a chat sdk message and app should handle it.
            Log.i(TAG, "App should handle it.");
        }
        Log.i(TAG, "Received: " + intent.getExtras().toString());
    }

    private void sendChatSDKNotification(String msg, Uri uri, Intent intent) {
        Log.d(TAG, "Got notification: msg = " + msg + ", uri = " + uri);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, ChatListActivity.class);
        if (intent != null) {
            notificationIntent.putExtras(intent);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, ApplicationDelegate.getInstance().getNotificationChannelID())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(getApplicationInfo().labelRes))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);

        if (uri != null) {
            builder.setSound(uri);
        }

        builder.setContentIntent(contentIntent);
        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
