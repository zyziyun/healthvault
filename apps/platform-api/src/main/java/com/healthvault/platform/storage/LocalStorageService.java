package com.healthvault.platform.storage;

import com.healthvault.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Default object store backed by the local filesystem. Used for local dev, tests, and CI so the
 * build needs no AWS credentials. Swap in {@link S3StorageService} with the {@code s3} profile to
 * talk to real S3 or MinIO; callers do not change. Keys look like {@code 2026/08/16/<uuid>} so a
 * key is portable between the two implementations.
 */
@Service
@Profile("!s3")
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${storage.local.dir:${java.io.tmpdir}/healthvault-storage}") String dir) {
        this.root = Path.of(dir);
    }

    @Override
    public String put(byte[] content, String contentType) {
        LocalDate today = LocalDate.now();
        String key = "%04d/%02d/%02d/%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID());
        try {
            Path target = root.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "storage_error", "could not store file");
        }
        return key;
    }

    @Override
    public byte[] get(String storageKey) {
        try {
            return Files.readAllBytes(root.resolve(storageKey));
        } catch (IOException e) {
            throw ApiException.notFound("object not found");
        }
    }

    @Override
    public String presignedGetUrl(String storageKey, long ttlSeconds) {
        // No signing on a filesystem; expose a local path stand-in so the API shape is the same.
        return root.resolve(storageKey).toUri().toString();
    }
}
