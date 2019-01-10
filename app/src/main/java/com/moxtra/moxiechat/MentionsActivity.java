package com.moxtra.moxiechat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.chat.controller.MentionsController;
import com.moxtra.sdk.chat.model.FeedData;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ActionListener;

public class MentionsActivity extends BaseActivity {
    private static final String TAG = "DEMO_MentionsActivity";

    private MentionsController mMentionsController;

    private final ActionListener<FeedData> mOnFeedClickListener = new ActionListener<FeedData>() {
        @Override
        public void onAction(View view, FeedData feedData) {
            ChatActivity.showFeed(MentionsActivity.this, feedData.getChat(), feedData.getFeedID());
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentions);

        ChatClientDelegate delegate = ChatClient.getClientDelegate();
        if (delegate == null) {
            Log.e(TAG, "ChatClientDelegate is null");
            finish();
            return;
        }
        mMentionsController = delegate.getMentionsController();
        mMentionsController.setOpenFeedActionListener(mOnFeedClickListener);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.mentions_frame);
        if (fragment == null) {
            fragment = mMentionsController.createMentionsFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.mentions_frame, fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMentionsController != null) {
            mMentionsController.cleanup();
        }
    }
}
