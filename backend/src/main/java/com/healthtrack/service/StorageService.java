package com.healthtrack.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${supabase.url:https://db.mtplvschncmlhlrdkvvc.supabase.co}")
    private String supabaseUrl;

    public String generatePreSignedUrl(String bucket, String filePath, int expirationMinutes) {
        String token = UUID.randomUUID().toString();
        log.info("Generated pre-signed URL for bucket '{}', path '{}', valid for {}m", bucket, filePath, expirationMinutes);
        return String.format("%s/storage/v1/object/sign/%s/%s?token=%s&expiresIn=%d",
                supabaseUrl, bucket, filePath, token, expirationMinutes * 60);
    }
}
