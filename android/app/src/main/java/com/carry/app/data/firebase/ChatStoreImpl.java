package com.carry.app.data.firebase;

import com.carry.app.models.ChatMessage;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatStoreImpl implements ChatStore {

    private static final String COLLECTION_CHATS = "chats";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_USERS = "users";
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MESSAGE_HISTORY_LIMIT = 50;

    private final FirebaseFirestore firestore;

    public ChatStoreImpl() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public ChatStoreImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void resolveFirebaseUid(Long backendUserId, ParticipantResolutionCallback callback) {
        if (backendUserId == null) {
            if (callback != null) callback.onError("Invalid user identifier.");
            return;
        }

        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("backendUserId", backendUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String uid = doc.getString("firebaseUid");
                        if (uid == null || uid.isEmpty()) {
                            uid = doc.getId();
                        }
                        String displayName = doc.getString("displayName");
                        if (callback != null) callback.onResolved(uid, displayName);
                    } else {
                        if (callback != null) callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    @Override
    public void initChat(String requestId, String orderTitle, List<String> participantUids, InitChatCallback callback) {
        if (requestId == null || requestId.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Invalid chat identifier.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        if (orderTitle != null) {
            data.put("orderTitle", orderTitle);
        }
        if (participantUids != null && !participantUids.isEmpty()) {
            data.put("participantUids", participantUids);
        }
        data.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_CHATS)
                .document(requestId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    @Override
    public ChatSubscription attachMessagesListener(String requestId, MessagesCallback callback) {
        if (requestId == null || requestId.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid chat identifier.");
            return () -> {};
        }

        CollectionReference messagesRef = firestore.collection(COLLECTION_CHATS)
                .document(requestId)
                .collection(COLLECTION_MESSAGES);

        Query query = messagesRef.orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(MESSAGE_HISTORY_LIMIT);

        ListenerRegistration registration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                if (callback != null) callback.onError(error.getMessage());
                return;
            }

            if (snapshots != null) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    ChatMessage message = doc.toObject(ChatMessage.class);
                    if (message != null) {
                        message.setId(doc.getId());
                        messages.add(message);
                    }
                }
                if (callback != null) callback.onMessagesLoaded(messages);
            }
        });

        return registration::remove;
    }

    @Override
    public void sendMessage(String requestId, String senderUid, String senderName, String text, SendCallback callback) {
        if (requestId == null || requestId.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Invalid chat identifier.");
            return;
        }
        if (senderUid == null || senderUid.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Sender authentication required.");
            return;
        }

        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) {
            if (callback != null) callback.onFailure("Message cannot be empty.");
            return;
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            if (callback != null) callback.onFailure("Message cannot exceed 500 characters.");
            return;
        }

        DocumentReference messageDoc = firestore.collection(COLLECTION_CHATS)
                .document(requestId)
                .collection(COLLECTION_MESSAGES)
                .document();

        String messageId = messageDoc.getId();
        String safeName = senderName != null && !senderName.trim().isEmpty() ? senderName : "User";

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("senderUid", senderUid);
        messageData.put("senderName", safeName);
        messageData.put("text", trimmed);
        messageData.put("createdAt", FieldValue.serverTimestamp());

        // Update chat document metadata with last message and union current sender UID
        Map<String, Object> chatMetadata = new HashMap<>();
        chatMetadata.put("requestId", requestId);
        chatMetadata.put("lastMessage", trimmed);
        chatMetadata.put("lastMessageTime", FieldValue.serverTimestamp());
        chatMetadata.put("participantUids", FieldValue.arrayUnion(senderUid));

        firestore.collection(COLLECTION_CHATS)
                .document(requestId)
                .set(chatMetadata, SetOptions.merge());

        messageDoc.set(messageData)
                .addOnSuccessListener(aVoid -> {
                    ChatMessage created = new ChatMessage(messageId, senderUid, safeName, trimmed, new Date());
                    if (callback != null) callback.onSuccess(created);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
