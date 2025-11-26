package com.example.a1112;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.TextView;


public class ChildHomeActivity extends AppCompatActivity {

    private String childId;
    private TextView zoneText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        String idFromIntent = getIntent().getStringExtra("childId");
        if (idFromIntent != null && !idFromIntent.isEmpty()) {
            childId = idFromIntent;
        }

        Toast.makeText(this, "ChildHome for: " + childId, Toast.LENGTH_SHORT).show();

        zoneText = findViewById(R.id.zone);
        Button check = findViewById(R.id.check);
        Button log = findViewById(R.id.log);
        Button help = findViewById(R.id.help);
        Button out = findViewById(R.id.out);
        Button practice = findViewById(R.id.practice);
        Button progress = findViewById(R.id.progress);
        Button history = findViewById(R.id.history);
        Button pefButton = findViewById(R.id.PEF);


        childId = getIntent().getStringExtra("childId");
        if (check != null) {
            check.setOnClickListener(v -> {
                Intent intent = new Intent(ChildHomeActivity.this, DailyCheckInActivity.class);
                intent.putExtra("childId", childId);
                intent.putExtra("authorRole", "child");
                startActivity(intent);
            });
        }
        if (log != null) {
            log.setOnClickListener(v ->
                    Toast.makeText(this, "TODO: Log medicine screen for R3", Toast.LENGTH_SHORT).show()
            );
        }
        if (help != null) {
            help.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, TriageActivity.class);
                i.putExtra("childId", childId);
                startActivity(i);
            });
        }

        if (practice != null) {
            practice.setOnClickListener(v ->
                    Toast.makeText(this, "TODO:practice helper for R3", Toast.LENGTH_SHORT).show()
            );
        }
        if (progress != null) {
            progress.setOnClickListener(v ->
                    Toast.makeText(this, "TODO: Motivation for R3", Toast.LENGTH_SHORT).show()
            );
        }
        if (history != null) {
            history.setOnClickListener(v ->
                    Toast.makeText(this, "TODO: History for R5", Toast.LENGTH_SHORT).show()
            );
        }
        if (out != null) {
            out.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            });
        }
        if (pefButton != null) {
            pefButton.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, PEFActivity.class);
                i.putExtra("childId", childId);
                startActivity(i);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateZoneText();
    }

    private void updateZoneText() {
        if (zoneText == null) return;

        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        String prefix = "child_" + childId + "_";

        int lastPef = prefs.getInt(prefix + "last_pef", -1);
        String lastZone = prefs.getString(prefix + "last_zone", null);

        if (lastZone == null) {
            zoneText.setText("Hi, Today you are in the zone unknown");
        } else if (lastPef == -1) {
            zoneText.setText("Hi, Today you are in the " + lastZone + " zone");
        } else {
            zoneText.setText("Hi, Today you are in the " + lastZone
                    + " zone (PEF " + lastPef + ")");
        }
    }
}