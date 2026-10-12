package com.vynk.goal.entity;

import com.vynk.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "networking_goals", indexes = {
    @Index(name = "idx_goal_user_active", columnList = "user_id, active")
})
public class NetworkingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NetworkingPurpose purpose;

    @Column(length = 255)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "networking_goal_skills", joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "skill", length = 50, nullable = false)
    private Set<String> skills = new LinkedHashSet<>();

    @Column
    private Integer targetYear;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime expiresAt;

    public NetworkingGoal() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills != null ? skills : new LinkedHashSet<>();
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
