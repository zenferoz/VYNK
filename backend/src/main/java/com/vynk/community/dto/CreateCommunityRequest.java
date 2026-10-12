package com.vynk.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommunityRequest {

    @NotBlank(message = "Community name is required.")
    @Size(min = 3, max = 100, message = "Community name must be between 3 and 100 characters.")
    private String name;

    @NotBlank(message = "Description is required.")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
    private String description;

    @NotBlank(message = "Purpose is required.")
    @Size(max = 50, message = "Purpose cannot exceed 50 characters.")
    private String purpose;

    public CreateCommunityRequest() {
    }

    public CreateCommunityRequest(String name, String description, String purpose) {
        this.name = name;
        this.description = description;
        this.purpose = purpose;
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
}
