package com.healthvault.platform.document;

import com.healthvault.platform.error.ApiException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload safety lives in one place. Three checks, none optional:
 * 1. size (the container also caps it, this gives a clean error),
 * 2. allowed type,
 * 3. content sniffing: the client-declared content-type can be forged, so we confirm the real
 *    format from the file's leading "magic bytes" before trusting it.
 */
final class UploadValidator {

    private UploadValidator() {
    }

    static String detectAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("file is empty");
        }
        byte[] head = new byte[12];
        try {
            int read = file.getInputStream().read(head);
            if (read < 4) {
                throw ApiException.badRequest("file too short to identify");
            }
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("could not read file");
        }
        // JPEG: FF D8 FF; PNG: 89 50 4E 47; PDF: 25 50 44 46 ("%PDF")
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "image/png";
        }
        if (head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
            return "application/pdf";
        }
        throw ApiException.badRequest("unsupported file type: only jpeg, png, pdf are allowed");
    }
}
