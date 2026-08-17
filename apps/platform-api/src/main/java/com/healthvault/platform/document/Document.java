package com.healthvault.platform.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Maps the {@code documents} table (created in Flyway migration V2). */
@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owner. Every query is scoped by this, and it comes from the JWT, never the request body.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    // Key into object storage; the bytes live in S3, not here.
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String status = "queued";

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private OffsetDateTime uploadedAt;
}
