package com.santanic.build.api;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import okhttp3.*;

public class GitHubUploader {
    private final String token;
    private final String owner;
    private final String repo;
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public GitHubUploader(String token, String owner, String repo) {
        this.token = token; this.owner = owner; this.repo = repo;
    }

    public void uploadFiles(Map<String, File> filesMap, String commitMessage, UploadCallback callback) {
        new Thread(() -> {
            try {
                String headSha = getLatestCommitSha();
                if (headSha == null) { 
                    callback.onError("Gagal ambil commit HEAD. Pastikan repo & branch main ada."); 
                    return; 
                }
                String baseTreeSha = getBaseTreeSha(headSha);

                JSONArray treeArray = new JSONArray();
                for (Map.Entry<String, File> entry : filesMap.entrySet()) {
                    String blobSha = createBlob(entry.getValue());
                    if (blobSha != null) {
                        JSONObject treeItem = new JSONObject();
                        treeItem.put("path", entry.getKey());
                        treeItem.put("mode", "100644");
                        treeItem.put("type", "blob");
                        treeItem.put("sha", blobSha);
                        treeArray.put(treeItem);
                    }
                }

                String newTreeSha = createTree(baseTreeSha, treeArray);
                String newCommitSha = createCommit(commitMessage, newTreeSha, headSha);
                boolean success = updateReference(newCommitSha);
                
                if (success) callback.onSuccess(newCommitSha);
                else callback.onError("Gagal push ke repository");
            } catch (Exception e) { 
                callback.onError("Error: " + e.getMessage()); 
            }
        }).start();
    }

    // PERBAIKAN: Menggunakan 'throws Exception' agar aman dari JSONException & IOException
    private String getLatestCommitSha() throws Exception {
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/refs/heads/main")
                .addHeader("Authorization", "Bearer " + token).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONObject("object").getString("sha");
            }
        }
        return null;
    }

    private String getBaseTreeSha(String commitSha) throws Exception {
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/commits/" + commitSha)
                .addHeader("Authorization", "Bearer " + token).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONObject("tree").getString("sha");
            }
        }
        return null;
    }

    private String createBlob(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) { fis.read(bytes); }
        String base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("content", base64Content);
        jsonBody.put("encoding", "base64");

        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/blobs")
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(jsonBody.toString(), JSON)).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getString("sha");
            }
        }
        return null;
    }

    private String createTree(String baseTreeSha, JSONArray treeArray) throws Exception {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("base_tree", baseTreeSha);
        jsonBody.put("tree", treeArray);

        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/trees")
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(jsonBody.toString(), JSON)).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getString("sha");
            }
        }
        return null;
    }

    private String createCommit(String message, String treeSha, String parentSha) throws Exception {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("message", message);
        jsonBody.put("tree", treeSha);
        jsonBody.put("parents", new JSONArray().put(parentSha));

        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/commits")
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(jsonBody.toString(), JSON)).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getString("sha");
            }
        }
        return null;
    }

    private boolean updateReference(String commitSha) throws Exception {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("sha", commitSha);
        jsonBody.put("force", false);

        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/git/refs/heads/main")
                .addHeader("Authorization", "Bearer " + token)
                .patch(RequestBody.create(jsonBody.toString(), JSON)).build();
        try (Response response = client.newCall(request).execute()) { 
            return response.isSuccessful(); 
        }
    }

    public interface UploadCallback { 
        void onSuccess(String commitSha); 
        void onError(String error); 
    }
}
