package com.healthvault.platform.document;

import com.healthvault.platform.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Document CRUD, all scoped to the JWT-authenticated user. The user id comes from
 * {@code @CurrentUser} (set by the JWT filter), never from a request parameter, so a caller
 * cannot act on behalf of someone else by changing a field.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse upload(@CurrentUser Long userId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "sourceType", required = false) String sourceType) {
        return documents.upload(userId, file, sourceType);
    }

    @GetMapping
    public List<DocumentResponse> list(@CurrentUser Long userId) {
        return documents.listOwn(userId);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@CurrentUser Long userId, @PathVariable Long id) {
        return documents.getOwned(userId, id);
    }

    @GetMapping("/{id}/download")
    public Map<String, String> download(@CurrentUser Long userId, @PathVariable Long id) {
        return Map.of("url", documents.downloadUrl(userId, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser Long userId, @PathVariable Long id) {
        documents.delete(userId, id);
    }
}
