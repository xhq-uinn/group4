package com.example.a1112;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ParentMotivationSettingsActivity extends AppCompatActivity {

    private EditText etControllerStreak, etTechniqueStreak, etHQCount, etLowRescue;
    private Button btnSave;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_motivation_settings);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        parentId = auth.getCurrentUser().getUid();

        etControllerStreak = findViewById(R.id.etControllerStreak);
        etTechniqueStreak = findViewById(R.id.etTechniqueStreak);
        etHQCount = findViewById(R.id.etHQCount);
        etLowRescue = findViewById(R.id.etLowRescue);
        btnSave = findViewById(R.id.btnSaveMotivationSettings);

        loadCurrentSettings();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadCurrentSettings() {

        db.collection("users")
                .document(parentId)
                .collection("motivationSettings")
                .document("config")
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    Number c = (Number) doc.get("controllerStreakTarget");
                    if (c != null) etControllerStreak.setText(String.valueOf(c.intValue()));

                    Number t = (Number) doc.get("techniqueStreakTarget");
                    if (t != null) etTechniqueStreak.setText(String.valueOf(t.intValue()));

                    Number h = (Number) doc.get("techniqueHighQualityCount");
                    if (h != null) etHQCount.setText(String.valueOf(h.intValue()));

                    Number l = (Number) doc.get("lowRescueThreshold");
                    if (l != null) etLowRescue.setText(String.valueOf(l.intValue()));
                });
    }

    private void saveSettings() {

        int c = parse(etControllerStreak.getText().toString(), 7);
        int t = parse(etTechniqueStreak.getText().toString(), 7);
        int h = parse(etHQCount.getText().toString(), 10);
        int l = parse(etLowRescue.getText().toString(), 4);

        Map<String, Object> data = new HashMap<>();
        data.put("controllerStreakTarget", c);
        data.put("techniqueStreakTarget", t);
        data.put("techniqueHighQualityCount", h);
        data.put("lowRescueThreshold", l);

        db.collection("users")
                .document(parentId)
                .collection("motivationSettings")
                .document("config")
                .set(data)
                .addOnSuccessListener(s -> Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show());
    }

    private int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}