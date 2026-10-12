package com.vynk.matching.dto;

import java.util.List;

public class StudentMatchResponse {

    private Long userId;
    private String fullName;
    private String department;
    private String course;
    private Integer currentYear;
    private String bio;
    private List<String> skills;
    private List<String> interests;
    private String availability;
    private List<String> activePurposes;
    private int matchScore;
    private List<String> matchReasons;
    private boolean semanticAssisted;

    public StudentMatchResponse() {
    }

    public StudentMatchResponse(
            Long userId,
            String fullName,
            String department,
            String course,
            Integer currentYear,
            String bio,
            List<String> skills,
            List<String> interests,
            String availability,
            List<String> activePurposes,
            int matchScore,
            List<String> matchReasons
    ) {
        this.userId = userId;
        this.fullName = fullName;
        this.department = department;
        this.course = course;
        this.currentYear = currentYear;
        this.bio = bio;
        this.skills = skills;
        this.interests = interests;
        this.availability = availability;
        this.activePurposes = activePurposes;
        this.matchScore = matchScore;
        this.matchReasons = matchReasons;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public List<String> getActivePurposes() {
        return activePurposes;
    }

    public void setActivePurposes(List<String> activePurposes) {
        this.activePurposes = activePurposes;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons;
    }

    public boolean isSemanticAssisted() {
        return semanticAssisted;
    }

    public void setSemanticAssisted(boolean semanticAssisted) {
        this.semanticAssisted = semanticAssisted;
    }
}
