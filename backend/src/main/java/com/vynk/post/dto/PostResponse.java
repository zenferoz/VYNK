package com.vynk.post.dto;

import com.vynk.post.entity.Post;
import java.time.LocalDateTime;

public class PostResponse {

    private Long id;
    private Long authorId;
    private String authorName;
    private String authorDepartment;
    private String authorCourse;
    private Integer authorYear;
    private String content;
    private boolean mine;
    private LocalDateTime createdAt;

    public PostResponse() {
    }

    public PostResponse(
            Post post,
            String authorName,
            String authorDepartment,
            String authorCourse,
            Integer authorYear,
            Long currentUserId) {
        this.id = post.getId();
        this.authorId = post.getAuthor().getId();
        this.authorName = authorName;
        this.authorDepartment = authorDepartment;
        this.authorCourse = authorCourse;
        this.authorYear = authorYear;
        this.content = post.getContent();
        this.mine = post.getAuthor().getId().equals(currentUserId);
        this.createdAt = post.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAuthorCourse() {
        return authorCourse;
    }

    public void setAuthorCourse(String authorCourse) {
        this.authorCourse = authorCourse;
    }

    public Integer getAuthorYear() {
        return authorYear;
    }

    public void setAuthorYear(Integer authorYear) {
        this.authorYear = authorYear;
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
