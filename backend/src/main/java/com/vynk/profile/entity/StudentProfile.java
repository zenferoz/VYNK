package com.vynk.profile.entity;

import com.vynk.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String course;

    @Column(nullable = false)
    private Integer currentYear;

    @Column(length = 1000)
    private String bio;

    @Column(nullable = false)
    private boolean discoverable = true;

    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    @Column(length = 200)
    private String availability;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "student_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", length = 50, nullable = false)
    private java.util.Set<String> skills = new java.util.LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "student_profile_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "interest", length = 50, nullable = false)
    private java.util.Set<String> interests = new java.util.LinkedHashSet<>();

    public StudentProfile() {
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

    public boolean isDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public java.util.Set<String> getSkills() {
        return skills;
    }

    public void setSkills(java.util.Set<String> skills) {
        this.skills = skills != null ? skills : new java.util.LinkedHashSet<>();
    }

    public java.util.Set<String> getInterests() {
        return interests;
    }

    public void setInterests(java.util.Set<String> interests) {
        this.interests = interests != null ? interests : new java.util.LinkedHashSet<>();
    }
}