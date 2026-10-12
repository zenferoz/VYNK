package com.vynk.goal.entity;

public enum NetworkingPurpose {
    PROJECT_COLLABORATION("Project Collaboration"),
    HACKATHON_TEAM("Hackathon Team"),
    STUDY_PARTNER("Study Partner"),
    RESEARCH("Academic Research"),
    PLACEMENT_PREP("Placement & Interview Prep"),
    CASUAL_NETWORKING("Campus Networking");

    private final String displayName;

    NetworkingPurpose(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
