package com.vynk.message.dto;

import java.time.LocalDateTime;

public class ConversationSummaryResponse {

    private Long peerId;
    private String peerName;
    private String peerDepartment;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public ConversationSummaryResponse() {
    }

    public ConversationSummaryResponse(
            Long peerId,
            String peerName,
            String peerDepartment,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.peerDepartment = peerDepartment;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public Long getPeerId() {
        return peerId;
    }

    public void setPeerId(Long peerId) {
        this.peerId = peerId;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public String getPeerDepartment() {
        return peerDepartment;
    }

    public void setPeerDepartment(String peerDepartment) {
        this.peerDepartment = peerDepartment;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
