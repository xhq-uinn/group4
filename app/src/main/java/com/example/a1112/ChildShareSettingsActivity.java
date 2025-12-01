package com.example.a1112;

import android.os.Bundle;
import android.util.Log;
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

    // cbPermissionEnabled is REMOVED from the fields list.
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
        // NOTE: cbPermissionEnabled initialization removed here.
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
            // Document does not exist (new setup) OR Document exists (existing setup)
            // In both cases, default unchecked/false if the field is missing.

            // Load and set the state of the 7 detailed CheckBoxes

            // Rescue logs
            boolean rescueLogsChecked = Boolean.TRUE.equals(snapshot.getBoolean("rescueLogs"));
            setCheckboxText(cbRescueLogs, "Rescue logs", rescueLogsChecked);
            cbRescueLogs.setChecked(rescueLogsChecked);

            // Controller adherence
            boolean controllerChecked = Boolean.TRUE.equals(snapshot.getBoolean("controllerAdherence"));
            setCheckboxText(cbController, "Controller adherence", controllerChecked);
            cbController.setChecked(controllerChecked);

            // Symptoms
            boolean symptomsChecked = Boolean.TRUE.equals(snapshot.getBoolean("symptoms"));
            setCheckboxText(cbSymptoms, "Symptoms", symptomsChecked);
            cbSymptoms.setChecked(symptomsChecked);

            // Triggers
            boolean triggersChecked = Boolean.TRUE.equals(snapshot.getBoolean("triggers"));
            setCheckboxText(cbTriggers, "Triggers", triggersChecked);
            cbTriggers.setChecked(triggersChecked);

            // Peak-flow
            boolean peakFlowChecked = Boolean.TRUE.equals(snapshot.getBoolean("peakFlow"));
            setCheckboxText(cbPeakFlow, "Peak-flow", peakFlowChecked);
            cbPeakFlow.setChecked(peakFlowChecked);

            // Triage incidents
            boolean triageChecked = Boolean.TRUE.equals(snapshot.getBoolean("triageIncidents"));
            setCheckboxText(cbTriage, "Triage incidents", triageChecked);
            cbTriage.setChecked(triageChecked);

            // Summary charts
            boolean summaryChecked = Boolean.TRUE.equals(snapshot.getBoolean("summaryCharts"));
            setCheckboxText(cbSummary, "Summary charts", summaryChecked);
            cbSummary.setChecked(summaryChecked);

            // NOTE: setDetailedPermissionEnabled() method is now removed, as permissions are always enabled.
        });
    }

    // NOTE: setDetailedPermissionEnabled() method is removed.

    private void setCheckboxText(CheckBox cb, String baseText, boolean isChecked) {
        if (isChecked) {
            cb.setText(baseText + " (Shared with Provider)");
        } else {
            cb.setText(baseText);
        }
    }

    private void setupListeners() {
        // General listener: used to map CheckBox ID to the corresponding Firestore field name
        CompoundButton.OnCheckedChangeListener generalListener = (buttonView, isChecked) -> {
            String field = null;
            String baseText = "";
            CheckBox currentCheckbox = (CheckBox) buttonView;

            // Determine the Firestore field name AND the base text based on the CheckBox ID
            if (buttonView.getId() == R.id.cbRescueLogs) {
                field = "rescueLogs";
                baseText = "Rescue logs";
            } else if (buttonView.getId() == R.id.cbController) {
                field = "controllerAdherence";
                baseText = "Controller adherence";
            } else if (buttonView.getId() == R.id.cbSymptoms) {
                field = "symptoms";
                baseText = "Symptoms";
            } else if (buttonView.getId() == R.id.cbTriggers) {
                field = "triggers";
                baseText = "Triggers";
            } else if (buttonView.getId() == R.id.cbPeakFlow) {
                field = "peakFlow";
                baseText = "Peak-flow";
            } else if (buttonView.getId() == R.id.cbTriage) {
                field = "triageIncidents";
                baseText = "Triage incidents";
            } else if (buttonView.getId() == R.id.cbSummary) {
                field = "summaryCharts";
                baseText = "Summary charts";
            }

            if (field != null) {
                // Update the CheckBox text dynamically
                setCheckboxText(currentCheckbox, baseText, isChecked);

                // Update Firestore
                updateSharingField(field, isChecked);
            }
        };

        // NOTE: Specific listener for cbPermissionEnabled is removed.
        // NOTE: All helper methods for managing listeners are removed.

        // Attach the general Firestore update and text update listener to the 7 detailed CheckBoxes
        cbRescueLogs.setOnCheckedChangeListener(generalListener);
        cbController.setOnCheckedChangeListener(generalListener);
        cbSymptoms.setOnCheckedChangeListener(generalListener);
        cbTriggers.setOnCheckedChangeListener(generalListener);
        cbPeakFlow.setOnCheckedChangeListener(generalListener);
        cbTriage.setOnCheckedChangeListener(generalListener);
        cbSummary.setOnCheckedChangeListener(generalListener);
    }

    // NOTE: Helper methods (removeDetailedListeners, attachDetailedListeners, setDetailedCheckboxes, updateAllDetailedFields) are REMOVED.

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