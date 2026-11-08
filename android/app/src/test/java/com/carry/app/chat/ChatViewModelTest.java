package com.carry.app.chat;

import com.carry.app.data.firebase.ChatStore;
import com.carry.app.data.firebase.ChatSubscription;
import com.carry.app.models.ChatMessage;
import com.carry.app.ui.chat.ChatViewModel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChatViewModelTest {

    private static class FakeChatStore implements ChatStore {
        boolean resolveCalled = false;
        Long lastResolvedBackendId;
        boolean resolveShouldSucceed = true;
        String resolvedFirebaseUid = "firebase_partner_123";
        String resolvedDisplayName = "Partner Name";

        boolean initChatCalled = false;
        List<String> lastInitParticipants;

        MessagesCallback attachedMessagesCallback;
        boolean subscriptionRemoved = false;

        final List<ChatMessage> sentMessages = new ArrayList<>();
        boolean shouldSendSucceed = true;

        @Override
        public void resolveFirebaseUid(Long backendUserId, ParticipantResolutionCallback callback) {
            resolveCalled = true;
            lastResolvedBackendId = backendUserId;
            if (resolveShouldSucceed) {
                callback.onResolved(resolvedFirebaseUid, resolvedDisplayName);
            } else {
                callback.onNotFound();
            }
        }

        @Override
        public void initChat(String requestId, String orderTitle, List<String> participantUids, InitChatCallback callback) {
            initChatCalled = true;
            lastInitParticipants = participantUids;
            if (callback != null) callback.onSuccess();
        }

        @Override
        public ChatSubscription attachMessagesListener(String requestId, MessagesCallback callback) {
            this.attachedMessagesCallback = callback;
            this.subscriptionRemoved = false;
            return () -> subscriptionRemoved = true;
        }

        @Override
        public void sendMessage(String requestId, String senderUid, String senderName, String text, SendCallback callback) {
            if (shouldSendSucceed) {
                ChatMessage msg = new ChatMessage("msg_" + System.currentTimeMillis(), senderUid, senderName, text, new Date());
                sentMessages.add(msg);
                if (callback != null) callback.onSuccess(msg);
            } else {
                if (callback != null) callback.onFailure("Network error");
            }
        }
    }

    private FakeChatStore fakeChatStore;
    private ChatViewModel viewModel;
    private ChatViewModel.ChatState lastObservedState;

    @Before
    public void setUp() {
        fakeChatStore = new FakeChatStore();
        viewModel = new ChatViewModel(fakeChatStore);
        viewModel.setStateListener(state -> lastObservedState = state);
    }

    @Test
    public void testEmptyMessageRejected() {
        boolean sent = viewModel.sendMessage("req_1", "uid_1", "Farhan", "   ");
        assertFalse("Empty trimmed message should be rejected", sent);
        assertNotNull(lastObservedState);
        assertEquals("Message cannot be empty.", lastObservedState.errorMessage);
        assertEquals(0, fakeChatStore.sentMessages.size());
    }

    @Test
    public void testOver500CharsRejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append("A");
        }
        boolean sent = viewModel.sendMessage("req_1", "uid_1", "Farhan", sb.toString());
        assertFalse("Message > 500 characters should be rejected", sent);
        assertNotNull(lastObservedState);
        assertEquals("Message cannot exceed 500 characters.", lastObservedState.errorMessage);
        assertEquals(0, fakeChatStore.sentMessages.size());
    }

    @Test
    public void testSendMessageSuccess() {
        boolean sent = viewModel.sendMessage("req_1", "uid_1", "Farhan", "I'm near Cafeteria!");
        assertTrue("Valid message should be accepted", sent);
        assertNotNull(lastObservedState);
        assertFalse(lastObservedState.isSending);
        assertTrue(lastObservedState.messageSentSuccess);
        assertEquals(1, fakeChatStore.sentMessages.size());
        assertEquals("I'm near Cafeteria!", fakeChatStore.sentMessages.get(0).getText());
    }

    @Test
    public void testMessagesListenerUpdatesState() {
        viewModel.attachMessagesListener("req_100");
        assertTrue(lastObservedState.isLoading);

        List<ChatMessage> incoming = new ArrayList<>();
        incoming.add(new ChatMessage("1", "uid_partner", "Ratul", "Picked up your items!", new Date()));
        incoming.add(new ChatMessage("2", "uid_me", "Farhan", "Great, thanks!", new Date()));

        fakeChatStore.attachedMessagesCallback.onMessagesLoaded(incoming);

        assertFalse(lastObservedState.isLoading);
        assertEquals(2, lastObservedState.messages.size());
        assertEquals("Picked up your items!", lastObservedState.messages.get(0).getText());
    }

    @Test
    public void testListenerCleanupOnDetach() {
        viewModel.attachMessagesListener("req_100");
        assertFalse(fakeChatStore.subscriptionRemoved);

        viewModel.detachMessagesListener();
        assertTrue("Subscription should be removed on detach", fakeChatStore.subscriptionRemoved);
    }

    @Test
    public void testParticipantResolutionAvailable() {
        fakeChatStore.resolveShouldSucceed = true;
        fakeChatStore.resolvedFirebaseUid = "carrier_fb_uid";

        viewModel.initChat("req_50", "Noodles", 12L, "my_fb_uid");

        assertTrue(fakeChatStore.resolveCalled);
        assertEquals(Long.valueOf(12L), fakeChatStore.lastResolvedBackendId);
        assertTrue(lastObservedState.isOtherPartyAvailable);
        assertEquals("carrier_fb_uid", lastObservedState.otherPartyUid);

        assertTrue(fakeChatStore.initChatCalled);
        assertTrue(fakeChatStore.lastInitParticipants.contains("my_fb_uid"));
        assertTrue(fakeChatStore.lastInitParticipants.contains("carrier_fb_uid"));
    }

    @Test
    public void testParticipantResolutionNotFound() {
        fakeChatStore.resolveShouldSucceed = false;

        viewModel.initChat("req_50", "Noodles", 99L, "my_fb_uid");

        assertTrue(fakeChatStore.resolveCalled);
        assertFalse(lastObservedState.isOtherPartyAvailable);

        assertTrue(fakeChatStore.initChatCalled);
        assertEquals(Collections.singletonList("my_fb_uid"), fakeChatStore.lastInitParticipants);
    }
}
