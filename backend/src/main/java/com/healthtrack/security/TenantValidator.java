package com.healthtrack.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TenantValidator {

    public void validateHospitalAccess(Long entityHospitalId, Long currentHospitalId) {
        if (!entityHospitalId.equals(currentHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-tenant access denied");
        }
    }
}
