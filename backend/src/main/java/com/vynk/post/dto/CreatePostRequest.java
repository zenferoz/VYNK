package com.vynk.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePostRequest {

    @NotBlank(message = "Post content cannot be blank.")
    @Size(min = 1, max = 2000, message = "Post content must be between 1 and 2000 characters.")
    private String content;

    public CreatePostRequest() {
    }

    public CreatePostRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
