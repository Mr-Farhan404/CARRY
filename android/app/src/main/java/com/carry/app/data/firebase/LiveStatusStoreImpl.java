package com.carry.app.data.firebase;

import com.carry.app.models.LiveLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveStatusStoreImpl implements LiveStatusStore {

    public static final String RTDB_URL = "https://carry-56e49-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String NODE_ORDER_EVENTS = "orderEvents";
    private static final String NODE_TRIP_LOCATIONS = "tripLocations";

    private final FirebaseDatabase database;
    private final Map<Long, ValueEventListener> orderListeners = new ConcurrentHashMap<>();
    private final Map<Long, ValueEventListener> locationListeners = new ConcurrentHashMap<>();

    public LiveStatusStoreImpl() {
        this.database = FirebaseDatabase.getInstance(RTDB_URL);
    }

    public LiveStatusStoreImpl(FirebaseDatabase database) {
        this.database = database;
    }

    @Override
    public void emitOrderEvent(Long requestId, String status, String byUid, OperationCallback callback) {
        if (requestId == null) {
            if (callback != null) callback.onFailure("Invalid request ID.");
            return;
        }

        DatabaseReference ref = database.getReference(NODE_ORDER_EVENTS).child(String.valueOf(requestId));
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("status", status != null ? status : "UPDATED");
        eventData.put("updatedAt", ServerValue.TIMESTAMP);
        eventData.put("byUid", byUid != null ? byUid : "");

        ref.setValue(eventData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to emit live event: " + e.getMessage());
                });
    }

    @Override
    public void attachOrderEventListener(Long requestId, OrderEventListener listener) {
        if (requestId == null || listener == null) return;

        detachOrderEventListener(requestId);

        DatabaseReference ref = database.getReference(NODE_ORDER_EVENTS).child(String.valueOf(requestId));
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listener.onOrderEventReceived(requestId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError("Realtime updates unavailable: " + error.getMessage());
            }
        };

        orderListeners.put(requestId, valueEventListener);
        ref.addValueEventListener(valueEventListener);
    }

    @Override
    public void detachOrderEventListener(Long requestId) {
        if (requestId == null) return;
        ValueEventListener listener = orderListeners.remove(requestId);
        if (listener != null) {
            DatabaseReference ref = database.getReference(NODE_ORDER_EVENTS).child(String.valueOf(requestId));
            ref.removeEventListener(listener);
        }
    }

    @Override
    public void shareTripLocation(Long tripId, double lat, double lng, String uid, OperationCallback callback) {
        if (tripId == null) {
            if (callback != null) callback.onFailure("Invalid trip ID.");
            return;
        }

        DatabaseReference ref = database.getReference(NODE_TRIP_LOCATIONS).child(String.valueOf(tripId));
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("lat", lat);
        locationData.put("lng", lng);
        locationData.put("updatedAt", ServerValue.TIMESTAMP);
        locationData.put("uid", uid != null ? uid : "");

        ref.setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to share location: " + e.getMessage());
                });
    }

    @Override
    public void stopSharingTripLocation(Long tripId, OperationCallback callback) {
        if (tripId == null) return;

        DatabaseReference ref = database.getReference(NODE_TRIP_LOCATIONS).child(String.valueOf(tripId));
        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to remove shared location.");
                });
    }

    @Override
    public void attachTripLocationListener(Long tripId, TripLocationListener listener) {
        if (tripId == null || listener == null) return;

        detachTripLocationListener(tripId);

        DatabaseReference ref = database.getReference(NODE_TRIP_LOCATIONS).child(String.valueOf(tripId));
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Double lat = snapshot.child("lat").getValue(Double.class);
                        Double lng = snapshot.child("lng").getValue(Double.class);
                        Long updatedAt = snapshot.child("updatedAt").getValue(Long.class);
                        String uid = snapshot.child("uid").getValue(String.class);

                        LiveLocation location = new LiveLocation(lat, lng, updatedAt, uid);
                        listener.onLocationUpdated(location);
                    } catch (Exception e) {
                        listener.onError("Failed to parse location.");
                    }
                } else {
                    listener.onLocationRemoved();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError("Location updates unavailable: " + error.getMessage());
            }
        };

        locationListeners.put(tripId, valueEventListener);
        ref.addValueEventListener(valueEventListener);
    }

    @Override
    public void detachTripLocationListener(Long tripId) {
        if (tripId == null) return;
        ValueEventListener listener = locationListeners.remove(tripId);
        if (listener != null) {
            DatabaseReference ref = database.getReference(NODE_TRIP_LOCATIONS).child(String.valueOf(tripId));
            ref.removeEventListener(listener);
        }
    }
}
