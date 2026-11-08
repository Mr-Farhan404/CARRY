package com.carry.app.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.data.firebase.ChatStoreImpl;
import com.carry.app.models.ChatMessage;
import com.carry.app.models.User;
import com.carry.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_ORDER_NAME = "extra_order_name";
    public static final String EXTRA_OTHER_PARTY_NAME = "extra_other_party_name";
    public static final String EXTRA_OTHER_PARTY_BACKEND_ID = "extra_other_party_backend_id";

    private static final String KEY_DRAFT_TEXT = "key_draft_text";

    private String requestId;
    private String orderName;
    private String otherPartyName;
    private Long otherPartyBackendId;

    private MaterialToolbar chatToolbar;
    private LinearLayout layoutFallbackBanner;
    private TextView tvFallbackNotice;
    private RecyclerView rvChatMessages;
    private ProgressBar progressBarChat;
    private TextView tvEmptyChat;
    private EditText etChatMessage;
    private FloatingActionButton btnSendMessage;

    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private String currentFirebaseUid;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (requestId == null) {
            long reqLong = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1L);
            if (reqLong > 0) {
                requestId = String.valueOf(reqLong);
            }
        }
        orderName = getIntent().getStringExtra(EXTRA_ORDER_NAME);
        otherPartyName = getIntent().getStringExtra(EXTRA_OTHER_PARTY_NAME);
        if (getIntent().hasExtra(EXTRA_OTHER_PARTY_BACKEND_ID)) {
            otherPartyBackendId = getIntent().getLongExtra(EXTRA_OTHER_PARTY_BACKEND_ID, -1L);
            if (otherPartyBackendId <= 0) otherPartyBackendId = null;
        }

        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(this, "Order identifier missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SessionManager session = SessionManager.getInstance(this);
        currentFirebaseUid = session.getFirebaseUid();
        User currentUser = session.getUser();
        currentUserName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "User";

        initViews();
        setupToolbar();
        setupRecyclerView();

        viewModel = new ChatViewModel(new ChatStoreImpl());
        viewModel.initChat(requestId, orderName, otherPartyBackendId, currentFirebaseUid);

        btnSendMessage.setOnClickListener(v -> handleSendMessage());

        if (savedInstanceState != null) {
            String draft = savedInstanceState.getString(KEY_DRAFT_TEXT);
            if (draft != null) {
                etChatMessage.setText(draft);
                etChatMessage.setSelection(draft.length());
            }
        }
    }

    private void initViews() {
        chatToolbar = findViewById(R.id.chatToolbar);
        layoutFallbackBanner = findViewById(R.id.layoutFallbackBanner);
        tvFallbackNotice = findViewById(R.id.tvFallbackNotice);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        progressBarChat = findViewById(R.id.progressBarChat);
        tvEmptyChat = findViewById(R.id.tvEmptyChat);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
    }

    private void setupToolbar() {
        String title = otherPartyName != null && !otherPartyName.isEmpty() ? otherPartyName : "Order Chat";
        chatToolbar.setTitle(title);
        if (orderName != null && !orderName.isEmpty()) {
            chatToolbar.setSubtitle("Order: " + orderName);
        } else {
            chatToolbar.setSubtitle("Order #" + requestId);
        }

        chatToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(currentFirebaseUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.setStateListener(this::renderState);
            viewModel.attachMessagesListener(requestId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (viewModel != null) {
            viewModel.detachMessagesListener();
            viewModel.removeStateListener();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etChatMessage != null) {
            outState.putString(KEY_DRAFT_TEXT, etChatMessage.getText().toString());
        }
    }

    private void handleSendMessage() {
        String text = etChatMessage.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }

        boolean submitted = viewModel.sendMessage(requestId, currentFirebaseUid, currentUserName, text);
        if (submitted) {
            etChatMessage.setText("");
        }
    }

    private void renderState(ChatViewModel.ChatState state) {
        runOnUiThread(() -> {
            progressBarChat.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            List<ChatMessage> messages = state.messages;
            boolean hasMessages = messages != null && !messages.isEmpty();
            tvEmptyChat.setVisibility(!state.isLoading && !hasMessages ? View.VISIBLE : View.GONE);

            if (adapter != null) {
                adapter.submitList(messages, () -> {
                    if (hasMessages && rvChatMessages != null) {
                        rvChatMessages.scrollToPosition(messages.size() - 1);
                    }
                });
            }

            if (!state.isOtherPartyAvailable) {
                layoutFallbackBanner.setVisibility(View.VISIBLE);
                String name = otherPartyName != null ? otherPartyName : "The other party";
                tvFallbackNotice.setText(name + " hasn't joined live chat yet. Messages will be waiting when they connect.");
            } else {
                layoutFallbackBanner.setVisibility(View.GONE);
            }

            btnSendMessage.setEnabled(!state.isSending);

            if (state.errorMessage != null && !state.errorMessage.isEmpty()) {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
