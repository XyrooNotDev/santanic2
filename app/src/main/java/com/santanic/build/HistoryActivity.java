package com.santanic.build;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"Flutter Build", "2 menit lalu", "Berhasil"});
        data.add(new String[]{"Android Studio", "15 menit lalu", "Gagal"});
        data.add(new String[]{"WebToAPK", "1 jam lalu", "Berhasil"});

        rv.setAdapter(new BuildHistoryAdapter(data));
    }
}