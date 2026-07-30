package com.healthtrack.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HmsEvent extends ApplicationEvent {

    private final String eventType;
    private final Long hospitalId;
    private final Object payload;

    public HmsEvent(Object source, String eventType, Long hospitalId, Object payload) {
        super(source);
        this.eventType = eventType;
        this.hospitalId = hospitalId;
        this.payload = payload;
    }
}
