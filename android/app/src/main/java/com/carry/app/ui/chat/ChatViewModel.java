package com.carry.app.ui.chat;

import com.carry.app.data.firebase.ChatStore;
import com.carry.app.data.firebase.ChatSubscription;
import com.carry.app.models.ChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatViewModel {

    public static class ChatState {
        public List<ChatMessage> messages = new ArrayList<>();
        public boolean isLoading = true;
        public boolean isSending = false;
        public boolean isOtherPartyAvailable = true;
        public String otherPartyUid;
        public String otherPartyDisplayName;
        public String errorMessage;
        public boolean messageSentSuccess = false;

        public ChatState copy() {
            ChatState state = new ChatState();
            state.messages = new ArrayList<>(this.messages);
            state.isLoading = this.isLoading;
            state.isSending = this.isSending;
            state.isOtherPartyAvailable = this.isOtherPartyAvailable;
            state.otherPartyUid = this.otherPartyUid;
            state.otherPartyDisplayName = this.otherPartyDisplayName;
            state.errorMessage = this.errorMessage;
            state.messageSentSuccess = this.messageSentSuccess;
            return state;
        }
    }

    public interface StateListener {
        void onStateChanged(ChatState state);
    }

    private final ChatStore chatStore;
    private ChatSubscription chatSubscription;
    private ChatState currentState = new ChatState();
    private StateListener stateListener;

    public ChatViewModel(ChatStore chatStore) {
        this.chatStore = chatStore;
    }

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
        if (listener != null) {
            listener.onStateChanged(currentState.copy());
        }
    }

    public void removeStateListener() {
        this.stateListener = null;
    }

    public ChatState getCurrentState() {
        return currentState.copy();
    }

    private void notifyStateChanged() {
        if (stateListener != null) {
            stateListener.onStateChanged(currentState.copy());
        }
    }

    public void initChat(String requestId, String orderTitle, Long otherPartyBackendId, String currentFirebaseUid) {
        if (otherPartyBackendId != null && otherPartyBackendId > 0) {
            chatStore.resolveFirebaseUid(otherPartyBackendId, new ChatStore.ParticipantResolutionCallback() {
                @Override
                public void onResolved(String firebaseUid, String displayName) {
                    currentState.isOtherPartyAvailable = true;
                    currentState.otherPartyUid = firebaseUid;
                    currentState.otherPartyDisplayName = displayName;
                    notifyStateChanged();

                    List<String> participants = new ArrayList<>();
                    if (currentFirebaseUid != null) participants.add(currentFirebaseUid);
                    if (firebaseUid != null) participants.add(firebaseUid);
                    chatStore.initChat(requestId, orderTitle, participants, null);
                }

                @Override
                public void onNotFound() {
                    currentState.isOtherPartyAvailable = false;
                    notifyStateChanged();

                    if (currentFirebaseUid != null) {
                        chatStore.initChat(requestId, orderTitle, Collections.singletonList(currentFirebaseUid), null);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Non-fatal: other party check failed (e.g. offline)
                    currentState.isOtherPartyAvailable = false;
                    notifyStateChanged();
                }
            });
        } else {
            if (currentFirebaseUid != null) {
                chatStore.initChat(requestId, orderTitle, Collections.singletonList(currentFirebaseUid), null);
            }
        }

        attachMessagesListener(requestId);
    }

    public void attachMessagesListener(String requestId) {
        detachMessagesListener();
        currentState.isLoading = true;
        notifyStateChanged();

        chatSubscription = chatStore.attachMessagesListener(requestId, new ChatStore.MessagesCallback() {
            @Override
            public void onMessagesLoaded(List<ChatMessage> messages) {
                currentState.isLoading = false;
                currentState.messages = messages != null ? messages : new ArrayList<>();
                currentState.errorMessage = null;
                notifyStateChanged();
            }

            @Override
            public void onError(String errorMessage) {
                currentState.isLoading = false;
                currentState.errorMessage = errorMessage;
                notifyStateChanged();
            }
        });
    }

    public void detachMessagesListener() {
        if (chatSubscription != null) {
            chatSubscription.remove();
            chatSubscription = null;
        }
    }

    public boolean sendMessage(String requestId, String senderUid, String senderName, String text) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) {
            currentState.errorMessage = "Message cannot be empty.";
            notifyStateChanged();
            return false;
        }

        if (trimmed.length() > 500) {
            currentState.errorMessage = "Message cannot exceed 500 characters.";
            notifyStateChanged();
            return false;
        }

        currentState.isSending = true;
        currentState.errorMessage = null;
        currentState.messageSentSuccess = false;
        notifyStateChanged();

        chatStore.sendMessage(requestId, senderUid, senderName, trimmed, new ChatStore.SendCallback() {
            @Override
            public void onSuccess(ChatMessage message) {
                currentState.isSending = false;
                currentState.messageSentSuccess = true;
                notifyStateChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                currentState.isSending = false;
                currentState.errorMessage = errorMessage;
                notifyStateChanged();
            }
        });

        return true;
    }
}
