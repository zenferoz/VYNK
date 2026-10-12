package com.vynk.safety.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.safety.entity.UserBlock;
import com.vynk.safety.entity.UserReport;
import com.vynk.safety.repository.UserBlockRepository;
import com.vynk.safety.repository.UserReportRepository;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SafetyService {

    private final UserBlockRepository blockRepository;
    private final UserReportRepository reportRepository;
    private final UserRepository userRepository;

    public SafetyService(
            UserBlockRepository blockRepository,
            UserReportRepository reportRepository,
            UserRepository userRepository) {
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void blockUser(User blocker, Long targetUserId) {
        if (blocker.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot block your own profile.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        if (!blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), targetUserId)) {
            blockRepository.save(new UserBlock(blocker, targetUser));
        }
    }

    @Transactional
    public void unblockUser(User blocker, Long targetUserId) {
        blockRepository.findByBlockerIdAndBlockedId(blocker.getId(), targetUserId)
                .ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(Long userA, Long userB) {
        if (userA == null || userB == null) return false;
        return blockRepository.existsBlockBetween(userA, userB);
    }

    @Transactional(readOnly = true)
    public Set<Long> getAllBlockedUserIdsFor(Long userId) {
        Set<Long> blockedByMe = blockRepository.findBlockedUserIdsByBlockerId(userId);
        Set<Long> whoBlockedMe = blockRepository.findBlockerUserIdsByBlockedId(userId);
        Set<Long> allExcluded = new HashSet<>(blockedByMe);
        allExcluded.addAll(whoBlockedMe);
        return allExcluded;
    }

    @Transactional(readOnly = true)
    public List<UserBlock> getBlockedUsersBy(Long blockerId) {
        return blockRepository.findByBlockerId(blockerId);
    }

    @Transactional
    public void reportUser(User reporter, Long targetUserId, String reason, String description) {
        if (reporter.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot report your own profile.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        // Enforce same-college policy
        if (!reporter.getCollege().getId().equals(targetUser.getCollege().getId())) {
            throw new IllegalArgumentException("You can only report students within your college network.");
        }

        // Prevent duplicate spam within 24 hours
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        if (reportRepository.existsByReporterIdAndReportedUserIdAndCreatedAtAfter(reporter.getId(), targetUserId, last24h)) {
            throw new IllegalArgumentException("You have already submitted a report for this user recently.");
        }

        String cleanReason = reason != null ? reason.trim() : "INAPPROPRIATE_BEHAVIOR";
        String cleanDesc = description != null ? description.trim() : "";
        if (cleanDesc.length() > 500) {
            cleanDesc = cleanDesc.substring(0, 500);
        }

        reportRepository.save(new UserReport(reporter, targetUser, cleanReason, cleanDesc));
    }
}
