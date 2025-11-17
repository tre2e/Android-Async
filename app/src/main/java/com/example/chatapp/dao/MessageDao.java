package com.example.chatapp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.chatapp.models.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages")
    List<Message> getAll();

    @Insert
    void insertAll(Message... messages);

    @Delete
    void delete(Message message);
}
