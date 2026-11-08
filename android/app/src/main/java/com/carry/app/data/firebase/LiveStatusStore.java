package com.carry.app.data.firebase;

import com.carry.app.models.LiveLocation;

public interface LiveStatusStore {

    interface OrderEventListener {
        void onOrderEventReceived(Long requestId);
        void onError(String errorMessage);
    }

    interface TripLocationListener {
        void onLocationUpdated(LiveLocation location);
        void onLocationRemoved();
        void onError(String errorMessage);
    }

    interface OperationCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    void emitOrderEvent(Long requestId, String status, String byUid, OperationCallback callback);

    void attachOrderEventListener(Long requestId, OrderEventListener listener);

    void detachOrderEventListener(Long requestId);

    void shareTripLocation(Long tripId, double lat, double lng, String uid, OperationCallback callback);

    void stopSharingTripLocation(Long tripId, OperationCallback callback);

    void attachTripLocationListener(Long tripId, TripLocationListener listener);

    void detachTripLocationListener(Long tripId);
}
