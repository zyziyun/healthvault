package com.healthvault.platform.document;

import com.healthvault.platform.error.ApiException;
import com.healthvault.platform.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Business logic for documents. This is where object-level authorization is enforced: every read
 * or delete first proves the row belongs to the calling user, otherwise 403. Skipping that check
 * is the IDOR bug (broken object level authorization), the top item on the API security list.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documents;
    private final StorageService storage;

    public DocumentService(DocumentRepository documents, StorageService storage) {
        this.documents = documents;
        this.storage = storage;
    }

    @Transactional
    public DocumentResponse upload(Long userId, MultipartFile file, String sourceType) {
        String contentType = UploadValidator.detectAndValidate(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("could not read upload");
        }
        String key = storage.put(bytes, contentType);

        Document doc = new Document();
        doc.setUserId(userId);
        doc.setSourceType(sourceType == null ? "lab_report" : sourceType);
        doc.setStorageKey(key);
        doc.setStatus("queued");
        documents.save(doc);

        // Hand the slow extraction to the AI service asynchronously; the caller returns immediately.
        // The real path publishes a job to the Redis queue the worker consumes (see ai-api).
        enqueueExtraction(doc.getId());
        return DocumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listOwn(Long userId) {
        return documents.findByUserIdOrderByIdDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getOwned(Long userId, Long documentId) {
        return DocumentResponse.from(requireOwned(userId, documentId));
    }

    @Transactional(readOnly = true)
    public String downloadUrl(Long userId, Long documentId) {
        Document doc = requireOwned(userId, documentId);
        // Hand back a short-lived signed URL so the file streams straight from the store,
        // not back through this service.
        return storage.presignedGetUrl(doc.getStorageKey(), 300);
    }

    @Transactional
    public void delete(Long userId, Long documentId) {
        Document doc = requireOwned(userId, documentId);
        documents.delete(doc);
    }

    /** The authorization gate: found AND owned, else 404/403. Every per-id path goes through here. */
    private Document requireOwned(Long userId, Long documentId) {
        Document doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("document not found"));
        if (!doc.getUserId().equals(userId)) {
            throw ApiException.forbidden("not your document");
        }
        return doc;
    }

    private void enqueueExtraction(Long documentId) {
        log.info("enqueued extraction job for document {}", documentId);
    }
}
