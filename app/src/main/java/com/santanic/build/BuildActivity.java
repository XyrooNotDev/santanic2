package com.santanic.build;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.santanic.build.api.GitHubActionsManager;
import com.santanic.build.api.GitHubUploader;
import com.santanic.build.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class BuildActivity extends AppCompatActivity {
    private SessionManager session;
    private Map<String, File> selectedFiles = new HashMap<>();
    private TextView tvFileCount;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build);
        session = new SessionManager(this);

        String type = getIntent().getStringExtra("BUILD_TYPE");
        ((TextView) findViewById(R.id.tvTitle)).setText("Build " + type);

        tvFileCount = findViewById(R.id.tvFileCount);
        TextInputEditText etOwner = findViewById(R.id.etOwner);
        TextInputEditText etRepo = findViewById(R.id.etRepo);
        TextInputEditText etWorkflow = findViewById(R.id.etWorkflow);

        etOwner.setText(session.getUsername());
        
        // Siapkan File Picker (Bisa pilih banyak file)
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                processUri(uri);
                            }
                        } else if (data.getData() != null) {
                            processUri(data.getData());
                        }
                        tvFileCount.setText(selectedFiles.size() + " file dipilih");
                    }
                }
        );

        findViewById(R.id.btnPickFiles).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // Bisa semua tipe file
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(intent);
        });

        findViewById(R.id.btnStartBuild).setOnClickListener(v -> {
            String owner = etOwner.getText().toString().trim();
            String repo = etRepo.getText().toString().trim();
            String workflow = etWorkflow.getText().toString().trim();

            if (owner.isEmpty() || repo.isEmpty() || workflow.isEmpty() || selectedFiles.isEmpty()) {
                Toast.makeText(this, "Lengkapi data dan pilih file!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Mengunggah file...", Toast.LENGTH_SHORT).show();
            
            GitHubUploader uploader = new GitHubUploader(session.getToken(), owner, repo);
            uploader.uploadFiles(selectedFiles, "Auto Commit from Santanic", new GitHubUploader.UploadCallback() {
                @Override public void onSuccess(String commitSha) {
                    runOnUiThread(() -> {
                        Toast.makeText(BuildActivity.this, "Upload Sukses! Memulai Build...", Toast.LENGTH_SHORT).show();
                        // Trigger workflow setelah upload
                        GitHubActionsManager actions = new GitHubActionsManager(session.getToken(), owner, repo);
                        actions.triggerWorkflow(workflow, "main", new GitHubActionsManager.TriggerCallback() {
                            @Override public void onSuccess() {
                                runOnUiThread(() -> {
                                    Toast.makeText(BuildActivity.this, "Build Berhasil Dimulai!", Toast.LENGTH_LONG).show();
                                    finish();
                                });
                            }
                            @Override public void onError(String error) {
                                runOnUiThread(() -> Toast.makeText(BuildActivity.this, "Trigger Gagal: " + error, Toast.LENGTH_LONG).show());
                            }
                        });
                    });
                }
                @Override public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(BuildActivity.this, "Upload Gagal: " + error, Toast.LENGTH_LONG).show());
                }
            });
        });
    }

    private void processUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            File tempFile = new File(getCacheDir(), fileName);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
            }
            // Simpan di map dengan path default (misal folder 'src/')
            selectedFiles.put(fileName, tempFile);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal baca file: " + uri.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
}