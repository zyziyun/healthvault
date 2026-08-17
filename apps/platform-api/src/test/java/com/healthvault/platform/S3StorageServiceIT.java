package com.healthvault.platform;

import com.healthvault.platform.storage.S3StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real round-trip against a running MinIO. Skipped unless MINIO_IT=1 (CI has no MinIO), so it
 * never blocks the normal build. Run locally with: docker compose up -d minio minio-init, then
 * {@code MINIO_IT=1 ./gradlew test --tests '*S3StorageServiceIT'}.
 */
@EnabledIfEnvironmentVariable(named = "MINIO_IT", matches = "1")
class S3StorageServiceIT {

    private S3StorageService storage() {
        return new S3StorageService("healthvault", "us-east-1",
                "http://localhost:9000", "minioadmin", "minioadmin");
    }

    @Test
    void putThenGetRoundTrips() {
        S3StorageService s3 = storage();
        byte[] original = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};
        String key = s3.put(original, "image/png");
        assertNotNull(key);
        assertArrayEquals(original, s3.get(key));
    }

    @Test
    void presignedUrlPointsAtTheObject() {
        S3StorageService s3 = storage();
        String key = s3.put("hello".getBytes(), "text/plain");
        String url = s3.presignedGetUrl(key, 300);
        assertTrue(url.contains(key), "presigned url should reference the object key");
        assertTrue(url.contains("X-Amz-Signature"), "presigned url should be signed");
    }
}
