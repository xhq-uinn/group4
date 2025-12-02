package com.example.a1112;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.TextView;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import com.example.a1112.MotivationCalculator;

public class ChildHomeActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private FirebaseFirestore db;
    private TextView zoneText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
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
        Button pefButton = findViewById(R.id.PEF);

        db = FirebaseFirestore.getInstance();
        childName = getIntent().getStringExtra("childName");

        if (childName == null) {
            //find name child name using id when it isnt passed with intent
            db.collection("children").document(childId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        //always just set name to My if its not found/null/empty/fails
                        if (documentSnapshot.exists()) {
                            childName = documentSnapshot.getString("name");
                            if (childName == null || childName.isEmpty()) {
                                childName = "My";
                            }
                        }
                        else
                        {
                            childName = "My";
                        }
                    })
                    .addOnFailureListener(e -> {
                        childName = "My";
                    });
        }


//        childId = getIntent().getStringExtra("childId");
        if (check != null) {
            check.setOnClickListener(v -> {
                Intent intent = new Intent(ChildHomeActivity.this, DailyCheckInActivity.class);
                intent.putExtra("childId", childId);
                intent.putExtra("authorRole", "child");
                startActivity(intent);
            });
        }
        if (log != null) {
            log.setOnClickListener(v -> {

                Intent intent = new Intent(ChildHomeActivity.this, MedicineLogActivity.class);
                intent.putExtra("CHILD_ID", childId);
                intent.putExtra("CHILD_NAME", childName);
                intent.putExtra("USER_TYPE", "child");
                startActivity(intent);
            });
        }
        if (help != null) {
            help.setOnClickListener(v -> {
                Intent i = new Intent(ChildHomeActivity.this, TriageActivity.class);
                i.putExtra("childId", childId);
                startActivity(i);
            });
        }

        if (practice != null) {
            practice.setOnClickListener(v -> {
                Intent intent = new Intent(ChildHomeActivity.this, TechniqueHelperActivity.class);
                intent.putExtra("childId", childId);
                startActivity(intent);
            });
        }
        if (progress != null) {
            progress.setOnClickListener(v -> {

                Intent intent = new Intent(ChildHomeActivity.this, MotivationActivity.class);
                intent.putExtra("childId", childId);
                startActivity(intent);
            });
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
        if (childId != null && !childId.isEmpty()) {
            new MotivationCalculator().updateAllMotivation(childId, () -> {
                runOnUiThread(() -> {
                    // No UI update needed here
                });
            });
        }
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
    private void loadMotivationUI() {
        // (intentionally left blank - streak UI code removed)
    }
}