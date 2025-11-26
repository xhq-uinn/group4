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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChildHomeActivity extends AppCompatActivity {

//    private FirebaseFirestore db;
//    private FirebaseAuth auth;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        // Firebase
//        db = FirebaseFirestore.getInstance();
//        auth = FirebaseAuth.getInstance();
//        FirebaseUser user = auth.getCurrentUser();

        Button check = findViewById(R.id.check);
        Button log = findViewById(R.id.log);
        Button help = findViewById(R.id.help);
        Button out = findViewById(R.id.out);
        Button practice = findViewById(R.id.practice);
        Button progress = findViewById(R.id.progress);
        Button history = findViewById(R.id.history);

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
            help.setOnClickListener(v ->
                    Toast.makeText(this, "TODO: Breathing help / triage screen for R4", Toast.LENGTH_SHORT).show()
            );
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
            history.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, HistoryActivity.class);
                i.putExtra("childId", childId);
                startActivity(i);
            });
        }
        if (out != null) {
            out.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            });
        }
    }
}