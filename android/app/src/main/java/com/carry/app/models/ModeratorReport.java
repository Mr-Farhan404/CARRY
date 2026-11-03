package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class ModeratorReport {
    @SerializedName("id")
    private Long id;

    @SerializedName("moderatorId")
    private Long moderatorId;

    @SerializedName("moderatorName")
    private String moderatorName;

    @SerializedName("actionType")
    private String actionType;

    @SerializedName("description")
    private String description;

    @SerializedName("targetId")
    private Long targetId;

    @SerializedName("createdAt")
    private String createdAt;

    public ModeratorReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getModeratorId() { return moderatorId; }
    public void setModeratorId(Long moderatorId) { this.moderatorId = moderatorId; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
