package com.vynk.user.dto;

public class UserResponse {

    private Long id;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
    private Long collegeId;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String email,
            String role,
            String status,
            boolean emailVerified,
            Long collegeId) {

        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.collegeId = collegeId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Long getCollegeId() {
        return collegeId;
    }
}