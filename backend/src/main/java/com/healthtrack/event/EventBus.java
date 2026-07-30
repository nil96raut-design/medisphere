package com.healthtrack.event;

public interface EventBus {

    void publish(String eventType, Long hospitalId, Object payload);

    void publishAsync(String eventType, Long hospitalId, Object payload);
}
