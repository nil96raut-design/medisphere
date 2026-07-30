package com.healthtrack.service;

import com.healthtrack.entity.EventOutbox;
import com.healthtrack.entity.WriteBufferEntry;
import com.healthtrack.repository.EventOutboxRepository;
import com.healthtrack.repository.WriteBufferRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private final EventOutboxRepository eventOutboxRepository;
    private final WriteBufferRepository writeBufferRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDeadLetterEvents() {
        List<EventOutbox> deadLetterOutbox = eventOutboxRepository.findByStatusOrderByCreatedAtDesc("DEAD_LETTER");
        List<WriteBufferEntry> deadLetterBuffer = writeBufferRepository.findByStatusOrderByCreatedAtDesc("DEAD_LETTER");

        Map<String, Object> response = new HashMap<>();
        response.put("outboxDeadLetters", deadLetterOutbox);
        response.put("writeBufferDeadLetters", deadLetterBuffer);
        response.put("totalCount", deadLetterOutbox.size() + deadLetterBuffer.size());
        return response;
    }

    @Transactional
    public void replayDeadLetter(String category, Long id) {
        if ("OUTBOX".equalsIgnoreCase(category)) {
            EventOutbox outbox = eventOutboxRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox event not found"));
            outbox.setStatus("PENDING");
            outbox.setRetryCount(0);
            eventOutboxRepository.save(outbox);
            log.info("Replayed dead letter outbox event ID: {}", id);
        } else if ("WRITE_BUFFER".equalsIgnoreCase(category)) {
            WriteBufferEntry entry = writeBufferRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Write buffer entry not found"));
            entry.setStatus("PENDING");
            entry.setRetryCount(0);
            entry.setErrorMessage(null);
            writeBufferRepository.save(entry);
            log.info("Replayed dead letter write buffer entry ID: {}", id);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category. Must be OUTBOX or WRITE_BUFFER");
        }
    }

    @Transactional
    public Map<String, Integer> replayAllDeadLetters() {
        List<EventOutbox> deadLetterOutbox = eventOutboxRepository.findByStatusOrderByCreatedAtDesc("DEAD_LETTER");
        for (EventOutbox event : deadLetterOutbox) {
            event.setStatus("PENDING");
            event.setRetryCount(0);
        }
        eventOutboxRepository.saveAll(deadLetterOutbox);

        List<WriteBufferEntry> deadLetterBuffer = writeBufferRepository.findByStatusOrderByCreatedAtDesc("DEAD_LETTER");
        for (WriteBufferEntry entry : deadLetterBuffer) {
            entry.setStatus("PENDING");
            entry.setRetryCount(0);
            entry.setErrorMessage(null);
        }
        writeBufferRepository.saveAll(deadLetterBuffer);

        log.info("Replayed {} outbox dead letters and {} write buffer dead letters",
                deadLetterOutbox.size(), deadLetterBuffer.size());

        Map<String, Integer> result = new HashMap<>();
        result.put("outboxReplayed", deadLetterOutbox.size());
        result.put("writeBufferReplayed", deadLetterBuffer.size());
        return result;
    }
}
