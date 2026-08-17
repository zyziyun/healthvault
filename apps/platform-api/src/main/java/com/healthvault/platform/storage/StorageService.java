package com.healthvault.platform.storage;

/**
 * Object storage boundary. Files live in an object store (S3 in production), never in Postgres;
 * the database keeps only the returned {@code storageKey} plus metadata. The interface lets the
 * filesystem impl back local/CI runs and the S3 impl back production without touching callers.
 */
public interface StorageService {

    /** Stores the bytes under a freshly generated key and returns that key. */
    String put(byte[] content, String contentType);

    /** Fetches the bytes for a key. */
    byte[] get(String storageKey);

    /**
     * A short-lived, signed URL the client can use to download straight from the store,
     * so file bytes never flow back through this service. Filesystem impl has no real URL.
     */
    String presignedGetUrl(String storageKey, long ttlSeconds);
}
