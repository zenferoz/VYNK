package com.vynk.connection.dto;

import com.vynk.connection.entity.ConnectionStatus;
import java.time.LocalDateTime;

public class ConnectionResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String department;
    private Integer currentYear;
    private ConnectionStatus status;
    private String purposeNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String direction;

    public ConnectionResponse() {
    }

    public ConnectionResponse(
            Long id,
            Long userId,
            String fullName,
            String department,
            Integer currentYear,
            ConnectionStatus status,
            String purposeNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String direction
    ) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.department = department;
        this.currentYear = currentYear;
        this.status = status;
        this.purposeNote = purposeNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.direction = direction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public String getPurposeNote() {
        return purposeNote;
    }

    public void setPurposeNote(String purposeNote) {
        this.purposeNote = purposeNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
