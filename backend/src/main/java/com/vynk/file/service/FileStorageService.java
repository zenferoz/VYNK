package com.vynk.file.service;

import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.file.dto.FileUploadResponse;
import com.vynk.file.entity.UploadedFile;
import com.vynk.file.repository.UploadedFileRepository;
import com.vynk.community.repository.CommunityMemberRepository;
import com.vynk.message.entity.Message;
import com.vynk.message.repository.MessageRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf",
            "text/plain"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${vynk.files.upload-dir:${user.home}/.vynk/uploads}")
    private String uploadDir;

    private Path rootLocation;
    private final UploadedFileRepository fileRepository;
    private final MessageRepository messageRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final SafetyService safetyService;
    private final ConnectionRequestRepository connectionRequestRepository;

    public FileStorageService(UploadedFileRepository fileRepository) {
        this(fileRepository, null, null, null, null);
    }

    @Autowired
    public FileStorageService(
            UploadedFileRepository fileRepository,
            @Autowired(required = false) MessageRepository messageRepository,
            @Autowired(required = false) CommunityMemberRepository communityMemberRepository,
            @Autowired(required = false) SafetyService safetyService,
            @Autowired(required = false) ConnectionRequestRepository connectionRequestRepository) {
        this.fileRepository = fileRepository;
        this.messageRepository = messageRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.safetyService = safetyService;
        this.connectionRequestRepository = connectionRequestRepository;
    }

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("Initialized secure upload directory at: {}", rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize upload directory", e);
        }
    }

    @Transactional
    public FileUploadResponse storeFile(
            User currentUser,
            MultipartFile file,
            String associationType,
            Long associationId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File type not permitted. Allowed types: JPEG, PNG, PDF, TXT.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename with path traversal characters.");
        }

        // Clean filename
        String sanitizedName = Paths.get(originalFilename).getFileName().toString();
        String extension = "";
        int dotIndex = sanitizedName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = sanitizedName.substring(dotIndex);
        }

        // Validate actual file content and signature
        String validatedContentType = detectAndValidateContent(file, extension, file.getContentType());

        // Validate upload-time association authorization
        if (associationType != null) {
            if (associationType.equalsIgnoreCase("MESSAGE") && associationId != null) {
                if (currentUser.getId().equals(associationId)) {
                    throw new IllegalArgumentException("Cannot send file attachments to yourself.");
                }
                if (safetyService != null && safetyService.isBlockedEitherWay(currentUser.getId(), associationId)) {
                    throw new AccessDeniedException("Cannot upload attachment for a blocked user.");
                }
                if (connectionRequestRepository != null && !connectionRequestRepository.existsAcceptedBetween(currentUser.getId(), associationId)) {
                    boolean isExistingMessage = messageRepository != null && messageRepository.existsById(associationId);
                    if (!isExistingMessage) {
                        throw new AccessDeniedException("Active accepted connection required to send message attachments.");
                    }
                }
            } else if (associationType.equalsIgnoreCase("COMMUNITY") && associationId != null) {
                if (communityMemberRepository != null && !communityMemberRepository.existsByCommunityIdAndUserId(associationId, currentUser.getId())) {
                    throw new AccessDeniedException("You must be an active member of this community to upload attachments.");
                }
            }
        }

        String storedFileName = UUID.randomUUID() + extension;
        Path destinationFile = null;

        try {
            destinationFile = this.rootLocation.resolve(storedFileName).normalize().toAbsolutePath();

            // Guard against path traversal outside root
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new IllegalArgumentException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            UploadedFile uploadedFile = new UploadedFile(
                    sanitizedName,
                    storedFileName,
                    validatedContentType,
                    file.getSize(),
                    currentUser,
                    associationType,
                    associationId
            );

            UploadedFile saved = fileRepository.save(uploadedFile);
            return new FileUploadResponse(saved);

        } catch (Exception e) {
            if (destinationFile != null) {
                try {
                    Files.deleteIfExists(destinationFile);
                } catch (IOException cleanupEx) {
                    log.warn("Failed to clean up orphaned file after failure: {}", cleanupEx.getMessage());
                }
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            log.error("Failed to store file: {}", e.getMessage());
            throw new IllegalStateException("Failed to store uploaded file.", e);
        }
    }

    public String detectAndValidateContent(MultipartFile file, String extension, String declaredMime) {
        byte[] headerBytes = new byte[Math.min(4096, (int) Math.max(1, file.getSize()))];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.read(headerBytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file content for validation", e);
        }
        if (bytesRead <= 0) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }

        // Check JPEG: FF D8 FF
        if (bytesRead >= 3 &&
                (headerBytes[0] & 0xFF) == 0xFF &&
                (headerBytes[1] & 0xFF) == 0xD8 &&
                (headerBytes[2] & 0xFF) == 0xFF) {
            if (!extension.equalsIgnoreCase(".jpg") && !extension.equalsIgnoreCase(".jpeg")) {
                throw new IllegalArgumentException("File extension does not match detected JPEG format.");
            }
            return "image/jpeg";
        }

        // Check PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytesRead >= 8 &&
                (headerBytes[0] & 0xFF) == 0x89 &&
                headerBytes[1] == 'P' &&
                headerBytes[2] == 'N' &&
                headerBytes[3] == 'G' &&
                headerBytes[4] == 0x0D &&
                headerBytes[5] == 0x0A &&
                headerBytes[6] == 0x1A &&
                headerBytes[7] == 0x0A) {
            if (!extension.equalsIgnoreCase(".png")) {
                throw new IllegalArgumentException("File extension does not match detected PNG format.");
            }
            return "image/png";
        }

        // Check PDF: %PDF- (25 50 44 46 2D)
        if (bytesRead >= 5 &&
                headerBytes[0] == '%' &&
                headerBytes[1] == 'P' &&
                headerBytes[2] == 'D' &&
                headerBytes[3] == 'F' &&
                headerBytes[4] == '-') {
            if (!extension.equalsIgnoreCase(".pdf")) {
                throw new IllegalArgumentException("File extension does not match detected PDF format.");
            }
            return "application/pdf";
        }

        // Check Disguised Executable or Binary headers:
        // Windows PE: MZ (4D 5A)
        if (bytesRead >= 2 && headerBytes[0] == 0x4D && headerBytes[1] == 0x5A) {
            throw new IllegalArgumentException("Executable binaries (PE/MZ) are not permitted.");
        }
        // Linux ELF: 7F 45 4C 46
        if (bytesRead >= 4 && (headerBytes[0] & 0xFF) == 0x7F && headerBytes[1] == 'E' && headerBytes[2] == 'L' && headerBytes[3] == 'F') {
            throw new IllegalArgumentException("Executable binaries (ELF) are not permitted.");
        }
        // Java Class: CA FE BA BE
        if (bytesRead >= 4 &&
                (headerBytes[0] & 0xFF) == 0xCA &&
                (headerBytes[1] & 0xFF) == 0xFE &&
                (headerBytes[2] & 0xFF) == 0xBA &&
                (headerBytes[3] & 0xFF) == 0xBE) {
            throw new IllegalArgumentException("Java class binaries are not permitted.");
        }

        // Plain Text Validation
        if (extension.equalsIgnoreCase(".txt")) {
            for (int i = 0; i < bytesRead; i++) {
                byte b = headerBytes[i];
                if (b == 0x00) {
                    throw new IllegalArgumentException("Binary content with null bytes is not permitted as text.");
                }
                if ((b >= 0 && b < 9) || b == 11 || b == 12 || (b >= 14 && b < 32)) {
                    throw new IllegalArgumentException("Control characters are not permitted in plain text files.");
                }
            }

            String textPreamble = new String(headerBytes, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8)
                    .toLowerCase(java.util.Locale.ROOT)
                    .trim();

            if (textPreamble.startsWith("<!doctype") ||
                    textPreamble.startsWith("<html") ||
                    textPreamble.contains("<script") ||
                    textPreamble.contains("<svg") ||
                    textPreamble.startsWith("<?php") ||
                    textPreamble.startsWith("#!/bin/")) {
                throw new IllegalArgumentException("Script, HTML, or executable markup is not permitted in text uploads.");
            }

            return "text/plain";
        }

        throw new IllegalArgumentException("File content signature does not match any allowed format (JPEG, PNG, PDF, TXT).");
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(User currentUser, Long fileId) {
        UploadedFile uploadedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));

        verifyFileAccessAuthorization(currentUser, uploadedFile);

        try {
            Path filePath = this.rootLocation.resolve(uploadedFile.getStoredFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on server.");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File could not be read: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UploadedFile getFileMetadata(User currentUser, Long fileId) {
        UploadedFile uploadedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        verifyFileAccessAuthorization(currentUser, uploadedFile);
        return uploadedFile;
    }

    @Transactional(readOnly = true)
    public UploadedFile getFileMetadata(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
    }

    public void verifyFileAccessAuthorization(User currentUser, UploadedFile uploadedFile) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authentication required.");
        }

        // College isolation check
        if (uploadedFile.getUploader().getCollege() != null && currentUser.getCollege() != null &&
            !uploadedFile.getUploader().getCollege().getId().equals(currentUser.getCollege().getId())) {
            throw new AccessDeniedException("Cannot access files from another college.");
        }

        // Uploader can always access their uploaded file
        if (uploadedFile.getUploader().getId().equals(currentUser.getId())) {
            return;
        }

        String assocType = uploadedFile.getAssociationType();
        Long assocId = uploadedFile.getAssociationId();

        if (assocType != null && assocType.equalsIgnoreCase("MESSAGE")) {
            if (assocId == null) {
                throw new AccessDeniedException("Invalid message attachment association.");
            }

            // Check if assocId corresponds to a Message record
            if (messageRepository != null) {
                Optional<Message> msgOpt = messageRepository.findById(assocId);
                if (msgOpt.isPresent()) {
                    Message msg = msgOpt.get();
                    Long senderId = msg.getSender().getId();
                    Long recipientId = msg.getRecipient().getId();
                    if (!currentUser.getId().equals(senderId) && !currentUser.getId().equals(recipientId)) {
                        throw new AccessDeniedException("Not authorized to access this message attachment.");
                    }
                    if (safetyService != null && (safetyService.isBlockedEitherWay(currentUser.getId(), senderId) ||
                                                  safetyService.isBlockedEitherWay(currentUser.getId(), recipientId))) {
                        throw new AccessDeniedException("Access blocked.");
                    }
                    return; // Authorized!
                }
            }

            // Fallback: if assocId was stored as the recipient peer ID
            if (currentUser.getId().equals(assocId)) {
                if (safetyService != null && safetyService.isBlockedEitherWay(currentUser.getId(), uploadedFile.getUploader().getId())) {
                    throw new AccessDeniedException("Access blocked.");
                }
                if (connectionRequestRepository != null && !connectionRequestRepository.existsAcceptedBetween(currentUser.getId(), uploadedFile.getUploader().getId())) {
                    throw new AccessDeniedException("Active accepted connection required to access message attachments.");
                }
                return; // Authorized peer recipient!
            }

            throw new AccessDeniedException("Not authorized to access this message attachment.");
        }

        if (assocType != null && assocType.equalsIgnoreCase("COMMUNITY")) {
            if (assocId == null) {
                throw new AccessDeniedException("Invalid community attachment association.");
            }
            if (communityMemberRepository != null) {
                boolean isMember = communityMemberRepository.existsByCommunityIdAndUserId(assocId, currentUser.getId());
                if (!isMember) {
                    throw new AccessDeniedException("You must be an active member of this community to access attachments.");
                }
                return; // Authorized community member!
            }
            throw new AccessDeniedException("Not authorized to access community attachment.");
        }

        // Missing, unknown, or unrelated association: only uploader can access
        throw new AccessDeniedException("Not authorized to access this file.");
    }

    @Transactional
    public void deleteFile(User currentUser, Long fileId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        UploadedFile uploadedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        if (!uploadedFile.getUploader().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the uploader can delete this file.");
        }

        String assocType = uploadedFile.getAssociationType();
        if (assocType != null && !assocType.isBlank()) {
            if (assocType.equalsIgnoreCase("MESSAGE") || assocType.equalsIgnoreCase("COMMUNITY")) {
                throw new IllegalStateException("Cannot delete file with an active " + assocType.toLowerCase(java.util.Locale.ROOT) + " association.");
            }
        }

        Path filePath = this.rootLocation.resolve(uploadedFile.getStoredFileName()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file {}: {}", filePath, e.getMessage());
        }
        fileRepository.delete(uploadedFile);
    }
}
