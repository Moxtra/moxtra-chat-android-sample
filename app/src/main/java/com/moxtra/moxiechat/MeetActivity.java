package com.moxtra.moxiechat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ActionListener;
import com.moxtra.sdk.common.ApiCallback;
import com.moxtra.sdk.common.EventListener;
import com.moxtra.sdk.common.model.User;
import com.moxtra.sdk.meet.controller.MeetSessionController;
import com.moxtra.sdk.meet.model.Meet;
import com.moxtra.sdk.meet.model.MeetSession;
import com.moxtra.sdk.meet.repo.MeetRepo;

import java.util.ArrayList;
import java.util.List;

public class MeetActivity extends BaseActivity {

    private static final String TAG = "DEMO_MeetActivity";

    private static final String KEY_ACTION = "action";
    private static final String ACTION_JOIN = "join";
    private static final String ACTION_START = "start";
    private static final String ACTION_SHOW = "show";
    private static final String KEY_MEET = "meet";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_USER_LIST = "userList";

    private final Handler mHandler = new Handler();

    private ChatClientDelegate mChatClientDelegate;
    private MeetRepo mMeetRepo;
    private MeetSession mMeetSession;
    private MeetSessionController mMeetSessionController;

    public static void joinMeet(Context ctx, Meet meet) {
        Intent intent = new Intent(ctx, MeetActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_JOIN);
        intent.putExtra(KEY_MEET, meet);
        ctx.startActivity(intent);
    }

    public static void startMeet(Context ctx, String topic, ArrayList<User> userList) {
        Intent intent = new Intent(ctx, MeetActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_START);
        intent.putExtra(KEY_TOPIC, topic);
        intent.putParcelableArrayListExtra(KEY_USER_LIST, userList);
        ctx.startActivity(intent);
    }

    public static void showMeet(Context ctx) {
        Intent intent = new Intent(ctx, MeetActivity.class);
        intent.putExtra(KEY_ACTION, ACTION_SHOW);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meet);
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
        mMeetRepo = mChatClientDelegate.createMeetRepo();

        setLoading(true);
        String action = intent.getStringExtra(KEY_ACTION);
        if (ACTION_JOIN.equals(action)) {
            joinMeet(intent);
        } else if (ACTION_START.equals(action)) {
            startMeet(intent);
        } else if (ACTION_SHOW.equals(action)) {
            mMeetSessionController = mChatClientDelegate.createMeetSessionController(null);
            mMeetSession = mMeetSessionController.getMeetSession();
            showMeetFragment();
        } else {
            Log.e(TAG, "unsupported action: " + action);
            finishInMainThread();
        }
    }

    private void showMeetFragment() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.meet_frame);
                if (fragment == null) {
                    fragment = mMeetSessionController.createMeetFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.meet_frame, fragment).commit();
                }
                mMeetSessionController.setSwitchToNormalViewActionListener(new ActionListener<Void>() {
                    @Override
                    public void onAction(View view, Void aVoid) {
                        MeetActivity.showMeet(view.getContext());
                    }
                });
                mMeetSessionController.setSwitchToFloatingViewActionListener(new ActionListener<Void>() {
                    @Override
                    public void onAction(View view, Void aVoid) {
                        finish();
                    }
                });
                mMeetSession.setOnMeetEndedEventListener(new EventListener<Void>() {
                    @Override
                    public void onEvent(Void aVoid) {
                        finish();
                    }
                });
                setLoading(false);
            }
        });
    }

    private void finishInMainThread() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    private void joinMeet(Intent intent) {
        Meet meet = intent.getParcelableExtra(KEY_MEET);
        if (meet == null) {
            Log.e(TAG, "No chat found");
            finishInMainThread();
            return;
        }

        mMeetRepo.joinMeet(meet.getID(), new ApiCallback<MeetSession>() {
            @Override
            public void onCompleted(MeetSession meetSession) {
                Log.i(TAG, "Join meet successfully.");
                mMeetSession = meetSession;
                mMeetSessionController = mChatClientDelegate.createMeetSessionController(mMeetSession);
                showMeetFragment();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.e(TAG, "Failed to join meet, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                finishInMainThread();
            }
        });

    }

    private void startMeet(Intent intent) {
        String topic = intent.getStringExtra(KEY_TOPIC);
        final List<User> userList = intent.getParcelableArrayListExtra(KEY_USER_LIST);
        mMeetRepo.startMeetWithTopic(topic, new ApiCallback<MeetSession>() {
            @Override
            public void onCompleted(MeetSession meetSession) {
                Log.i(TAG, "Start meet successfully.");
                mMeetSession = meetSession;
                mMeetSession.inviteParticipants(userList, new ApiCallback<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        Log.i(TAG, "Invite participants successfully.");
                    }

                    @Override
                    public void onError(int errorCode, String errorMsg) {
                        Log.i(TAG, "Failed to invite participants, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                    }
                });
                mMeetSessionController = mChatClientDelegate.createMeetSessionController(mMeetSession);
                showMeetFragment();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.e(TAG, "Failed to start meet, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                finishInMainThread();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMeetSession != null) {
            mMeetSession.cleanup();
        }
        if (mMeetSessionController != null) {
            mMeetSessionController.cleanup();
        }
        Log.d(TAG, "onCreate");
    }
}
