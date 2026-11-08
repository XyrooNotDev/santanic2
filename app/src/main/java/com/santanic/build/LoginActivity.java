package com.santanic.build;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.santanic.build.api.GitHubAuthManager;
import com.santanic.build.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView tvDeviceCode, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        tvDeviceCode = findViewById(R.id.tvDeviceCode);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnLogin = findViewById(R.id.btnLoginGithub);

        btnLogin.setOnClickListener(v -> {
            tvStatus.setText("Meminta kode...");
            GitHubAuthManager auth = new GitHubAuthManager();
            auth.requestDeviceCode(new GitHubAuthManager.AuthCallback() {
                @Override public void onCodeReceived(String userCode, String uri) {
                    tvDeviceCode.setText("Buka " + uri + "\nKode: " + userCode);
                    tvStatus.setText("Menunggu login...");
                }
                @Override public void onLoginSuccess(String token) {
                    tvStatus.setText("Login berhasil! Mengambil data...");
                    auth.getUsername(token, username -> {
                        sessionManager.saveSession(token, username);
                        Toast.makeText(LoginActivity.this, "Selamat datang, " + username, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });
                }
                @Override public void onError(String error) {
                    tvStatus.setText("Error: " + error);
                }
            });
        });
    }
}