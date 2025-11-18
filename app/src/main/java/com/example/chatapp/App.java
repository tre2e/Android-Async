package com.example.chatapp;

import android.app.Application;

import androidx.room.Room;

import com.example.chatapp.database.AppDatabase;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class App extends Application {

    private static App appInstance;        // Application 实例
    private static AppDatabase database;   // Room 数据库实例

    // 可被外部调用的 CookieJar（带 clear 方法）
    public static final PersistentCookieJar cookieJar = new PersistentCookieJar();

    // 全局统一的 OkHttpClient（自动管理 Cookie）
    public static final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build();

    @Override
    public void onCreate() {
        super.onCreate();
        appInstance = this;

        database = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat")
                // .allowMainThreadQueries()   // 正式环境请删除这行！
                .fallbackToDestructiveMigration()
                .build();
    }

    // 返回 Application 实例
    public static App getAppInstance() {
        return appInstance;
    }

    // 返回 Room 数据库实例（原来你所有地方用的就是这个）
    public static AppDatabase getDatabase() {
        return database;
    }

    // 登出时清除所有 Cookie
    public static void logout() {
        cookieJar.clear();
    }

    // 独立的持久化 CookieJar（内存版，足够聊天 App 使用）
    public static class PersistentCookieJar implements CookieJar {
        private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), new ArrayList<>(cookies));
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<>();
        }

        public void clear() {
            cookieStore.clear();
        }
    }
}