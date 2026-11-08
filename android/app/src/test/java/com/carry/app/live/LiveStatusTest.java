package com.carry.app.live;

import com.carry.app.data.firebase.LiveStatusStore;
import com.carry.app.models.LiveLocation;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.RequestStatus;
import com.carry.app.models.StatusUpdate;
import com.carry.app.ui.requests.RequestDetailViewModel;
import com.carry.app.utils.TimeAgoFormatter;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LiveStatusTest {

    // --- Fake LiveStatusStore ---
    private static class FakeLiveStatusStore implements LiveStatusStore {
        OrderEventListener registeredOrderListener;
        Long registeredOrderRequestId;
        boolean orderListenerDetached = false;

        TripLocationListener registeredLocationListener;
        Long registeredTripId;
        boolean tripListenerDetached = false;

        final Map<Long, String> orderEvents = new HashMap<>();
        final Map<Long, LiveLocation> tripLocations = new HashMap<>();

        @Override
        public void emitOrderEvent(Long requestId, String status, String byUid, OperationCallback callback) {
            orderEvents.put(requestId, status);
            if (callback != null) callback.onSuccess();
        }

        @Override
        public void attachOrderEventListener(Long requestId, OrderEventListener listener) {
            this.registeredOrderRequestId = requestId;
            this.registeredOrderListener = listener;
            this.orderListenerDetached = false;
        }

        @Override
        public void detachOrderEventListener(Long requestId) {
            this.orderListenerDetached = true;
            this.registeredOrderListener = null;
        }

        @Override
        public void shareTripLocation(Long tripId, double lat, double lng, String uid, OperationCallback callback) {
            tripLocations.put(tripId, new LiveLocation(lat, lng, System.currentTimeMillis(), uid));
            if (callback != null) callback.onSuccess();
        }

        @Override
        public void stopSharingTripLocation(Long tripId, OperationCallback callback) {
            tripLocations.remove(tripId);
            if (callback != null) callback.onSuccess();
        }

        @Override
        public void attachTripLocationListener(Long tripId, TripLocationListener listener) {
            this.registeredTripId = tripId;
            this.registeredLocationListener = listener;
            this.tripListenerDetached = false;
        }

        @Override
        public void detachTripLocationListener(Long tripId) {
            this.tripListenerDetached = true;
            this.registeredLocationListener = null;
        }
    }

    // --- Fake BackendDataSource ---
    private static class FakeBackendDataSource implements RequestDetailViewModel.BackendDataSource {
        ProductRequest mockRequest;
        List<StatusUpdate> mockTimeline = new ArrayList<>();
        int requestLoadCount = 0;
        int timelineLoadCount = 0;
        boolean shouldFail = false;

        @Override
        public void loadRequest(Long requestId, RequestDetailViewModel.BackendCallback<ProductRequest> callback) {
            requestLoadCount++;
            if (shouldFail) {
                callback.onError("Backend unavailable");
            } else {
                callback.onSuccess(mockRequest);
            }
        }

        @Override
        public void loadTimeline(Long requestId, RequestDetailViewModel.BackendCallback<List<StatusUpdate>> callback) {
            timelineLoadCount++;
            if (shouldFail) {
                callback.onError("Backend unavailable");
            } else {
                callback.onSuccess(mockTimeline);
            }
        }
    }

    private FakeLiveStatusStore fakeLiveStatusStore;
    private FakeBackendDataSource fakeBackendDataSource;
    private RequestDetailViewModel viewModel;
    private static final long FIXED_NOW = 1700000000000L;

    @Before
    public void setUp() {
        fakeLiveStatusStore = new FakeLiveStatusStore();
        fakeBackendDataSource = new FakeBackendDataSource();

        ProductRequest testReq = new ProductRequest();
        testReq.setId(101L);
        testReq.setProductName("KUET Cafeteria Snack");
        testReq.setStatus(RequestStatus.ACCEPTED);
        testReq.setMatchedTripId(55L);
        fakeBackendDataSource.mockRequest = testReq;

        viewModel = new RequestDetailViewModel(
                fakeLiveStatusStore,
                fakeBackendDataSource,
                () -> FIXED_NOW
        );
    }

    // =========================================================================
    // 1. TimeAgoFormatter Tests
    // =========================================================================

    @Test
    public void testTimeAgoFormatter_justNow() {
        long current = FIXED_NOW;
        assertEquals("Just now", TimeAgoFormatter.format(current - 10000L, current)); // 10s ago
        assertEquals("Just now", TimeAgoFormatter.format(current - 59000L, current)); // 59s ago
    }

    @Test
    public void testTimeAgoFormatter_minutes() {
        long current = FIXED_NOW;
        assertEquals("1 min ago", TimeAgoFormatter.format(current - 60000L, current)); // 1 min ago
        assertEquals("15 min ago", TimeAgoFormatter.format(current - (15 * 60000L), current)); // 15 min ago
        assertEquals("59 min ago", TimeAgoFormatter.format(current - (59 * 60000L), current)); // 59 min ago
    }

    @Test
    public void testTimeAgoFormatter_hoursAndDays() {
        long current = FIXED_NOW;
        assertEquals("1 hour ago", TimeAgoFormatter.format(current - (60 * 60000L), current));
        assertEquals("3 hours ago", TimeAgoFormatter.format(current - (3 * 3600000L), current));
        assertEquals("1 day ago", TimeAgoFormatter.format(current - (24 * 3600000L), current));
        assertEquals("5 days ago", TimeAgoFormatter.format(current - (5 * 24 * 3600000L), current));
    }

    @Test
    public void testTimeAgoFormatter_invalidOrFuture() {
        long current = FIXED_NOW;
        assertEquals("Unknown", TimeAgoFormatter.format(0L, current));
        assertEquals("Unknown", TimeAgoFormatter.format(-100L, current));
        assertEquals("Just now", TimeAgoFormatter.format(current + 5000L, current)); // future
    }

    // =========================================================================
    // 2. Nudge Principle: ignore RTDB payload, refresh from authoritative backend
    // =========================================================================

    @Test
    public void testNudgeRefreshesBackend_andIgnoresRtdbPayload() {
        viewModel.start(101L, 55L);

        // Initial fetch happened
        assertEquals(1, fakeBackendDataSource.requestLoadCount);
        assertEquals(1, fakeBackendDataSource.timelineLoadCount);
        assertEquals(RequestStatus.ACCEPTED, viewModel.getCurrentState().request.getStatus());

        // Now backend changes status to COLLECTED
        fakeBackendDataSource.mockRequest.setStatus(RequestStatus.COLLECTED);

        // RTDB emits a nudge with some status payload (even if different or stale)
        fakeLiveStatusStore.registeredOrderListener.onOrderEventReceived(101L);

        // Backend was re-fetched (count incremented to 2)
        assertEquals(2, fakeBackendDataSource.requestLoadCount);
        assertEquals(2, fakeBackendDataSource.timelineLoadCount);

        // State displays authoritative backend data
        assertEquals(RequestStatus.COLLECTED, viewModel.getCurrentState().request.getStatus());
    }

    // =========================================================================
    // 3. Fallback Polling on Listener Error
    // =========================================================================

    @Test
    public void testPollingFallbackStartsWhenListenerErrors() {
        viewModel.start(101L, 55L);
        assertFalse(viewModel.getCurrentState().isFallbackPollingActive);

        // RTDB encounters error
        fakeLiveStatusStore.registeredOrderListener.onError("Realtime updates unavailable: Permission denied");

        // Polling fallback active
        assertTrue(viewModel.getCurrentState().isFallbackPollingActive);
        assertEquals("Realtime updates unavailable: Permission denied", viewModel.getCurrentState().errorMessage);
    }

    // =========================================================================
    // 4. Live Location Sharing & TimeAgo Integration
    // =========================================================================

    @Test
    public void testLocationUpdate_andRemoval() {
        viewModel.start(101L, 55L);
        assertNotNull(fakeLiveStatusStore.registeredLocationListener);

        // Partner shares location 5 minutes before FIXED_NOW
        long locationTime = FIXED_NOW - (5 * 60000L);
        LiveLocation loc = new LiveLocation(22.8995, 89.5023, locationTime, "partner_uid_123");
        fakeLiveStatusStore.registeredLocationListener.onLocationUpdated(loc);

        RequestDetailViewModel.ViewState state = viewModel.getCurrentState();
        assertNotNull(state.liveLocation);
        assertEquals(Double.valueOf(22.8995), state.liveLocation.getLat());
        assertEquals(Double.valueOf(89.5023), state.liveLocation.getLng());
        assertEquals("5 min ago", state.locationTimeAgo);

        // Partner stops sharing
        fakeLiveStatusStore.registeredLocationListener.onLocationRemoved();
        state = viewModel.getCurrentState();
        assertNull(state.liveLocation);
        assertNull(state.locationTimeAgo);
    }

    // =========================================================================
    // 5. Cleanup on Stop
    // =========================================================================

    @Test
    public void testCleanupOnStop() {
        viewModel.start(101L, 55L);
        assertFalse(fakeLiveStatusStore.orderListenerDetached);
        assertFalse(fakeLiveStatusStore.tripListenerDetached);

        viewModel.stop();

        assertTrue(fakeLiveStatusStore.orderListenerDetached);
        assertTrue(fakeLiveStatusStore.tripListenerDetached);
        assertFalse(viewModel.getCurrentState().isFallbackPollingActive);
        assertFalse(viewModel.getCurrentState().isSafetyNetPollingActive);
    }
}
