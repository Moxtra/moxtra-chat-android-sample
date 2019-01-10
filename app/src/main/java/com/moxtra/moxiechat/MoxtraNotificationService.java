package com.moxtra.moxiechat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.moxtra.binder.ui.app.ApplicationDelegate;
import com.moxtra.sdk.notification.BasePushIntentService;
import com.moxtra.sdk.notification.NotificationHelper;

public class MoxtraNotificationService extends BasePushIntentService {
    private static final String TAG = "DEMO_LCIntentService";
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        boolean handled = NotificationHelper.isValidRemoteNotification(intent);
        if (handled) {
            // This is a chat sdk message and it will be handled by chat sdk
            String title = NotificationHelper.getNotificationMessageText(this, intent);
            sendChatSDKNotification(title, null, intent);
        } else {
            // Not a chat sdk message and app should handle it.
            com.moxtra.util.Log.i(TAG, "App should handle it.");
        }
        com.moxtra.util.Log.i(TAG, "Received: " + intent.getExtras().toString());
    }

    private void sendChatSDKNotification(String msg, Uri uri, Intent intent) {
        Intent notificationIntent = new Intent(this, ChatListActivity.class);
        if (intent != null) {
            notificationIntent.putExtras(intent);
        }
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
        NotificationManager nm =
                (android.app.NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
