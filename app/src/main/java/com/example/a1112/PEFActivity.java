package com.example.a1112;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PEFActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private String childId;
    private String parentId = null;


    private int pb = 350;

    private EditText pefEdit;
    private Button save, cancel;

    private String prefsPrefix = "child_default_child_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();


        String fromIntent = getIntent().getStringExtra("childId");
        if (fromIntent != null && !fromIntent.isEmpty()) {
            childId = fromIntent;
        }
        else {
            Toast.makeText(this,
                    "Not signed in. Returning to login.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PEFActivity.this, LoginActivity.class));
            finish();
            return;
        }
        prefsPrefix = "child_" + childId + "_";


        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        int storedPb = prefs.getInt(prefsPrefix + "pb", 0);
        if (storedPb > 0) {
            pb = storedPb;
        }


        loadPbFromFirestore();
        loadParentIdFromFirestore();

        pefEdit = findViewById(R.id.PEFedit);
        save = findViewById(R.id.savePEF);
        cancel = findViewById(R.id.cancelPEF);

        save.setOnClickListener(v -> savePefAndShowZone());
        cancel.setOnClickListener(v -> finish());
    }


    private void loadPbFromFirestore() {
        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long pbVal = doc.getLong("pb");
                        if (pbVal != null && pbVal > 0) {
                            pb = pbVal.intValue();

                            SharedPreferences prefs =
                                    getSharedPreferences("child_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putInt(prefsPrefix + "pb", pb)
                                    .apply();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load PB: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


    private void loadParentIdFromFirestore() {
        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        parentId = doc.getString("parentId");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load parentId: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }



    private void savePefAndShowZone() {
        if (pb <= 0) {
            Toast.makeText(this,
                    "PB not set. Please ask your parent to set PB first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String pefStr = pefEdit.getText().toString().trim();
        if (pefStr.isEmpty()) {
            pefEdit.setError("PEF is required");
            return;
        }

        int pef;
        try {
            pef = Integer.parseInt(pefStr);
        } catch (NumberFormatException e) {
            pefEdit.setError("PEF must be a number");
            return;
        }

        double ratio = (double) pef / pb;
        String zone;
        if (ratio >= 0.80) {
            zone = "GREEN";
        } else if (ratio >= 0.50) {
            zone = "YELLOW";
        } else {
            zone = "RED";
        }

        if ("RED".equals(zone)) {
            sendParentAlert(
                    "redzone",
                    "Child's PEF is in the RED zone (PEF " + pef + ", PB " + pb + ")."
            );
        }

        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt(prefsPrefix + "last_pef", pef)
                .putString(prefsPrefix + "last_zone", zone)
                .apply();

        logPefToFirebase(pef, zone);

        String message = "Today: " + zone + " ZONE (PEF " + pef + ")";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        finish();
    }


    private void logPefToFirebase(int pef, String zone) {
        Map<String, Object> data = new HashMap<>();
        data.put("pef", pef);
        data.put("zone", zone);
        data.put("pb", pb);
        data.put("date", FieldValue.serverTimestamp());

        db.collection("children")
                .document(childId)
                .collection("pefLogs")
                .add(data)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to save PEF log: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


    private void sendParentAlert(String type, String details) {
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this,
                    "Alert saved locally, but no parent linked.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString(prefsPrefix + "alert_type", type)
                .putLong(prefsPrefix + "alert_time", System.currentTimeMillis())
                .putString(prefsPrefix + "alert_details", details)
                .apply();

        Map<String, Object> alert = new HashMap<>();
        alert.put("childId", childId);
        alert.put("parentId", parentId);
        alert.put("type", type);
        alert.put("details", details);
        alert.put("timestamp", FieldValue.serverTimestamp());

        db.collection("alerts")
                .add(alert)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this,
                                "Parent alert queued (redzone)",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to queue alert: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}