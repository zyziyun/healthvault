package com.healthvault.platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class DocumentController {

    // Accepts a document upload, records it, and enqueues it for the AI service.
    // The heavy extraction runs async on the worker, so this returns immediately.
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> upload(@RequestParam String filename) {
        String documentId = UUID.randomUUID().toString();
        // TODO: persist the document row and publish a job to the Redis queue.
        return Map.of(
                "documentId", documentId,
                "filename", filename,
                "status", "queued"
        );
    }
}
