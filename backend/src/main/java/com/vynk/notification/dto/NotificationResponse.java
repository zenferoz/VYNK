package com.vynk.notification.dto;

import com.vynk.notification.entity.Notification;
import com.vynk.notification.entity.NotificationType;
import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String targetUrl;
    private boolean read;
    private Long actorId;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.type = n.getType();
        this.title = n.getTitle();
        this.message = n.getMessage();
        this.targetUrl = n.getTargetUrl();
        this.read = n.isRead();
        this.actorId = n.getActor() != null ? n.getActor().getId() : null;
        this.createdAt = n.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
