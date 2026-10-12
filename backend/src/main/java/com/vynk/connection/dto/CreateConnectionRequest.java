package com.vynk.connection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateConnectionRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @Size(max = 300, message = "Purpose note must not exceed 300 characters")
    private String purposeNote;

    public CreateConnectionRequest() {
    }

    public CreateConnectionRequest(Long receiverId, String purposeNote) {
        this.receiverId = receiverId;
        this.purposeNote = purposeNote;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getPurposeNote() {
        return purposeNote;
    }

    public void setPurposeNote(String purposeNote) {
        this.purposeNote = purposeNote;
    }
}
