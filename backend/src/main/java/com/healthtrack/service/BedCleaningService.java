package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.BedCleaningResponse;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BedCleaningService {

    private final BedCleaningRepository bedCleaningRepository;
    private final BedRepository bedRepository;
    private final UserRepository userRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    public BedCleaningResponse requestCleaning(Long bedId, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));
        tenantValidator.validateHospitalAccess(bed.getHospital().getId(), hospitalId);

        if (!bed.getIsOccupied()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bed is not occupied, no cleaning needed");
        }

        if (bedCleaningRepository.existsByBedIdAndStatus(bedId, CleaningStatus.REQUESTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cleaning already requested for this bed");
        }

        BedCleaningRequest request = BedCleaningRequest.builder()
                .hospital(bed.getHospital())
                .bed(bed)
                .requestedBy(currentUser.getUser())
                .build();
        request = bedCleaningRepository.save(request);

        eventPublisher.publish(EventConstants.BED_CLEANING_REQUESTED, hospitalId,
                Map.of("cleaningId", request.getId(), "bedId", bedId,
                        "wardName", bed.getWardName(), "bedNumber", bed.getBedNumber(),
                        "hospitalId", hospitalId));

        return mapCleaningResponse(request);
    }

    @Transactional
    public BedCleaningResponse markCleaned(Long cleaningId, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();

        BedCleaningRequest request = bedCleaningRepository.findById(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning request not found"));
        tenantValidator.validateHospitalAccess(request.getHospital().getId(), hospitalId);

        if (request.getStatus() == CleaningStatus.CLEANED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bed already marked as cleaned");
        }

        request.setStatus(CleaningStatus.CLEANED);
        request.setCleanedBy(currentUser.getUser());
        request.setCleanedAt(OffsetDateTime.now());

        Bed bed = request.getBed();
        bed.setIsOccupied(false);
        bedRepository.save(bed);

        request = bedCleaningRepository.save(request);

        eventPublisher.publish(EventConstants.BED_CLEANED, hospitalId,
                Map.of("cleaningId", cleaningId, "bedId", request.getBed().getId(),
                        "wardName", request.getBed().getWardName(),
                        "bedNumber", request.getBed().getBedNumber(),
                        "hospitalId", hospitalId));

        return mapCleaningResponse(request);
    }

    @Transactional
    public BedCleaningResponse autoRequestCleaningOnDischarge(Long bedId, UserPrincipal currentUser) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));

        if (!bedCleaningRepository.existsByBedIdAndStatus(bedId, CleaningStatus.REQUESTED)) {
            BedCleaningRequest request = BedCleaningRequest.builder()
                    .hospital(bed.getHospital())
                    .bed(bed)
                    .requestedBy(currentUser.getUser())
                    .build();
            request = bedCleaningRepository.save(request);

            eventPublisher.publish(EventConstants.BED_CLEANING_REQUESTED, currentUser.getHospitalId(),
                    Map.of("cleaningId", request.getId(), "bedId", bedId,
                            "wardName", bed.getWardName(), "bedNumber", bed.getBedNumber(),
                            "hospitalId", currentUser.getHospitalId()));
            return mapCleaningResponse(request);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<BedCleaningResponse> getPendingCleaningRequests(UserPrincipal currentUser) {
        return bedCleaningRepository.findByStatusOrderByCreatedAtDesc(CleaningStatus.REQUESTED).stream()
                .filter(r -> r.getHospital().getId().equals(currentUser.getHospitalId()))
                .map(this::mapCleaningResponse).toList();
    }

    private BedCleaningResponse mapCleaningResponse(BedCleaningRequest r) {
        return new BedCleaningResponse(
                r.getId(), r.getBed().getId(), r.getBed().getBedNumber(),
                r.getBed().getWardName(),
                r.getRequestedBy().getId(), r.getRequestedBy().getFullName(),
                r.getStatus(), r.getCreatedAt(), r.getCleanedAt());
    }
}
