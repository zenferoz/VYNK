package com.vynk.notification.service;

import com.vynk.common.exception.ResourceNotFoundException;
import com.vynk.notification.dto.NotificationResponse;
import com.vynk.notification.entity.Notification;
import com.vynk.notification.entity.NotificationType;
import com.vynk.notification.repository.NotificationRepository;
import com.vynk.safety.service.SafetyService;
import com.vynk.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SafetyService safetyService;

    public NotificationService(
            NotificationRepository notificationRepository,
            SafetyService safetyService) {
        this.notificationRepository = notificationRepository;
        this.safetyService = safetyService;
    }

    @Transactional
    public void createNotification(
            User recipient,
            User actor,
            NotificationType type,
            String title,
            String message,
            String targetUrl) {

        if (recipient == null) {
            return;
        }

        // Do not notify self
        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

        // Do not notify if blocked either way
        if (actor != null && safetyService.isBlockedEitherWay(recipient.getId(), actor.getId())) {
            return;
        }

        Notification notification = new Notification(
                recipient,
                actor,
                type,
                title,
                message,
                targetUrl
        );
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(User user, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(50, Math.max(1, size));
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(boundedPage, boundedSize))
                .map(NotificationResponse::new);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user.getId());
    }
}
