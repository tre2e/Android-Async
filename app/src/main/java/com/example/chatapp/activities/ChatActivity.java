package com.example.chatapp.activities;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.App;
import com.example.chatapp.R;
import com.example.chatapp.adapter.MessageAdapter;
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.models.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();

    // 直接使用 App 中全局单例的 OkHttpClient（自动带 Cookie）
    // private final OkHttpClient client = new OkHttpClient();   ← 删除这行

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private AppDatabase db;
    private final long currentUserId = 1L; // 实际项目中从登录信息获取

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = App.getDatabase();  // 正确方式获取数据库

        initViews();
        setupRecyclerView();
        loadAllMessages();

        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. 本地立即显示
            Message localMsg = new Message();
            localMsg.setSenderId(currentUserId);
            localMsg.setMessage(text);
            localMsg.setSentAt(System.currentTimeMillis());
            localMsg.setIsSynced(0);

            executor.execute(() -> {
                db.messageDao().insert(localMsg);
                runOnUiThread(this::loadLocalMessagesOnly);
            });

            editTextMessage.setText("");
            // 2. 后台上传
            uploadMessage(text);
        });
    }

    private void initViews() {
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(lm);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void uploadMessage(String content) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("message", content);

                Request request = new Request.Builder()
                        .url("http://47.100.72.149:8081/message")
                        .post(RequestBody.create(json.toString(), JSON_TYPE))
                        .build();

                try (Response response = App.client.newCall(request).execute()) {
                    boolean success = response.isSuccessful();
                    if (success) {
                        db.messageDao().markLastUnsyncedAsSynced(currentUserId);
                    }
                    String tip = success ? "发送成功" : "发送失败（已保存本地）";
                    uiHandler.post(() -> Toast.makeText(this, tip, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(this, "网络错误，消息已保存本地", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadLocalMessagesOnly() {
        executor.execute(() -> {
            List<Message> local = db.messageDao().getAllMessagesOrdered();
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(local);
                messageAdapter.updateMessages(local);
                recyclerViewMessages.scrollToPosition(local.size() - 1);
            });
        });
    }

    private void loadAllMessages() {
        executor.execute(() -> {
            List<Message> server = fetchMessagesFromServer();

            if (server != null) {
                db.messageDao().deleteAllSynced();
                for (Message m : server) {
                    m.setIsSynced(1);
                    db.messageDao().insert(m);
                }
            }

            List<Message> finalList = db.messageDao().getAllMessagesOrdered();
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(finalList);
                messageAdapter.updateMessages(finalList);
                recyclerViewMessages.scrollToPosition(finalList.size() - 1);
            });
        });
    }

    private List<Message> fetchMessagesFromServer() {
        try {
            Request request = new Request.Builder()
                    .url("http://47.100.72.149:8081/messages")
                    .get()
                    .build();

            try (Response response = App.client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                return parseMessagesJson(response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Message> parseMessagesJson(String json) {
        List<Message> list = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                Message msg = new Message();
                msg.setSenderId(obj.getLong("senderId"));
                msg.setMessage(obj.getString("message"));

                String timeStr = obj.optString("sentAt", null);
                if (timeStr != null && !timeStr.trim().isEmpty()) {
                    try {
                        Date date = sdf.parse(timeStr);
                        if (date != null) msg.setSentAt(date.getTime());
                    } catch (ParseException ignored) { }
                }
                if (msg.getSentAt() == 0) msg.setSentAt(System.currentTimeMillis());

                msg.setIsSynced(1);
                list.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}