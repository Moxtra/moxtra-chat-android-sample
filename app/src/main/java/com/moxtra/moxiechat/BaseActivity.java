package com.moxtra.moxiechat;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.moxtra.moxiechat.common.PreferenceUtil;
import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.common.ApiCallback;
import com.moxtra.sdk.common.ContextWrapper;

import java.io.IOException;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "DEMO_BaseActivity";
    private MenuItem mActionProgressItem;
    private boolean mIsLoading = false;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mActionProgressItem = menu.findItem(R.id.miActionProgress);
        setLoading(mIsLoading);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ContextWrapper.wrap(newBase, this));
    }

    protected void setLoading(boolean isLoading) {
        mIsLoading = isLoading;
        if (mActionProgressItem != null) {
            mActionProgressItem.setVisible(mIsLoading);
        }
    }

    private void logout() {
        PreferenceUtil.removeUser(this);
        setLoading(true);
        ChatClient.unlink(new ApiCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                Log.i(TAG, "Unlink Moxtra account successfully.");
                setLoading(false);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.e(TAG, "Failed to unlink Moxtra account, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                setLoading(false);
            }
        });

        FirebaseMessaging.getInstance().setAutoInitEnabled(false);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
}
