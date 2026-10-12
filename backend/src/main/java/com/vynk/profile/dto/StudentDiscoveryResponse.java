package com.vynk.profile.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentDiscoveryResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String department;
    private String course;
    private Integer currentYear;
    private String bio;
    private String collegeName;
    private String availability;
    private List<String> skills = new ArrayList<>();
    private List<String> interests = new ArrayList<>();

    public StudentDiscoveryResponse() {
    }

    public StudentDiscoveryResponse(
            Long id,
            Long userId,
            String fullName,
            String department,
            String course,
            Integer currentYear,
            String bio,
            String collegeName) {
        this(id, userId, fullName, department, course, currentYear, bio, collegeName, null, null, null);
    }

    public StudentDiscoveryResponse(
            Long id,
            Long userId,
            String fullName,
            String department,
            String course,
            Integer currentYear,
            String bio,
            String collegeName,
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
        this.collegeName = collegeName;
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

    public String getCollegeName() {
        return collegeName;
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
