package com.healthtrack.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.mode", havingValue = "spring", matchIfMissing = true)
public class SpringEventBus implements EventBus {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(String eventType, Long hospitalId, Object payload) {
        publisher.publishEvent(new HmsEvent(this, eventType, hospitalId, payload));
    }

    @Override
    @Async
    public void publishAsync(String eventType, Long hospitalId, Object payload) {
        publisher.publishEvent(new HmsEvent(this, eventType, hospitalId, payload));
    }
}
