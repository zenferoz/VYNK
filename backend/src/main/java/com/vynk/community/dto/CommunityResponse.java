package com.vynk.community.dto;

import com.vynk.community.entity.Community;
import java.time.LocalDateTime;

public class CommunityResponse {

    private Long id;
    private String name;
    private String description;
    private String purpose;
    private Long creatorId;
    private long memberCount;
    private boolean member;
    private LocalDateTime createdAt;

    public CommunityResponse() {
    }

    public CommunityResponse(Community c, long memberCount, boolean member) {
        this.id = c.getId();
        this.name = c.getName();
        this.description = c.getDescription();
        this.purpose = c.getPurpose();
        this.creatorId = c.getCreator().getId();
        this.memberCount = memberCount;
        this.member = member;
        this.createdAt = c.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isMember() {
        return member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
