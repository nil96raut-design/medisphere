package com.healthtrack.service;

import com.healthtrack.entity.IdempotencyRecord;
import com.healthtrack.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    @Transactional
    public boolean isProcessed(String requestId) {
        return idempotencyRepository.existsByRequestId(requestId);
    }

    @Transactional
    public void markProcessed(String requestId, Long hospitalId, String actionType, String result) {
        if (!idempotencyRepository.existsByRequestId(requestId)) {
            idempotencyRepository.save(IdempotencyRecord.builder()
                    .requestId(requestId)
                    .hospitalId(hospitalId)
                    .actionType(actionType)
                    .result(result)
                    .createdAt(OffsetDateTime.now())
                    .build());
        }
    }

    @Transactional
    public boolean tryProcess(String requestId, Long hospitalId, String actionType) {
        if (idempotencyRepository.existsByRequestId(requestId)) {
            return false;
        }
        try {
            idempotencyRepository.save(IdempotencyRecord.builder()
                    .requestId(requestId)
                    .hospitalId(hospitalId)
                    .actionType(actionType)
                    .createdAt(OffsetDateTime.now())
                    .build());
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return false;
        }
    }
}
