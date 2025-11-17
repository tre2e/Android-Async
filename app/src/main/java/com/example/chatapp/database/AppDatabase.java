package com.example.chatapp.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.chatapp.dao.MessageDao;
import com.example.chatapp.dao.UserDao;
import com.example.chatapp.models.Message;
import com.example.chatapp.models.User;

@Database(entities = {User.class, Message.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract MessageDao messageDao();
}
