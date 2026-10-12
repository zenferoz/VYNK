package com.vynk;

import com.vynk.college.entity.College;
import com.vynk.safety.repository.UserBlockRepository;
import com.vynk.safety.repository.UserReportRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SafetyServiceTest {

    private UserBlockRepository blockRepository;
    private UserReportRepository reportRepository;
    private UserRepository userRepository;
    private SafetyService safetyService;

    private College college;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() throws Exception {
        blockRepository = Mockito.mock(UserBlockRepository.class);
        reportRepository = Mockito.mock(UserReportRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        safetyService = new SafetyService(blockRepository, reportRepository, userRepository);

        college = new College();
        setField(college, "id", 1L);

        userA = new User();
        setField(userA, "id", 10L);
        userA.setCollege(college);

        userB = new User();
        setField(userB, "id", 20L);
        userB.setCollege(college);

        when(userRepository.findById(20L)).thenReturn(Optional.of(userB));
    }

    @Test
    void shouldPreventSelfBlocking() {
        assertThrows(IllegalArgumentException.class, () -> safetyService.blockUser(userA, 10L));
    }

    @Test
    void shouldPreventSelfReporting() {
        assertThrows(IllegalArgumentException.class, () -> safetyService.reportUser(userA, 10L, "SPAM", "details"));
    }

    @Test
    void shouldReturnCombinedTwoWayBlockedIds() {
        when(blockRepository.findBlockedUserIdsByBlockerId(10L)).thenReturn(Set.of(20L));
        when(blockRepository.findBlockerUserIdsByBlockedId(10L)).thenReturn(Set.of(30L));

        Set<Long> blockedIds = safetyService.getAllBlockedUserIdsFor(10L);
        assertEquals(2, blockedIds.size());
        assertTrue(blockedIds.contains(20L));
        assertTrue(blockedIds.contains(30L));
    }

    @Test
    void shouldPreventDuplicateReportSpam() {
        when(reportRepository.existsByReporterIdAndReportedUserIdAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> safetyService.reportUser(userA, 20L, "HARASSMENT", "Test"));
        assertTrue(ex.getMessage().contains("already submitted a report"));
    }

    @Test
    void shouldBlockUserSuccessfullyWhenNotAlreadyBlocked() {
        when(blockRepository.existsByBlockerIdAndBlockedId(10L, 20L)).thenReturn(false);

        safetyService.blockUser(userA, 20L);

        Mockito.verify(blockRepository, Mockito.times(1)).save(any());
    }

    @Test
    void shouldBeIdempotentWhenBlockingAlreadyBlockedUser() {
        when(blockRepository.existsByBlockerIdAndBlockedId(10L, 20L)).thenReturn(true);

        safetyService.blockUser(userA, 20L);

        // Does not save a duplicate
        Mockito.verify(blockRepository, Mockito.never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenBlockingNonexistentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.vynk.common.exception.ResourceNotFoundException.class,
                () -> safetyService.blockUser(userA, 999L));
    }

    @Test
    void shouldUnblockUserSuccessfully() {
        com.vynk.safety.entity.UserBlock block = new com.vynk.safety.entity.UserBlock(userA, userB);
        when(blockRepository.findByBlockerIdAndBlockedId(10L, 20L)).thenReturn(Optional.of(block));

        safetyService.unblockUser(userA, 20L);

        Mockito.verify(blockRepository, Mockito.times(1)).delete(block);
    }

    @Test
    void shouldBeIdempotentWhenUnblockingNotBlockedUser() {
        when(blockRepository.findByBlockerIdAndBlockedId(10L, 20L)).thenReturn(Optional.empty());

        safetyService.unblockUser(userA, 20L);

        Mockito.verify(blockRepository, Mockito.never()).delete(any());
    }

    @Test
    void shouldEnforceSameCollegeWhenReporting() throws Exception {
        College college2 = new College();
        setField(college2, "id", 2L);
        userB.setCollege(college2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> safetyService.reportUser(userA, 20L, "SPAM", "details"));
        assertTrue(ex.getMessage().contains("college network"));
    }

    @Test
    void shouldTruncateDescriptionTo500CharsWhenReporting() {
        String longDesc = "A".repeat(600);

        safetyService.reportUser(userA, 20L, "INAPPROPRIATE_BEHAVIOR", longDesc);

        Mockito.verify(reportRepository).save(Mockito.argThat(report ->
                report.getDescription().length() == 500
        ));
    }

    @Test
    void shouldCheckBlockedEitherWay() {
        when(blockRepository.existsBlockBetween(10L, 20L)).thenReturn(true);
        assertTrue(safetyService.isBlockedEitherWay(10L, 20L));

        when(blockRepository.existsBlockBetween(10L, 30L)).thenReturn(false);
        assertFalse(safetyService.isBlockedEitherWay(10L, 30L));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
