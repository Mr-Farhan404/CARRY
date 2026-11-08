package com.carry.app.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {
    private String id;
    private String senderUid;
    private String senderName;
    private String text;

    @ServerTimestamp
    private Date createdAt;

    // Convenience flag for adapter rendering (not stored in Firestore)
    @Exclude
    private boolean isMine;

    public ChatMessage() {}

    public ChatMessage(String id, String senderUid, String senderName, String text, Date createdAt) {
        this.id = id;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName != null ? senderName : "User";
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text != null ? text : "";
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public boolean isMine() {
        return isMine;
    }

    @Exclude
    public void setMine(boolean mine) {
        isMine = mine;
    }
}
