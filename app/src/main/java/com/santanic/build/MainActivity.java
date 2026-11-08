package com.santanic.build;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.santanic.build.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SessionManager session = new SessionManager(this);
        ((TextView) findViewById(R.id.tvUsername)).setText(session.getUsername() + " Connected");

        findViewById(R.id.cardAndroid).setOnClickListener(v -> openBuild("ANDROID"));
        findViewById(R.id.cardFlutter).setOnClickListener(v -> openBuild("FLUTTER"));
        findViewById(R.id.cardWeb).setOnClickListener(v -> openBuild("WEBTOAPK"));
        findViewById(R.id.cardHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void openBuild(String type) {
        Intent intent = new Intent(this, BuildActivity.class);
        intent.putExtra("BUILD_TYPE", type);
        startActivity(intent);
    }
}