package com.vynk.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDiscussionRequest {

    @NotBlank(message = "Title is required.")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(min = 5, max = 4000, message = "Content must be between 5 and 4000 characters.")
    private String content;

    public CreateDiscussionRequest() {
    }

    public CreateDiscussionRequest(String title, String content) {
        this.title = title;
        this.content = content;
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
}
