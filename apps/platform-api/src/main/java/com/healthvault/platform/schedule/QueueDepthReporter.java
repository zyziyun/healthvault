package com.healthvault.platform.schedule;

import com.healthvault.platform.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A time-triggered background job (contrast with the queue, which is event-triggered). Every
 * five minutes it logs how many documents are still waiting to be processed, a simple health
 * signal for the async pipeline.
 * <p>
 * Production caveat: with more than one instance, {@code @Scheduled} fires on every instance, so
 * a job with side effects would run N times. The fix is a distributed lock (e.g. ShedLock) or a
 * dedicated scheduler (Quartz). This job is read-only, so duplicate runs are harmless.
 */
@Component
public class QueueDepthReporter {

    private static final Logger log = LoggerFactory.getLogger(QueueDepthReporter.class);

    private final DocumentRepository documents;

    public QueueDepthReporter(DocumentRepository documents) {
        this.documents = documents;
    }

    @Scheduled(fixedDelayString = "${scheduling.queue-report.delay-ms:300000}")
    public void report() {
        long queued = documents.countByStatus("queued");
        log.info("documents waiting in queue: {}", queued);
    }
}
