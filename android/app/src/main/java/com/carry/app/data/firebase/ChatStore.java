package com.carry.app.data.firebase;

import com.carry.app.models.ChatMessage;

import java.util.List;

public interface ChatStore {

    interface MessagesCallback {
        void onMessagesLoaded(List<ChatMessage> messages);
        void onError(String errorMessage);
    }

    interface SendCallback {
        void onSuccess(ChatMessage message);
        void onFailure(String errorMessage);
    }

    interface ParticipantResolutionCallback {
        void onResolved(String firebaseUid, String displayName);
        void onNotFound();
        void onError(String errorMessage);
    }

    interface InitChatCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Resolves the Firebase UID and display name for a backend user ID by querying the users collection.
     */
    void resolveFirebaseUid(Long backendUserId, ParticipantResolutionCallback callback);

    /**
     * Initializes or updates the chat document for an order with participant UIDs.
     */
    void initChat(String requestId, String orderTitle, List<String> participantUids, InitChatCallback callback);

    /**
     * Attaches a real-time listener for the latest 50 messages of an order's chat.
     * Returns a ChatSubscription handle to remove the listener.
     */
    ChatSubscription attachMessagesListener(String requestId, MessagesCallback callback);

    /**
     * Validates (1-500 chars trimmed) and sends a new message to the order's chat subcollection.
     */
    void sendMessage(String requestId, String senderUid, String senderName, String text, SendCallback callback);
}
