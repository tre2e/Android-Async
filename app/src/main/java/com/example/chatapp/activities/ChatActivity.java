package com.example.chatapp.activities;

import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.chatapp.R;
import com.example.chatapp.adapter.MessageAdapter;
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.models.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_COOKIE = "session_cookie";
    AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "chat").build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化控件
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        // 初始化消息列表和适配器
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerViewMessages.setAdapter(messageAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 从底部开始显示
        recyclerViewMessages.setLayoutManager(layoutManager);

        // 加载聊天消息
        new FetchMessagesTask().execute();

        // 发送按钮点击事件
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editTextMessage.getText().toString().trim();
                if (message.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "请输入消息", Toast.LENGTH_SHORT).show();
                    return;
                }
                new SendMessageTask().execute(message);
                editTextMessage.setText(""); // 清空输入框
            }
        });
    }

    // 异步任务：获取所有消息
    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String urlString = "http://10.0.2.2:8081/messages";
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // 添加 session cookie
                conn = LoginActivity.addSessionCookie(conn, ChatActivity.this);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    return response.toString();
                } else {
                    return "获取消息失败，响应码: " + responseCode;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "获取消息失败: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.startsWith("获取消息失败")) {
                Toast.makeText(ChatActivity.this, result, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                // 假设服务器返回的 DATETIME 格式为 "2025-11-16 15:19:36"
                // ⚠️ 格式必须严格匹配服务器返回的字符串
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                // 解析 JSON 数组
                JSONArray jsonArray = new JSONArray(result);
                List<Message> messages = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    Message message = new Message();

                    // 核心逻辑保持不变
                    message.setSenderId(json.getLong("senderId"));
                    message.setMessage(json.getString("message"));

                    // 🚀 错误解决点：类型转换
                    String sentAtString = json.optString("sentAt", ""); // 获取字符串
                    long sentAtLong = 0L;

                    if (!sentAtString.isEmpty()) {
                        try {
                            Date date = formatter.parse(sentAtString);
                            sentAtLong = date.getTime(); // 转换为 long 毫秒时间戳
                        } catch (ParseException e) {
                            // 如果日期格式解析失败，打印错误，使用默认值 0L
                            e.printStackTrace();
                        }
                    }

                    message.setSentAt(sentAtLong); // 传入 long 类型
                    messages.add(message);
                }

                // 更新 RecyclerView
                messageAdapter.updateMessages(messages);
                recyclerViewMessages.scrollToPosition(messages.size() - 1);

            } catch (Exception e) {
                e.printStackTrace();
                // 统一处理 JSON 或其他异常
                Toast.makeText(ChatActivity.this, "处理消息失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // 异步任务：发送消息
    private class SendMessageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String message = params[0];
            String urlString = "http://10.0.2.2:8081/message";
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                // 添加 session cookie
                conn = LoginActivity.addSessionCookie(conn, ChatActivity.this);

                // 构造 JSON 请求
                JSONObject json = new JSONObject();
                json.put("message", message);
                String postData = json.toString();
                byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

                // 发送请求
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(postDataBytes);
                os.flush();
                os.close();

                // 获取响应
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    return response.toString();
                } else {
                    return "发送消息失败，响应码: " + responseCode;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "发送消息失败: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(ChatActivity.this, result, Toast.LENGTH_LONG).show();
            if (result.equals("Send message successfully! ")) {
                // 刷新消息列表
                new FetchMessagesTask().execute();
            }
        }
    }
}
