package com.moxtra.moxiechat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.moxtra.sdk.ChatClient;
import com.moxtra.sdk.chat.controller.MyTodosController;
import com.moxtra.sdk.chat.model.Todo;
import com.moxtra.sdk.client.ChatClientDelegate;
import com.moxtra.sdk.common.ActionListener;

public class MyTodosActivity extends BaseActivity {
    private static final String TAG = "DEMO_MyTodoActivity";

    private MyTodosController mMyTodosController;

    private final ActionListener<Todo> mOnTodoClickListener = new ActionListener<Todo>() {
        @Override
        public void onAction(View view, Todo todo) {
            ChatActivity.showChat(MyTodosActivity.this, todo.getChat());
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mytodos);

        ChatClientDelegate delegate = ChatClient.getClientDelegate();
        if (delegate == null) {
            Log.e(TAG, "ChatClientDelegate is null");
            finish();
            return;
        }
        mMyTodosController = delegate.getMyTodosController();
        mMyTodosController.setOpenTodoActionListener(mOnTodoClickListener);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.mytodos_frame);
        if (fragment == null) {
            fragment = mMyTodosController.createMyTodosFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.mytodos_frame, fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyTodosController != null) {
            mMyTodosController.cleanup();
        }
    }
}
