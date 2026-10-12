package com.vynk.safety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateReportRequest {

    @NotBlank(message = "Report reason is required")
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public CreateReportRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
