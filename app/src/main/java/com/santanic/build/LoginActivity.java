package com.santanic.build;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.santanic.build.api.GitHubAuthManager;
import com.santanic.build.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView tvStatus;
    
    // ISI DENGAN DATA DARI GITHUB OAUTH APPS ANDA
    private static final String CLIENT_ID = "Ov23liJRu7F3iYnIevNi"; 
    private static final String CLIENT_SECRET = "ac9600820c85c060faf09fba0352bca060cf6776";
    private static final String REDIRECT_URI = "santanic://callback";

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

        tvStatus = findViewById(R.id.tvStatus);
        Button btnLogin = findViewById(R.id.btnLoginGithub);

        btnLogin.setOnClickListener(v -> {
            // Buka halaman login GitHub langsung di browser HP
            String url = "https://github.com/login/oauth/authorize" +
                    "?client_id=" + CLIENT_ID +
                    "&scope=repo%20workflow%20read:user" +
                    "&redirect_uri=" + REDIRECT_URI;

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
            
            tvStatus.setText("Silakan login di browser...");
        });
        
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        // Menangkap balasan dari browser setelah user klik Authorize
        if (data != null && data.toString().startsWith(REDIRECT_URI)) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                tvStatus.setText("Memproses login...");
                GitHubAuthManager auth = new GitHubAuthManager();
                // Tukar kode dengan Token
                auth.exchangeCodeForToken(CLIENT_ID, CLIENT_SECRET, code, REDIRECT_URI, new GitHubAuthManager.AuthCallback() {
                    @Override public void onCodeReceived(String userCode, String verificationUri) { }
                    @Override public void onLoginSuccess(String accessToken) {
                        auth.getUsername(accessToken, username -> {
                            sessionManager.saveSession(accessToken, username);
                            Toast.makeText(LoginActivity.this, "Login Berhasil: " + username, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    }
                    @Override public void onError(String error) {
                        tvStatus.setText("Error: " + error);
                    }
                });
            }
        }
    }
}
