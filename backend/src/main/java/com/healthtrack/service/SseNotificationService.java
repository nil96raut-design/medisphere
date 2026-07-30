package com.healthtrack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    // Map of hospitalId -> List of active SseEmitters
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long hospitalId) {
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout

        emitters.computeIfAbsent(hospitalId, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

        emitter.onCompletion(() -> removeEmitter(hospitalId, emitter));
        emitter.onTimeout(() -> removeEmitter(hospitalId, emitter));
        emitter.onError(e -> removeEmitter(hospitalId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected to HealthTrack Real-Time Event Stream"));
        } catch (IOException e) {
            removeEmitter(hospitalId, emitter);
        }

        log.info("Client subscribed to SSE stream for hospitalId {}", hospitalId);
        return emitter;
    }

    public void broadcastToHospital(Long hospitalId, String eventName, Object data) {
        List<SseEmitter> hospitalEmitters = emitters.get(hospitalId);
        if (hospitalEmitters == null || hospitalEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        synchronized (hospitalEmitters) {
            for (SseEmitter emitter : hospitalEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            hospitalEmitters.removeAll(deadEmitters);
        }
    }

    private void removeEmitter(Long hospitalId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(hospitalId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
