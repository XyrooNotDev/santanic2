package com.santanic.build.api;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class GitHubActionsManager {
    private final String token, owner, repo;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GitHubActionsManager(String token, String owner, String repo) {
        this.token = token; this.owner = owner; this.repo = repo;
    }

    public void triggerWorkflow(String workflowFile, String branch, TriggerCallback callback) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/workflows/" + workflowFile + "/dispatches";
        JSONObject jsonBody = new JSONObject();
        try { jsonBody.put("ref", branch); } catch (Exception e) {}

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { mainHandler.post(() -> callback.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() || response.code() == 204) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code() + ": " + response.message()));
                }
            }
        });
    }

    public interface TriggerCallback { void onSuccess(); void onError(String error); }
}