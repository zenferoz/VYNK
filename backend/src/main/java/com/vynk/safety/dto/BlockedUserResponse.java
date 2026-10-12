package com.vynk.safety.dto;

import com.vynk.safety.entity.UserBlock;
import java.time.LocalDateTime;

public class BlockedUserResponse {

    private Long id;
    private Long blockedUserId;
    private LocalDateTime createdAt;

    public BlockedUserResponse() {
    }

    public BlockedUserResponse(UserBlock block) {
        this.id = block.getId();
        this.blockedUserId = block.getBlocked().getId();
        this.createdAt = block.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getBlockedUserId() {
        return blockedUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
