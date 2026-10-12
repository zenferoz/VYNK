package com.vynk.community.dto;

import com.vynk.community.entity.CommunityDiscussion;
import java.time.LocalDateTime;

public class DiscussionResponse {

    private Long id;
    private Long communityId;
    private Long authorId;
    private String authorName;
    private String authorDepartment;
    private String title;
    private String content;
    private boolean mine;
    private LocalDateTime createdAt;

    public DiscussionResponse() {
    }

    public DiscussionResponse(CommunityDiscussion d, String authorName, String authorDepartment, Long currentUserId) {
        this.id = d.getId();
        this.communityId = d.getCommunity().getId();
        this.authorId = d.getAuthor().getId();
        this.authorName = authorName;
        this.authorDepartment = authorDepartment;
        this.title = d.getTitle();
        this.content = d.getContent();
        this.mine = d.getAuthor().getId().equals(currentUserId);
        this.createdAt = d.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorDepartment() {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment) {
        this.authorDepartment = authorDepartment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
