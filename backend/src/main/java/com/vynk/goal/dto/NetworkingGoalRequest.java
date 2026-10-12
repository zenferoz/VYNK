package com.vynk.goal.dto;

import com.vynk.goal.entity.NetworkingPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetworkingGoalRequest {

    @NotNull(message = "Networking purpose is required")
    private NetworkingPurpose purpose;

    @Size(max = 255, message = "Goal description must not exceed 255 characters")
    private String description;

    private List<String> skills = new ArrayList<>();

    @Min(value = 1, message = "Target year must be between 1 and 6")
    @Max(value = 6, message = "Target year must be between 1 and 6")
    private Integer targetYear;

    private Boolean active;

    private LocalDateTime expiresAt;

    public NetworkingGoalRequest() {
    }

    public NetworkingPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(NetworkingPurpose purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
