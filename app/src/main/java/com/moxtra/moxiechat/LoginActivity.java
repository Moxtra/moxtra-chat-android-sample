package com.moxtra.moxiechat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.moxtra.moxiechat.common.PreferenceUtil;
import com.moxtra.moxiechat.model.DummyData;
import com.moxtra.moxiechat.model.MoxieUser;
import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ApiCallback;
import com.moxtra.sdk.notification.NotificationManager;

import java.util.List;

/**
 * A login screen that offers login via unique ID.
 */
public class LoginActivity extends Activity {
    private static final String TAG = "DEMO_LoginActivity";

    /**
     * Here provides a sample to link with Moxtra account with accessToken via Intent.
     */
    private static final String KEY_TOKEN = "token";

    private static final String CLIENT_ID = "irej6RlLOBo";
    private static final String CLIENT_SECRET = "uiwE8ZzymRs";
    private static final String ORG_ID = null;
    private static final String BASE_DOMAIN = "sandbox.moxtra.com";
    private static final int REQUEST_CODE_GOOGLE_SERVICE = 1024;

    private final Handler mHandler = new Handler();
    // UI references.
    private Context mContext;
    private AutoCompleteTextView mUniqueIdView;
    private View mProgressView;
    private View mLoginFormView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ChatClient.getMyProfile() != null) {
            // Already linked to Moxtra account
            startChatListActivity();
            return;
        }

        ChatClient.setupDomain(BASE_DOMAIN, null, null, null);

        mContext = getApplicationContext();
        mProgressView = findViewById(R.id.login_progress);
        mUniqueIdView = (AutoCompleteTextView) findViewById(R.id.unique_id);
        mUniqueIdView.setText(DummyData.UNIQUE_IDS.get(0));
        addUniqueIdsToAutoComplete(DummyData.UNIQUE_IDS);

        Button mUniqueIdSignInButton = (Button) findViewById(R.id.unique_id_sign_in_button);
        mUniqueIdSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        mLoginFormView = findViewById(R.id.login_form);

        Intent intent = getIntent();
        if (intent != null) {
            String token = intent.getStringExtra(KEY_TOKEN);
            Log.d(TAG, "Token = " + token);
            if (token != null) {
                linkWithAccessToken(token);
                return;
            }
        }

        MoxieUser user = PreferenceUtil.getUser(this);
        if (user != null) {
            linkWithUniqueID(user.uniqueId);
        } else {
            showSelectionDialog();
        }

        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int status = availability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Log.i(TAG, "Google play service is unavailable, status = " + status);
            if (availability.isUserResolvableError(status)) {
                availability.getErrorDialog(this, status, REQUEST_CODE_GOOGLE_SERVICE).show();
            }
        }
    }

    private void startChatListActivity() {
        Log.i(TAG, "startChatListActivity");
        startActivity(new Intent(this, ChatListActivity.class));
        finish();
    }

    private void linkWithUniqueID(final String uniqueId) {
        Log.d(TAG, "Start to linkWithUniqueID...");
        showProgress(true);
        ChatClient.linkWithUniqueId(uniqueId, CLIENT_ID, CLIENT_SECRET, ORG_ID, new ApiCallback<ChatClientDelegate>() {
            @Override
            public void onCompleted(ChatClientDelegate ccd) {
                Log.i(TAG, "Linked to Moxtra account successfully.");
                PreferenceUtil.saveUser(mContext, uniqueId);
                initNotification();
                startChatListActivity();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Toast.makeText(mContext, "Failed to link to Moxtra account.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to link to Moxtra account, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                showProgress(false);
            }
        });
    }

    private void linkWithAccessToken(final String token) {
        Log.d(TAG, "Start to linkWithAccessToken...");
        showProgress(true);
        ChatClient.linkWithAccessToken(token, new ApiCallback<ChatClientDelegate>() {
            @Override
            public void onCompleted(ChatClientDelegate ccd) {
                Log.i(TAG, "Linked to Moxtra account successfully.");
                initNotification();
                startChatListActivity();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Toast.makeText(mContext, "Failed to link to Moxtra account.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to link to Moxtra account, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                showProgress(false);
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUniqueIdView.setError(null);
        String uniqueId = mUniqueIdView.getText().toString();

        // Check for a valid unique ID.
        if (TextUtils.isEmpty(uniqueId)) {
            mUniqueIdView.setError(getString(R.string.error_field_required));
            mUniqueIdView.requestFocus();
        } else {
            linkWithUniqueID(uniqueId);
        }
    }

    private void showSelectionDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.selectUserTitle)
                .items(DummyData.UNIQUE_IDS)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        MoxieUser moxieUser = DummyData.USERS.get(i);
                        mUniqueIdView.setText(moxieUser.uniqueId);
                    }
                })
                .show();
    }

    private void addUniqueIdsToAutoComplete(List<String> uniqueIdCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(mContext,
                        android.R.layout.simple_dropdown_item_1line, uniqueIdCollection);

        mUniqueIdView.setAdapter(adapter);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                mProgressView.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
    }

    private void initNotification() {
        ChatClientDelegate ccd = ChatClient.getClientDelegate();
        if (ccd == null) {
            return;
        }
        NotificationManager nm = ccd.getNotificationManager();
        if (nm == null) {
            return;
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Log.i(TAG, "use build-in push notification");
            nm.useBuiltInPushNotification(MoxtraNotificationService.class.getName());
        } else {
            Log.i(TAG, "use fcm notification");
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "fcm token = " + token);
            if (!TextUtils.isEmpty(token)) {
                nm.setNotificationDeviceToken(token);
            }
        }
    }

}

