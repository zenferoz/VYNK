package com.vynk.file.controller;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.file.dto.FileUploadResponse;
import com.vynk.file.entity.UploadedFile;
import com.vynk.file.service.FileStorageService;
import com.vynk.security.ratelimit.RateLimitingService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RateLimitingService rateLimitingService;

    public FileController(
            FileStorageService fileStorageService,
            UserRepository userRepository,
            RateLimitingService rateLimitingService) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "associationType", required = false) String associationType,
            @RequestParam(value = "associationId", required = false) Long associationId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (rateLimitingService != null) {
            rateLimitingService.checkLimit("upload", user.getId().toString(), 15, 60);
        }
        FileUploadResponse response = fileStorageService.storeFile(user, file, associationType, associationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        UploadedFile metadata = fileStorageService.getFileMetadata(user, id);
        Resource resource = fileStorageService.loadFileAsResource(user, id);

        String contentType = metadata.getFileType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String safeFileName = metadata.getFileName() != null
                ? metadata.getFileName().replaceAll("[\\r\\n\"]", "_")
                : "download";

        org.springframework.http.ContentDisposition contentDisposition =
                org.springframework.http.ContentDisposition.attachment()
                        .filename(safeFileName, java.nio.charset.StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        fileStorageService.deleteFile(user, id);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for active session."));
    }
}
