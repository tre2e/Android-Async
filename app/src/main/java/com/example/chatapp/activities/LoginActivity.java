package com.example.chatapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.chatapp.R;
import com.example.chatapp.database.AppDatabase;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private TextView textViewResult;
    private Button buttonLogin;
    private Button buttonGoToRegister;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_COOKIE = "session_cookie";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 绑定控件
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        textViewResult = findViewById(R.id.textViewResult);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoToRegister = findViewById(R.id.buttonGoToRegister);

        // 登录点击
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                new LoginTask().execute(username,password);
            }
        });

        // 跳转到注册页面
        buttonGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // 异步处理网络请求

    private class LoginTask extends AsyncTask<String, Void, String> {

        private String sessionCookie;  // 保存 session cookie

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            String urlString = "http://10.0.2.2:8081/login"; //设置为你的服务器ip

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "username=" + username + "&password=" + password;
                byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(postDataBytes);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                // 提取 Set-Cookie 头
                List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    sessionCookie = cookies.get(0); // 保存第一个 cookie（通常是 JSESSIONID）
                }

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
                    return "Login failed Status code:" + responseCode;
                }

            } catch (java.net.UnknownHostException e) {
                e.printStackTrace();
                return "Can't Analyze hostname" + e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                return "Login failed" + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            textViewResult.setText(result);
            Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();

            // 如果登录成功，保存 session cookie
            if (result != null && result.startsWith("success")) {
                if (sessionCookie != null) {
                    // 使用 SharedPreferences 保存 cookie
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(KEY_COOKIE, sessionCookie);
                    editor.apply();
                }

                // 可选：跳转到主页面
                Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "登录失败，请检查用户名或密码。", Toast.LENGTH_LONG).show();
            }
        }

    }

    // 工具方法：在后续请求中添加 cookie
    public static HttpURLConnection addSessionCookie(HttpURLConnection conn, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cookie = prefs.getString(KEY_COOKIE, null);
        if (cookie != null) {
            conn.setRequestProperty("Cookie", cookie);
        }
        return conn;
    }
}
