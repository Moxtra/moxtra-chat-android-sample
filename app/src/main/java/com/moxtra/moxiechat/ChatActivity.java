package com.moxtra.moxiechat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.chat.controller.ChatController;
import com.moxtra.sdk.chat.model.Chat;
import com.moxtra.sdk.chat.repo.ChatRepo;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ApiCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseActivity {

    private static final String KEY_CHAT = "chat";
    private static final String KEY_ACTION = "action";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_UNIQUE_ID_LIST = "uniqueIdList";
    private static final String KEY_FEED_ID = "feedId";

    private static final String ACTION_SHOW = "show";
    private static final String ACTION_START = "start";

    private static final String TAG = "DEMO_ChatActivity";

    private final Handler mHandler = new Handler();
    private ChatClientDelegate mChatClientDelegate;
    private ChatController mChatController;
    private ChatRepo mChatRepo;

    private Chat mChat;

    public static void showChat(Context ctx, Chat chat) {
        Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_SHOW);
        intent.putExtra(KEY_CHAT, chat);
        ctx.startActivity(intent);
    }

    public static void showFeed(Context ctx, Chat chat, String feedId) {
        Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_SHOW);
        intent.putExtra(KEY_CHAT, chat);
        intent.putExtra(KEY_FEED_ID, feedId);
        ctx.startActivity(intent);
    }

    public static void startGroupChat(Context ctx, String topic, ArrayList<String> uniqueIdList) {
        Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_START);
        intent.putExtra(KEY_TOPIC, topic);
        intent.putStringArrayListExtra(KEY_UNIQUE_ID_LIST, uniqueIdList);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "no intent received");
            finishInMainThread();
            return;
        }
        mChatClientDelegate = ChatClient.getClientDelegate();
        if (mChatClientDelegate == null) {
            Log.e(TAG, "ChatClient is null");
            finishInMainThread();
            return;
        }
        String action = intent.getStringExtra(KEY_ACTION);
        setLoading(true);
        if (ACTION_SHOW.equals(action)) {
            showChat(intent);
        } else if (ACTION_START.equals(action)) {
            startChat(intent);
        } else {
            finishInMainThread();
        }
    }

    private void startChat(Intent intent) {
        String topic = intent.getStringExtra(KEY_TOPIC);
        final List<String> uniqueIdList = intent.getStringArrayListExtra(KEY_UNIQUE_ID_LIST);
        final String orgId = null;
        mChatRepo = mChatClientDelegate.createChatRepo();
        mChatRepo.createGroupChat(topic, new ApiCallback<Chat>() {
            @Override
            public void onCompleted(Chat chat) {
                Log.i(TAG, "Create group chat successfully.");
                mChat = chat;
                mChat.getChatDetail().inviteMembers(orgId, uniqueIdList, new ApiCallback<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        Log.i(TAG, "Invite members successfully.");
                    }

                    @Override
                    public void onError(int errorCode, String errorMsg) {
                        Log.e(TAG, "Failed to invite members, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                    }
                });
                showChatFragment(null);
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.e(TAG, "Failed to create group chat, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                finishInMainThread();
            }
        });
    }

    private void showChat(Intent intent) {
        mChat = intent.getParcelableExtra(KEY_CHAT);
        if (mChat == null) {
            Log.e(TAG, "No chat found");
            finishInMainThread();
            return;
        }
        showChatFragment(intent.getStringExtra(KEY_FEED_ID));
    }

    private void finishInMainThread() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    private void showChatFragment(final String feedId) {
        Log.d(TAG, "showChatFragment() called with: feedId = [" + feedId + "]");
        mChatController = mChatClientDelegate.createChatController(mChat);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.chat_frame);
                if (fragment == null) {
                    fragment = mChatController.createChatFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.chat_frame, fragment).commit();
                }
                if (feedId != null) {
                    mChatController.scrollToFeed(feedId);
                }
                setLoading(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatController != null) {
            mChatController.cleanup();
        }
        Log.d(TAG, "onCreate");
    }
}
