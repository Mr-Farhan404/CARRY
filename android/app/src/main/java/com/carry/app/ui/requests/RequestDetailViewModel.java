package com.carry.app.ui.requests;

import com.carry.app.data.firebase.LiveStatusStore;
import com.carry.app.models.LiveLocation;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.StatusUpdate;
import com.carry.app.utils.TimeAgoFormatter;

import java.util.List;

public class RequestDetailViewModel {

    public interface BackendDataSource {
        void loadRequest(Long requestId, BackendCallback<ProductRequest> callback);
        void loadTimeline(Long requestId, BackendCallback<List<StatusUpdate>> callback);
    }

    public interface BackendCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public interface TimeProvider {
        long currentTimeMillis();
    }

    public static class ViewState {
        public ProductRequest request;
        public List<StatusUpdate> timeline;
        public LiveLocation liveLocation;
        public String locationTimeAgo;
        public boolean isFallbackPollingActive;
        public boolean isSafetyNetPollingActive;
        public int backendRefreshCount;
        public String errorMessage;

        public ViewState copy() {
            ViewState copy = new ViewState();
            copy.request = this.request;
            copy.timeline = this.timeline;
            copy.liveLocation = this.liveLocation;
            copy.locationTimeAgo = this.locationTimeAgo;
            copy.isFallbackPollingActive = this.isFallbackPollingActive;
            copy.isSafetyNetPollingActive = this.isSafetyNetPollingActive;
            copy.backendRefreshCount = this.backendRefreshCount;
            copy.errorMessage = this.errorMessage;
            return copy;
        }
    }

    public interface StateListener {
        void onStateChanged(ViewState state);
    }

    private final LiveStatusStore liveStatusStore;
    private final BackendDataSource backendDataSource;
    private final TimeProvider timeProvider;

    private Long currentRequestId;
    private Long currentTripId;
    private ViewState currentState = new ViewState();
    private StateListener stateListener;

    public RequestDetailViewModel(LiveStatusStore liveStatusStore, BackendDataSource backendDataSource) {
        this(liveStatusStore, backendDataSource, System::currentTimeMillis);
    }

    public RequestDetailViewModel(LiveStatusStore liveStatusStore, BackendDataSource backendDataSource, TimeProvider timeProvider) {
        this.liveStatusStore = liveStatusStore;
        this.backendDataSource = backendDataSource;
        this.timeProvider = timeProvider;
    }

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
        if (listener != null) {
            listener.onStateChanged(currentState.copy());
        }
    }

    public void setInitialRequest(ProductRequest request) {
        this.currentState.request = request;
        if (request != null && request.getMatchedTripId() != null) {
            this.currentTripId = request.getMatchedTripId();
        }
        notifyState();
    }

    public void start(Long requestId, Long tripId) {
        this.currentRequestId = requestId;
        this.currentTripId = tripId;

        // Initial fetch from authoritative backend
        refreshFromBackend();

        // 1. Attach RTDB order event listener for nudge
        if (requestId != null) {
            liveStatusStore.attachOrderEventListener(requestId, new LiveStatusStore.OrderEventListener() {
                @Override
                public void onOrderEventReceived(Long reqId) {
                    // Nudge principle: ignore RTDB payload, re-fetch from backend
                    refreshFromBackend();
                }

                @Override
                public void onError(String errorMessage) {
                    // Fall back to polling
                    currentState.isFallbackPollingActive = true;
                    currentState.errorMessage = errorMessage;
                    notifyState();
                }
            });
        }

        // 2. Attach live location listener if trip is assigned
        attachLocationListener(tripId);

        // Slow safety net is always active while visible
        currentState.isSafetyNetPollingActive = true;
        notifyState();
    }

    public void attachLocationListener(Long tripId) {
        this.currentTripId = tripId;
        if (tripId == null) {
            currentState.liveLocation = null;
            currentState.locationTimeAgo = null;
            notifyState();
            return;
        }

        liveStatusStore.attachTripLocationListener(tripId, new LiveStatusStore.TripLocationListener() {
            @Override
            public void onLocationUpdated(LiveLocation location) {
                currentState.liveLocation = location;
                if (location != null && location.getUpdatedAt() != null) {
                    currentState.locationTimeAgo = TimeAgoFormatter.format(location.getUpdatedAt(), timeProvider.currentTimeMillis());
                } else {
                    currentState.locationTimeAgo = null;
                }
                notifyState();
            }

            @Override
            public void onLocationRemoved() {
                currentState.liveLocation = null;
                currentState.locationTimeAgo = null;
                notifyState();
            }

            @Override
            public void onError(String errorMessage) {
                // Ignore silent location errors, location is optional
            }
        });
    }

    public void refreshFromBackend() {
        if (currentRequestId == null) return;
        currentState.backendRefreshCount++;
        notifyState();

        backendDataSource.loadRequest(currentRequestId, new BackendCallback<ProductRequest>() {
            @Override
            public void onSuccess(ProductRequest data) {
                currentState.request = data;
                if (data != null && data.getMatchedTripId() != null && !data.getMatchedTripId().equals(currentTripId)) {
                    attachLocationListener(data.getMatchedTripId());
                }
                notifyState();
            }

            @Override
            public void onError(String message) {
                currentState.errorMessage = message;
                notifyState();
            }
        });

        backendDataSource.loadTimeline(currentRequestId, new BackendCallback<List<StatusUpdate>>() {
            @Override
            public void onSuccess(List<StatusUpdate> data) {
                currentState.timeline = data;
                notifyState();
            }

            @Override
            public void onError(String message) {
                // Silent
            }
        });
    }

    public void stop() {
        if (currentRequestId != null) {
            liveStatusStore.detachOrderEventListener(currentRequestId);
        }
        if (currentTripId != null) {
            liveStatusStore.detachTripLocationListener(currentTripId);
        }
        currentState.isFallbackPollingActive = false;
        currentState.isSafetyNetPollingActive = false;
        notifyState();
    }

    public ViewState getCurrentState() {
        return currentState.copy();
    }

    private void notifyState() {
        if (stateListener != null) {
            stateListener.onStateChanged(currentState.copy());
        }
    }
}
