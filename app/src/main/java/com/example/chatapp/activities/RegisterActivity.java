package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private TextView textViewResult;
    private Button buttonRegister;

    // 单线程池（也可以根据需要改成固定线程数的池）
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final OkHttpClient client = new OkHttpClient();

    // Handler 运行在主线程
    private final Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        String result = msg.getData().getString("result");
        textViewResult.setText(result);
        Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_LONG).show();

        if (result != null && result.startsWith("success")) {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(RegisterActivity.this, "注册失败，请检查用户名或密码。", Toast.LENGTH_LONG).show();
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        textViewResult = findViewById(R.id.textViewResult);
        buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            register(username, password);
        });
    }

    private void register(String username, String password) {
        executor.execute(() -> {
            String result;
            try {
                RequestBody body = new FormBody.Builder()
                        .add("username", username)
                        .add("password", password)
                        .build();

                Request request = new Request.Builder()
                        .url("http://47.100.72.149:8081/insert")   // 你的服务器地址
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        result = response.body().string();
                    } else {
                        result = "注册失败，服务器响应码: " + response.code();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = "注册失败: " + e.getMessage();
            }

            // 把结果发送回主线程
            Message msg = uiHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("result", result);
            msg.setData(bundle);
            uiHandler.sendMessage(msg);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();   // 活动销毁时关闭线程池
        client.dispatcher().executorService().shutdown();
    }
}