package com.moxtra.moxiechat;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.notification.NotificationManager;

public class FcmInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = FcmInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "onTokenRefresh, token = " + token);
        if (!TextUtils.isEmpty(token)) {
            ChatClientDelegate ccd = ChatClient.getClientDelegate();
            if (ccd == null) {
                return;
            }
            NotificationManager nm = ccd.getNotificationManager();
            if (nm == null) {
                return;
            }
            nm.setNotificationDeviceToken(token);
        }
    }
}
