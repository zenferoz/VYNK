package com.vynk.college.dto;

public class CollegeResponse {

    private Long id;
    private String name;
    private String emailDomain;
    private boolean active;

    public CollegeResponse() {
    }

    public CollegeResponse(Long id, String name, String emailDomain, boolean active) {
        this.id = id;
        this.name = name;
        this.emailDomain = emailDomain;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public boolean isActive() {
        return active;
    }
}
