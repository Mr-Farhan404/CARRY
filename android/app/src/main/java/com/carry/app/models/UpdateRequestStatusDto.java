package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class UpdateRequestStatusDto {
    @SerializedName("status")
    private RequestStatus status;

    @SerializedName("note")
    private String note;

    public UpdateRequestStatusDto() {}

    public UpdateRequestStatusDto(RequestStatus status, String note) {
        this.status = status;
        this.note = note;
    }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
