package com.vynk.message.dto;

import com.vynk.message.entity.Message;
import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private Long recipientId;
    private String content;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private boolean mine;

    public MessageResponse() {
    }

    public MessageResponse(Message message, Long currentUserId) {
        this.id = message.getId();
        this.senderId = message.getSender().getId();
        this.recipientId = message.getRecipient().getId();
        this.content = message.getContent();
        this.readAt = message.getReadAt();
        this.createdAt = message.getCreatedAt();
        this.mine = message.getSender().getId().equals(currentUserId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }
}
