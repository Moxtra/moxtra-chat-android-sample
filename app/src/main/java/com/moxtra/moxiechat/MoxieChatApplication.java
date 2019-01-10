package com.moxtra.moxiechat;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.moxtra.binder.ui.app.ApplicationDelegate;
import com.moxtra.sdk.ChatClient;

public class MoxieChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(BuildConfig.APPLICATION_ID);
        ChatClient.initialize(this);
    }

    private void createNotificationChannel(String channelId) {
        ApplicationDelegate.getInstance().setNotificationChannelID(channelId);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
