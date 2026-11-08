package com.carry.app.data.firebase;

/**
 * Handle returned when attaching a real-time chat listener.
 * Allows detaching the listener cleanly when the activity or ViewModel is destroyed.
 */
public interface ChatSubscription {
    void remove();
}
