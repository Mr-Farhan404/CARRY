package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class StatusUpdate implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("requestId")
    private Long requestId;

    @SerializedName("status")
    private RequestStatus status;

    @SerializedName("note")
    private String note;

    @SerializedName("createdBy")
    private String createdBy;

    @SerializedName("createdAt")
    private String createdAt;

    public StatusUpdate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
