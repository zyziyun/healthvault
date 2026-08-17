package com.healthvault.platform.document;

import java.time.OffsetDateTime;

/**
 * What we return to the client. A DTO, not the entity: the internal model can grow columns
 * without changing the public contract, and nothing internal leaks by accident.
 */
public record DocumentResponse(
        Long id,
        String sourceType,
        String status,
        OffsetDateTime uploadedAt) {

    static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getSourceType(), d.getStatus(), d.getUploadedAt());
    }
}
