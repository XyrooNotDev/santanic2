package com.santanic.build.api;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class GitHubAuthManager {
    private static final String CLIENT_ID = "Ov23liJRu7F3iYnIevNi"; 
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onCodeReceived(String userCode, String verificationUri);
        void onLoginSuccess(String accessToken);
        void onError(String error);
    }

    public void requestDeviceCode(AuthCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", "repo workflow read:user")
                .build();

        Request request = new Request.Builder()
                .url("https://github.com/login/device/code")
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { mainHandler.post(() -> callback.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String deviceCode = json.getString("device_code");
                        String userCode = json.getString("user_code");
                        String verificationUri = json.getString("verification_uri");
                        int interval = json.getInt("interval");
                        mainHandler.post(() -> callback.onCodeReceived(userCode, verificationUri));
                        startPolling(deviceCode, interval, callback);
                    } catch (Exception e) { mainHandler.post(() -> callback.onError("Parse error")); }
                }
            }
        });
    }

    private void startPolling(String deviceCode, int interval, AuthCallback callback) {
        // Perbaikan: Menggunakan Runnable secara eksplisit agar tidak error
        Runnable pollRunnable = new Runnable() {
            @Override public void run() {
                RequestBody body = new FormBody.Builder()
                        .add("client_id", CLIENT_ID)
                        .add("device_code", deviceCode)
                        .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        .build();

                Request request = new Request.Builder()
                        .url("https://github.com/login/oauth/access_token")
                        .addHeader("Accept", "application/json")
                        .post(body).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            if (json.has("access_token")) {
                                mainHandler.post(() -> callback.onLoginSuccess(json.getString("access_token")));
                            } else if (json.optString("error").equals("authorization_pending")) {
                                // Perbaikan: Gunakan pollRunnable, bukan 'this'
                                mainHandler.postDelayed(pollRunnable, interval * 1000L);
                            } else {
                                mainHandler.post(() -> callback.onError("Gagal: " + json.optString("error")));
                            }
                        } catch (Exception e) { }
                    }
                });
            }
        };
        mainHandler.postDelayed(pollRunnable, interval * 1000L);
    }
    
    public void getUsername(String token, OnUsernameFetched listener) {
        Request request = new Request.Builder()
                .url("https://api.github.com/user")
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { listener.onFetched("User"); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    listener.onFetched(json.getString("login"));
                } catch (Exception e) { listener.onFetched("User"); }
            }
        });
    }
    public interface OnUsernameFetched { void onFetched(String username); }
}
