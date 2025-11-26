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

public class ChildHomeActivity extends AppCompatActivity {

    private String currentChildId;

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
        Button check = findViewById(R.id.check);
        Button log = findViewById(R.id.log);
        Button help = findViewById(R.id.help);
        Button out = findViewById(R.id.out);
        Button practice = findViewById(R.id.practice);
        Button progress = findViewById(R.id.progress);
        Button history = findViewById(R.id.history);
        if (check != null) {
            check.setOnClickListener(v ->
                    Toast.makeText(this, "TODO: Daily check-in screen for R5", Toast.LENGTH_SHORT).show()
            );
        }
        if (log != null) {
            log.setOnClickListener(v -> {


                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                currentChildId = currentUser.getUid();

                Intent intent = new Intent(ChildHomeActivity.this, MedicineLogActivity.class);
                intent.putExtra("CHILD_ID", currentChildId);
                intent.putExtra("CHILD_NAME", "My");
                intent.putExtra("USER_TYPE", "child");
                startActivity(intent);
            });
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

    }


}