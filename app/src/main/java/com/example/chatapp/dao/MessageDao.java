package com.example.chatapp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.chatapp.models.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(Message message);

    @Query("SELECT * FROM messages ORDER BY sent_at ASC")
    List<Message> getAllMessagesOrdered();

    @Query("DELETE FROM messages WHERE isSynced = 1")
    void deleteAllSynced();

    @Query("UPDATE messages SET isSynced = 1 WHERE sender_id = :senderId AND isSynced = 0")
    void markLastUnsyncedAsSynced(long senderId);
}
