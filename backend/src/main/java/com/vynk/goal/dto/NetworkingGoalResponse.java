package com.vynk.goal.dto;

import com.vynk.goal.entity.NetworkingGoal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetworkingGoalResponse {

    private Long id;
    private Long userId;
    private String purpose;
    private String purposeDisplayName;
    private String description;
    private List<String> skills = new ArrayList<>();
    private Integer targetYear;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public NetworkingGoalResponse() {
    }

    public NetworkingGoalResponse(NetworkingGoal goal) {
        this.id = goal.getId();
        this.userId = goal.getUser().getId();
        this.purpose = goal.getPurpose().name();
        this.purposeDisplayName = goal.getPurpose().getDisplayName();
        this.description = goal.getDescription();
        this.skills = new ArrayList<>(goal.getSkills());
        this.targetYear = goal.getTargetYear();
        this.active = goal.isActive();
        this.createdAt = goal.getCreatedAt();
        this.expiresAt = goal.getExpiresAt();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getPurposeDisplayName() {
        return purposeDisplayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getSkills() {
        return skills;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
