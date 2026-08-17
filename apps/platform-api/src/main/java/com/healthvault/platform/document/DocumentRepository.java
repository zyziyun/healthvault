package com.healthvault.platform.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Listing is always scoped to the owner: no way to accidentally return someone else's rows.
    List<Document> findByUserIdOrderByIdDesc(Long userId);

    long countByStatus(String status);
}
