package com.vynk.profile.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentProfileResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String department;
    private String course;
    private Integer currentYear;
    private String bio;
    private boolean discoverable;
    private boolean onboardingCompleted;
    private String availability;
    private List<String> skills = new ArrayList<>();
    private List<String> interests = new ArrayList<>();

    public StudentProfileResponse() {
    }

    public StudentProfileResponse(
            Long id,
            Long userId,
            String fullName,
            String department,
            String course,
            Integer currentYear,
            String bio,
            boolean discoverable,
            boolean onboardingCompleted) {
        this(id, userId, fullName, department, course, currentYear, bio, discoverable, onboardingCompleted, null, null, null);
    }

    public StudentProfileResponse(
            Long id,
            Long userId,
            String fullName,
            String department,
            String course,
            Integer currentYear,
            String bio,
            boolean discoverable,
            boolean onboardingCompleted,
            String availability,
            List<String> skills,
            List<String> interests) {

        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.department = department;
        this.course = course;
        this.currentYear = currentYear;
        this.bio = bio;
        this.discoverable = discoverable;
        this.onboardingCompleted = onboardingCompleted;
        this.availability = availability;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.interests = interests != null ? interests : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public String getCourse() {
        return course;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public String getBio() {
        return bio;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests != null ? interests : new ArrayList<>();
    }
}