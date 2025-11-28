package com.example.a1112;

import android.os.Bundle;
import android.util.Log; // Import Log for debugging
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ChildShareSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ChildShareSettings";

    private CheckBox cbPermissionEnabled; // Overall sharing control switch
    private CheckBox cbRescueLogs, cbController, cbSymptoms, cbTriggers,
            cbPeakFlow, cbTriage, cbSummary; // Detailed, granular sharing options

    private FirebaseFirestore db;
    private String inviteCode; // The unique code identifying the sharing relationship
    private String childId;    // The ID of the child whose settings are being modified

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_share_settings);


        // Ensure Intent parameters are retrieved and handle missing data gracefully
        inviteCode = getIntent().getStringExtra("inviteCode");
        childId = getIntent().getStringExtra("childId");

        if (inviteCode == null || childId == null) {
            // Log error if essential data is missing
            Log.e(TAG, "Missing inviteCode or childId");
            Toast.makeText(this, "Configuration error: Missing invite details.", Toast.LENGTH_LONG).show();
            // If parameters are missing, finish the Activity safely instead of crashing
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Initialize CheckBox views from the layout
        cbPermissionEnabled = findViewById(R.id.cbPermissionEnabled);
        cbRescueLogs = findViewById(R.id.cbRescueLogs);
        cbController = findViewById(R.id.cbController);
        cbSymptoms = findViewById(R.id.cbSymptoms);
        cbTriggers = findViewById(R.id.cbTriggers);
        cbPeakFlow = findViewById(R.id.cbPeakFlow);
        cbTriage = findViewById(R.id.cbTriage);
        cbSummary = findViewById(R.id.cbSummary);

        loadSharingSettings();
        setupListeners();
    }

    private void loadSharingSettings() {
        // Reference to the Firestore document storing the sharing permissions
        DocumentReference docRef = db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .document(inviteCode);

        docRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            // Determine the state of the overall permission switch (defaults to true)
            boolean permissionEnabled = Boolean.TRUE.equals(snapshot.getBoolean("permissionEnabled"));

            // NEW: Load the state of the overall switch
            cbPermissionEnabled.setChecked(permissionEnabled);

            // Load the status of the other 7 detailed CheckBoxes
            cbRescueLogs.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("rescueLogs")));
            cbController.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("controllerAdherence")));
            cbSymptoms.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("symptoms")));
            cbTriggers.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("triggers")));
            cbPeakFlow.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("peakFlow")));
            cbTriage.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("triageIncidents")));
            cbSummary.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("summaryCharts")));

            // NEW: Based on the loaded overall status, enable or disable the detailed switches
            setDetailedPermissionEnabled(permissionEnabled);
        });
    }

    private void setDetailedPermissionEnabled(boolean isEnabled) {
        cbRescueLogs.setEnabled(isEnabled);
        cbController.setEnabled(isEnabled);
        cbSymptoms.setEnabled(isEnabled);
        cbTriggers.setEnabled(isEnabled);
        cbPeakFlow.setEnabled(isEnabled);
        cbTriage.setEnabled(isEnabled);
        cbSummary.setEnabled(isEnabled);
    }

    private void setupListeners() {
        // General listener: used to map CheckBox ID to the corresponding Firestore field name
        CompoundButton.OnCheckedChangeListener generalListener = (buttonView, isChecked) -> {
            String field = null;

            // Determine the Firestore field name based on the CheckBox ID
            if (buttonView.getId() == R.id.cbRescueLogs) {
                field = "rescueLogs";
            } else if (buttonView.getId() == R.id.cbController) {
                field = "controllerAdherence";
            } else if (buttonView.getId() == R.id.cbSymptoms) {
                field = "symptoms";
            } else if (buttonView.getId() == R.id.cbTriggers) {
                field = "triggers";
            } else if (buttonView.getId() == R.id.cbPeakFlow) {
                field = "peakFlow";
            } else if (buttonView.getId() == R.id.cbTriage) {
                field = "triageIncidents";
            } else if (buttonView.getId() == R.id.cbSummary) {
                field = "summaryCharts";
            }

            if (field != null) {
                updateSharingField(field, isChecked);
            }
        };

        // Specific listener for the overall switch: handles Firestore update AND UI linkage
        cbPermissionEnabled.setOnCheckedChangeListener((buttonView, isEnabled) -> {
            // Update the 'permissionEnabled' field in Firestore
            updateSharingField("permissionEnabled", isEnabled);

            // UI Linkage: Enable or disable the other 7 detailed CheckBoxes
            setDetailedPermissionEnabled(isEnabled);
        });

        // Attach the general Firestore update listener to the other 7 detailed CheckBoxes
        cbRescueLogs.setOnCheckedChangeListener(generalListener);
        cbController.setOnCheckedChangeListener(generalListener);
        cbSymptoms.setOnCheckedChangeListener(generalListener);
        cbTriggers.setOnCheckedChangeListener(generalListener);
        cbPeakFlow.setOnCheckedChangeListener(generalListener);
        cbTriage.setOnCheckedChangeListener(generalListener);
        cbSummary.setOnCheckedChangeListener(generalListener);
    }

    private void updateSharingField(String field, boolean value) {
        Map<String, Object> update = new HashMap<>();
        update.put(field, value);

        db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .document(inviteCode)
                // Use SetOptions.merge() to update only the specified field without overwriting the whole document
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, field + " updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update " + field, Toast.LENGTH_SHORT).show());
    }
}