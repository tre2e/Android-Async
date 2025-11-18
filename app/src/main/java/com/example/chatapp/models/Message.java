package com.example.chatapp.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "sender_id")
    private long senderId;
    @ColumnInfo(name = "message")
    private String message;
    @ColumnInfo(name = "sent_at")
    private long sentAt;
    @ColumnInfo(name = "isSynced")
    private int isSynced;

    public int getId() {
        return id;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public int getIsSynced() {
        return isSynced;
    }

    public void setIsSynced(int isSynced) {
        this.isSynced = isSynced;
    }
}
