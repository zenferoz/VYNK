package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.file.entity.UploadedFile;
import com.vynk.file.repository.UploadedFileRepository;
import com.vynk.file.service.FileStorageService;
import com.vynk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.vynk.community.repository.CommunityMemberRepository;
import com.vynk.connection.repository.ConnectionRequestRepository;
import com.vynk.message.entity.Message;
import com.vynk.message.repository.MessageRepository;
import com.vynk.safety.service.SafetyService;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    private UploadedFileRepository fileRepository;
    private MessageRepository messageRepository;
    private CommunityMemberRepository communityMemberRepository;
    private SafetyService safetyService;
    private ConnectionRequestRepository connectionRequestRepository;
    private FileStorageService storageService;

    @TempDir
    Path tempDir;

    private User user;
    private College college;

    @BeforeEach
    void setUp() throws Exception {
        fileRepository = mock(UploadedFileRepository.class);
        messageRepository = mock(MessageRepository.class);
        communityMemberRepository = mock(CommunityMemberRepository.class);
        safetyService = mock(SafetyService.class);
        connectionRequestRepository = mock(ConnectionRequestRepository.class);

        storageService = new FileStorageService(
                fileRepository,
                messageRepository,
                communityMemberRepository,
                safetyService,
                connectionRequestRepository
        );

        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
        storageService.init();

        college = new College();
        setField(college, "id", 1L);

        user = new User();
        setField(user, "id", 5L);
        user.setCollege(college);

        when(connectionRequestRepository.existsAcceptedBetween(anyLong(), anyLong())).thenReturn(true);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void storesValidPdfFileSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "%PDF-1.4 Sample valid PDF document content".getBytes()
        );

        when(fileRepository.save(any(UploadedFile.class))).thenAnswer(i -> {
            UploadedFile f = i.getArgument(0);
            setField(f, "id", 1L);
            return f;
        });

        var res = storageService.storeFile(user, file, "MESSAGE", 10L);

        assertNotNull(res);
        assertEquals(1L, res.getId());
        assertEquals("resume.pdf", res.getFileName());
        assertEquals("application/pdf", res.getFileType());
    }

    @Test
    void storesValidJpegAndPngSuccessfully() throws Exception {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        MockMultipartFile jpeg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

        when(fileRepository.save(any(UploadedFile.class))).thenAnswer(i -> i.getArgument(0));

        var jpegRes = storageService.storeFile(user, jpeg, "MESSAGE", 10L);
        assertEquals("image/jpeg", jpegRes.getFileType());

        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        MockMultipartFile png = new MockMultipartFile("file", "image.png", "image/png", pngBytes);

        var pngRes = storageService.storeFile(user, png, "MESSAGE", 10L);
        assertEquals("image/png", pngRes.getFileType());
    }

    @Test
    void storesValidTextFileSuccessfully() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "These are valid plain text lecture notes.\nTab:\tEnd of notes.".getBytes()
        );

        when(fileRepository.save(any(UploadedFile.class))).thenAnswer(i -> i.getArgument(0));

        var res = storageService.storeFile(user, textFile, "MESSAGE", 10L);
        assertEquals("text/plain", res.getFileType());
    }

    @Test
    void rejectsForgedMimeWithExecutableBinary() {
        byte[] mzExeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00, 0x00}; // Windows MZ header
        MockMultipartFile forged = new MockMultipartFile("file", "innocent.pdf", "application/pdf", mzExeBytes);

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(user, forged, "MESSAGE", 10L)
        );
    }

    @Test
    void rejectsDisguisedHtmlAndScriptInText() {
        MockMultipartFile htmlDisguised = new MockMultipartFile(
                "file",
                "exploit.txt",
                "text/plain",
                "<!DOCTYPE html><html><script>alert(1)</script></html>".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(user, htmlDisguised, "MESSAGE", 10L)
        );
    }

    @Test
    void rejectsDisallowedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/x-msdownload",
                "executable bytes".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(user, file, "MESSAGE", 10L)
        );
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(user, file, "MESSAGE", 10L)
        );
    }

    @Test
    void rejectsPathTraversalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "text/plain",
                "hello".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(user, file, "MESSAGE", 10L)
        );
    }

    @Test
    void uploaderCanAlwaysDownloadTheirOwnFile() throws Exception {
        Path physicalFile = tempDir.resolve("my-doc.pdf");
        Files.writeString(physicalFile, "content");

        UploadedFile uploadedFile = new UploadedFile("my-doc.pdf", "my-doc.pdf", "application/pdf", 7L, user, null, null);
        when(fileRepository.findById(100L)).thenReturn(Optional.of(uploadedFile));

        var resource = storageService.loadFileAsResource(user, 100L);
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void messageSenderAndRecipientCanDownloadAttachment() throws Exception {
        User recipient = new User();
        setField(recipient, "id", 9L);
        recipient.setCollege(college);

        Message message = new Message(user, recipient, "Here is the file");
        when(messageRepository.findById(50L)).thenReturn(Optional.of(message));
        when(safetyService.isBlockedEitherWay(anyLong(), anyLong())).thenReturn(false);

        Path physicalFile = tempDir.resolve("shared.pdf");
        Files.writeString(physicalFile, "shared content");

        UploadedFile uploadedFile = new UploadedFile("shared.pdf", "shared.pdf", "application/pdf", 14L, user, "MESSAGE", 50L);
        when(fileRepository.findById(200L)).thenReturn(Optional.of(uploadedFile));

        // 1. Sender (uploader) downloads
        var res1 = storageService.loadFileAsResource(user, 200L);
        assertNotNull(res1);

        // 2. Recipient downloads
        var res2 = storageService.loadFileAsResource(recipient, 200L);
        assertNotNull(res2);

        // 3. Unrelated third party in same college is rejected
        User unrelated = new User();
        setField(unrelated, "id", 77L);
        unrelated.setCollege(college);

        assertThrows(AccessDeniedException.class, () ->
                storageService.loadFileAsResource(unrelated, 200L)
        );
    }

    @Test
    void communityMemberCanDownloadAttachmentWhileNonMemberIsRejected() throws Exception {
        User member = new User();
        setField(member, "id", 15L);
        member.setCollege(college);

        User nonMember = new User();
        setField(nonMember, "id", 16L);
        nonMember.setCollege(college);

        when(communityMemberRepository.existsByCommunityIdAndUserId(30L, 15L)).thenReturn(true);
        when(communityMemberRepository.existsByCommunityIdAndUserId(30L, 16L)).thenReturn(false);

        Path physicalFile = tempDir.resolve("comm.pdf");
        Files.writeString(physicalFile, "comm content");

        UploadedFile uploadedFile = new UploadedFile("comm.pdf", "comm.pdf", "application/pdf", 12L, user, "COMMUNITY", 30L);
        when(fileRepository.findById(300L)).thenReturn(Optional.of(uploadedFile));

        // Member succeeds
        var res = storageService.loadFileAsResource(member, 300L);
        assertNotNull(res);

        // Non-member fails
        assertThrows(AccessDeniedException.class, () ->
                storageService.loadFileAsResource(nonMember, 300L)
        );
    }

    @Test
    void crossCollegeUserIsRejected() throws Exception {
        College otherCollege = new College();
        setField(otherCollege, "id", 2L);

        User crossCollegeUser = new User();
        setField(crossCollegeUser, "id", 99L);
        crossCollegeUser.setCollege(otherCollege);

        UploadedFile uploadedFile = new UploadedFile("file.pdf", "file.pdf", "application/pdf", 10L, user, "COMMUNITY", 30L);
        when(fileRepository.findById(400L)).thenReturn(Optional.of(uploadedFile));

        assertThrows(AccessDeniedException.class, () ->
                storageService.loadFileAsResource(crossCollegeUser, 400L)
        );
    }

    @Test
    void messageRecipientMustHaveActiveConnectionToDownloadAttachment() throws Exception {
        User recipient = new User();
        setField(recipient, "id", 20L);
        recipient.setCollege(college);

        UploadedFile uploadedFile = new UploadedFile("msg.pdf", "msg.pdf", "application/pdf", 10L, user, "MESSAGE", 20L);
        when(fileRepository.findById(500L)).thenReturn(Optional.of(uploadedFile));
        when(safetyService.isBlockedEitherWay(20L, 5L)).thenReturn(false);
        when(connectionRequestRepository.existsAcceptedBetween(20L, 5L)).thenReturn(false);

        // Denied when not connected
        assertThrows(AccessDeniedException.class, () ->
                storageService.loadFileAsResource(recipient, 500L)
        );

        // Allowed when accepted connection exists
        Path physicalFile = tempDir.resolve("msg.pdf");
        Files.writeString(physicalFile, "message attachment content");
        when(connectionRequestRepository.existsAcceptedBetween(20L, 5L)).thenReturn(true);
        var res = storageService.loadFileAsResource(recipient, 500L);
        assertNotNull(res);
    }

    @Test
    void deleteFileByUploaderRemovesRecordAndPhysicalFile() throws Exception {
        Path physicalFile = tempDir.resolve("to_delete.txt");
        Files.writeString(physicalFile, "delete me");

        UploadedFile uploadedFile = new UploadedFile("to_delete.txt", "to_delete.txt", "text/plain", 9L, user, null, null);
        when(fileRepository.findById(600L)).thenReturn(Optional.of(uploadedFile));

        assertTrue(Files.exists(physicalFile));
        storageService.deleteFile(user, 600L);

        assertFalse(Files.exists(physicalFile));
        verify(fileRepository).delete(uploadedFile);
    }

    @Test
    void deleteFileByNonUploaderIsRejected() throws Exception {
        User otherUser = new User();
        setField(otherUser, "id", 88L);
        otherUser.setCollege(college);

        UploadedFile uploadedFile = new UploadedFile("file.txt", "file.txt", "text/plain", 9L, user, null, null);
        when(fileRepository.findById(601L)).thenReturn(Optional.of(uploadedFile));

        assertThrows(AccessDeniedException.class, () ->
                storageService.deleteFile(otherUser, 601L)
        );
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void deleteFileWithActiveMessageAssociationIsRejected() throws Exception {
        Path physicalFile = tempDir.resolve("msg_attachment.txt");
        Files.writeString(physicalFile, "message attachment content");

        UploadedFile uploadedFile = new UploadedFile("msg_attachment.txt", "msg_attachment.txt", "text/plain", 26L, user, "MESSAGE", 55L);
        when(fileRepository.findById(602L)).thenReturn(Optional.of(uploadedFile));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                storageService.deleteFile(user, 602L)
        );
        assertTrue(ex.getMessage().contains("active message association"));
        assertTrue(Files.exists(physicalFile), "Physical file must remain intact when deletion is blocked");
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void deleteFileWithActiveCommunityAssociationIsRejected() throws Exception {
        Path physicalFile = tempDir.resolve("community_attachment.png");
        Files.write(physicalFile, new byte[]{1, 2, 3});

        UploadedFile uploadedFile = new UploadedFile("community_attachment.png", "community_attachment.png", "image/png", 3L, user, "COMMUNITY", 77L);
        when(fileRepository.findById(603L)).thenReturn(Optional.of(uploadedFile));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                storageService.deleteFile(user, 603L)
        );
        assertTrue(ex.getMessage().contains("active community association"));
        assertTrue(Files.exists(physicalFile), "Physical file must remain intact when deletion is blocked");
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void deleteFileBlockedLeavesDatabaseRecordAndPhysicalFileIntact() throws Exception {
        Path physicalFile = tempDir.resolve("protected_file.pdf");
        Files.writeString(physicalFile, "%PDF-protected");

        UploadedFile uploadedFile = new UploadedFile("protected_file.pdf", "protected_file.pdf", "application/pdf", 14L, user, "MESSAGE", 999L);
        when(fileRepository.findById(604L)).thenReturn(Optional.of(uploadedFile));

        assertThrows(IllegalStateException.class, () ->
                storageService.deleteFile(user, 604L)
        );

        assertTrue(Files.exists(physicalFile), "Physical file must not be removed");
        verify(fileRepository, never()).delete(uploadedFile);
    }
}
