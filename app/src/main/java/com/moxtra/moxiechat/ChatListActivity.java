package com.moxtra.moxiechat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.moxtra.moxiechat.model.DummyData;
import com.moxtra.moxiechat.model.MoxieUser;
import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.chat.controller.ChatController;
import com.moxtra.sdk.chat.model.Chat;
import com.moxtra.sdk.chat.model.ChatMember;
import com.moxtra.sdk.chat.repo.ChatRepo;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ApiCallback;
import com.moxtra.sdk.common.BaseRepo;
import com.moxtra.sdk.common.model.MyProfile;
import com.moxtra.sdk.common.model.User;
import com.moxtra.sdk.meet.model.Meet;
import com.moxtra.sdk.meet.repo.MeetRepo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatListActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "DEMO_ChatList";

    private FloatingActionButton mFloatingActionButton;
    private RecyclerView mRecyclerView;
    private ChatListAdapter mAdapter;
    private final ApiCallback<List<Meet>> mMeetListApiCallback = new ApiCallback<List<Meet>>() {
        @Override
        public void onCompleted(List<Meet> meets) {
            Log.d(TAG, "FetchMeets: onCompleted");
            mAdapter.updateMeets(meets);
        }

        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.d(TAG, "FetchMeets: onError");
        }
    };
    private RecyclerView.LayoutManager mLayoutManager;
    private List<MoxieUser> mMoxieUserList;
    private MyProfile mMyProfile;
    private ChatClientDelegate mChatClientDelegate;
    private ChatRepo mChatRepo;
    private MeetRepo mMeetRepo;
    private ChatController mChatController;

    private static boolean isEnded(Meet meet) {
        return !meet.isInProgress() && !(meet.getScheduleStartTime() > 0 && System.currentTimeMillis() < meet.getScheduleEndTime());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_mentions).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.action_todos).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.action_mentions).setVisible(true);
        menu.findItem(R.id.action_todos).setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mentions: {
                startActivity(new Intent(this, MentionsActivity.class));
                break;
            }
            case R.id.action_todos: {
                startActivity(new Intent(this, MyTodosActivity.class));
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        Log.d(TAG, "onCreate!");

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_chat_list);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ChatListAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mMyProfile = ChatClient.getMyProfile();
        mChatClientDelegate = ChatClient.getClientDelegate();
        if (mChatClientDelegate == null) {
            Log.e(TAG, "Unlinked, ChatClient is null.");
            finish();
            return;
        }
        mChatRepo = mChatClientDelegate.createChatRepo();
        mMeetRepo = mChatClientDelegate.createMeetRepo();

        mChatRepo.setOnChangedListener(new BaseRepo.OnRepoChangedListener<Chat>() {
            @Override
            public void onCreated(List<Chat> items) {
                Log.d(TAG, "Chat: onCreated");
                mAdapter.updateChats(mChatRepo.getList());
            }

            @Override
            public void onUpdated(List<Chat> items) {
                Log.d(TAG, "Chat: onUpdated");
                mAdapter.updateChats(mChatRepo.getList());
            }

            @Override
            public void onDeleted(List<Chat> items) {
                Log.d(TAG, "Chat: onDeleted");
                mAdapter.updateChats(mChatRepo.getList());
            }
        });

        mMeetRepo.setOnChangedListener(new BaseRepo.OnRepoChangedListener<Meet>() {
            @Override
            public void onCreated(List<Meet> items) {
                Log.d(TAG, "Meet: onCreated");
                mMeetRepo.fetchMeets(mMeetListApiCallback);
            }

            @Override
            public void onUpdated(List<Meet> items) {
                Log.d(TAG, "Meet: onUpdated");
                mMeetRepo.fetchMeets(mMeetListApiCallback);
            }

            @Override
            public void onDeleted(List<Meet> items) {
                Log.d(TAG, "Meet: onDeleted");
                mMeetRepo.fetchMeets(mMeetListApiCallback);
            }
        });

        setLoading(true);
        mAdapter.updateChats(mChatRepo.getList());
        mMeetRepo.fetchMeets(mMeetListApiCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatController != null) {
            mChatController.cleanup();
            mChatController = null;
        }
        if (mChatRepo != null) {
            mChatRepo.cleanup();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            new MaterialDialog.Builder(this)
                    .title(R.string.selectUserTitle)
                    .items(getUserList())
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            ArrayList<String> uniqueIdList = new ArrayList<>();
                            uniqueIdList.add(mMoxieUserList.get(i).uniqueId);
                            String topic = mMyProfile.getFirstName() + "'s chat";
                            ChatActivity.startGroupChat(ChatListActivity.this, topic, uniqueIdList);
                        }
                    })
                    .show();
        }
    }

    private List<String> getUserList() {
        mMoxieUserList = DummyData.getUserListForSelect(DummyData.findByUniqueId(mMyProfile.getUniqueId()));
        List<String> users = new ArrayList<>(mMoxieUserList.size());
        for (MoxieUser user : mMoxieUserList) {
            users.add(user.firstName + " " + user.lastName);
        }
        return users;
    }

    private class ChatListAdapter extends RecyclerView.Adapter {

        final List<Session> sessionList = new ArrayList<>();

        List<Chat> chatList;
        List<Meet> meetList;

        ChatListAdapter() {
            super();
        }

        private void leaveOrDeleteChat(Chat chat) {
            mChatRepo.deleteOrLeaveChat(chat, new ApiCallback<Void>() {
                @Override
                public void onCompleted(Void result) {
                    Log.i(TAG, "Leave or delete chat successfully.");
                }

                @Override
                public void onError(int errorCode, String errorMsg) {
                    Log.e(TAG, "Failed to leave or delete chat, errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                }
            });
        }

        private void sortData() {
            Collections.sort(sessionList, new Comparator<Session>() {
                @Override
                public int compare(Session lhs, Session rhs) {
                    if (lhs.isMeet) return -1;
                    if (rhs.isMeet) return 1;
                    if (lhs.chat.getLastFeedTimeStamp() > rhs.chat.getLastFeedTimeStamp())
                        return -1;
                    return 0;
                }
            });
        }

        void updateChats(List<Chat> chats) {
            this.chatList = chats;
            refreshData();
        }

        void updateMeets(List<Meet> meets) {
            this.meetList = meets;
            refreshData();
        }

        void refreshData() {
            setLoading(false);
            sessionList.clear();
            if (chatList != null) {
                for (Chat chat : chatList) {
                    sessionList.add(new Session(chat));
                }
            }
            if (meetList != null) {
                for (Meet meet : meetList) {
                    if (!isEnded(meet)) {
                        sessionList.add(new Session(meet));
                    }
                }
            }
            sortData();
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_timeline, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ViewHolder theHolder = (ViewHolder) holder;
            Session session = sessionList.get(position);
            theHolder.session = session;

            if (session.isMeet) {
                final Meet meet = session.meet;
                ((CardView) theHolder.itemView).setCardBackgroundColor(
                        ContextCompat.getColor(ChatListActivity.this, R.color.yellow_100));
                theHolder.tvTopic.setText(meet.getTopic());
                if (!meet.isInProgress()) {
                    theHolder.btnMeet.setVisibility(View.GONE);
                } else {
                    theHolder.btnMeet.setVisibility(View.VISIBLE);
                    theHolder.btnMeet.setText(R.string.Join);
                    theHolder.btnMeet.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MeetActivity.joinMeet(ChatListActivity.this, meet);
                        }
                    });
                }
                theHolder.btnDelete.setVisibility(View.GONE);
                theHolder.tvBadge.setVisibility(View.INVISIBLE);
            } else {
                final Chat chat = session.chat;
                chat.fetchCover(new ApiCallback<String>() {
                    @Override
                    public void onCompleted(final String avatarPath) {
                        Log.d(TAG, " Chat cover=" + avatarPath);
                        if (!TextUtils.isEmpty(avatarPath)) {
                            theHolder.ivCover.setImageURI(Uri.fromFile(new File(avatarPath)));
                        } else {
                            theHolder.ivCover.setImageResource(R.mipmap.ic_launcher);
                        }
                    }

                    @Override
                    public void onError(int errorCode, String errorMsg) {
                    }
                });

                ((CardView) theHolder.itemView).setCardBackgroundColor(
                        ContextCompat.getColor(ChatListActivity.this, R.color.white));
                theHolder.tvTopic.setText(chat.getTopic());
                theHolder.tvLastMessage.setText(chat.getLastFeedContent());
                theHolder.btnMeet.setText(R.string.Meet);
                theHolder.btnMeet.setVisibility(View.VISIBLE);
                theHolder.btnMeet.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ArrayList<User> userList = new ArrayList<>();
                        chat.getChatDetail().getMembers(new ApiCallback<List<ChatMember>>() {
                            @Override
                            public void onCompleted(List<ChatMember> chatMembers) {
                                userList.addAll(chatMembers);
                                String topic = mMyProfile.getFirstName() + "'s " + "meet";
                                MeetActivity.startMeet(ChatListActivity.this, topic, userList);
                            }

                            @Override
                            public void onError(int i, String s) {

                            }
                        });
                    }
                });
                if (mMyProfile.getUniqueId().equals(chat.getOwner().getUniqueId())) {
                    theHolder.btnDelete.setText(R.string.Delete);
                } else {
                    theHolder.btnDelete.setText(R.string.Leave);
                }
                theHolder.btnDelete.setVisibility(View.VISIBLE);
                theHolder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new MaterialDialog.Builder(ChatListActivity.this)
                                .title(R.string.delete_confirm_title)
                                .content(R.string.delete_confirm)
                                .positiveText(android.R.string.yes)
                                .positiveColorRes(R.color.red_800)
                                .negativeColorRes(R.color.black)
                                .negativeText(android.R.string.no)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        leaveOrDeleteChat(chat);
                                        dialog.dismiss();
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                    }
                });
                if (chat.getUnreadFeedCount() > 0) {
                    theHolder.tvBadge.setText(String.valueOf(chat.getUnreadFeedCount()));
                    theHolder.tvBadge.setVisibility(View.VISIBLE);
                } else {
                    theHolder.tvBadge.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return sessionList.size();
        }

        private class Session {
            final boolean isMeet;
            Chat chat;
            Meet meet;

            Session(Chat chat) {
                this.chat = chat;
                this.isMeet = false;
            }

            Session(Meet meet) {
                this.meet = meet;
                this.isMeet = true;
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final ImageView ivCover;
            final TextView tvTopic, tvLastMessage, tvBadge;
            final Button btnDelete, btnMeet;
            final View itemView;
            Session session;

            ViewHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                ivCover = (ImageView) itemView.findViewById(R.id.iv_cover);
                tvTopic = (TextView) itemView.findViewById(R.id.tv_topic);
                tvLastMessage = (TextView) itemView.findViewById(R.id.tv_last_message);
                tvBadge = (TextView) itemView.findViewById(R.id.tv_badge);
                btnDelete = (Button) itemView.findViewById(R.id.btn_delete);
                btnMeet = (Button) itemView.findViewById(R.id.btn_meet);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (session.isMeet) {
                            MeetActivity.joinMeet(ChatListActivity.this, session.meet);
                        } else {
                            ChatActivity.showChat(ChatListActivity.this, session.chat);
                        }
                    }
                });
            }
        }
    }
}
