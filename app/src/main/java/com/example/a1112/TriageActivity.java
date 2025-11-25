package com.example.a1112;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TriageActivity extends AppCompatActivity {


    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;


    private String childId = "default_child";
    private String parentId = null;


    private String prefsPrefix = "child_default_child_";


    private int pb = 350;


    private int lastPef = -1;


    private CheckBox cbCantSpeak;
    private CheckBox cbChestPull;
    private CheckBox cbBlueLips;

    private EditText etRescueUses;
    private EditText etCurrentPef;

    private TextView tvDecision;
    private TextView tvSafetyNote;
    private TextView tvTimer;

    private Button btnRun;


    private CountDownTimer recheckTimer;
    private boolean firstRun = true;
    private String initialZone = "UNKNOWN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();


        String fromIntent = getIntent().getStringExtra("childId");
        if (fromIntent != null && !fromIntent.isEmpty()) {
            childId = fromIntent;
        } else if (currentUser != null) {
            childId = currentUser.getUid();
        } else {
            Toast.makeText(this,
                    "Not signed in. Returning to login.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TriageActivity.this, LoginActivity.class));
            finish();
            return;
        }
        prefsPrefix = "child_" + childId + "_";


        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        int storedPb = prefs.getInt(prefsPrefix + "pb", 0);
        if (storedPb > 0) {
            pb = storedPb;
        }
        lastPef = prefs.getInt(prefsPrefix + "last_pef", -1);


        loadPbFromFirestore();
        loadParentIdFromFirestore();


        cbCantSpeak = findViewById(R.id.cbCantSpeak);
        cbChestPull = findViewById(R.id.cbChestPull);
        cbBlueLips = findViewById(R.id.cbBlueLips);

        etRescueUses = findViewById(R.id.etRescueUses);
        etCurrentPef = findViewById(R.id.etCurrentPef);

        tvDecision = findViewById(R.id.tvDecision);
        tvSafetyNote = findViewById(R.id.tvSafetyNote);
        tvTimer = findViewById(R.id.tvTimer);

        btnRun = findViewById(R.id.btnRunTriage);

        tvSafetyNote.setText(
                "Safety note: This is guidance, not a diagnosis. " +
                        "If in doubt, call emergency."
        );


        sendParentAlert("TRIAGE_SESSION_START",
                "Child started a breathing triage session.");

        btnRun.setOnClickListener(v -> runTriage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecheckTimer();
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


    private void runTriage() {


        boolean hasRedFlag =
                cbCantSpeak.isChecked()
                        || cbChestPull.isChecked()
                        || cbBlueLips.isChecked();


        int rescueUses = 0;
        String rescueStr = etRescueUses.getText().toString().trim();
        if (!rescueStr.isEmpty()) {
            try {
                rescueUses = Integer.parseInt(rescueStr);
            } catch (NumberFormatException e) {
                etRescueUses.setError("Please enter a number");
                return;
            }
        }
        boolean rapidRescue = rescueUses >= 3;


        Integer usedPef = null;
        String pefStr = etCurrentPef.getText().toString().trim();
        if (!pefStr.isEmpty()) {
            try {
                usedPef = Integer.parseInt(pefStr);
            } catch (NumberFormatException e) {
                etCurrentPef.setError("PEF must be a number");
                return;
            }
        } else if (lastPef > 0) {
            usedPef = lastPef;
        }

        StringBuilder decision = new StringBuilder();
        boolean emergency = false;
        boolean startHomeSteps = false;
        String zone = "UNKNOWN";


        if (hasRedFlag) {

            emergency = true;
            zone = "RED";
            decision.append("⚠️ Critical danger signs detected.\n\n")
                    .append("Call Emergency Now.\n")
                    .append("- Use your emergency inhaler as in your action plan.\n")
                    .append("- Tell an adult immediately.\n");
        } else {
            if (usedPef != null && pb > 0) {

                double ratio = (double) usedPef / pb;

                if (ratio < 0.50) {
                    emergency = true;
                    zone = "RED";
                    decision.append("Your breathing is in the RED zone based on PEF.\n\n")
                            .append("Call Emergency Now.\n")
                            .append("- Follow your RED zone steps from the action plan.\n")
                            .append("- Use your rescue inhaler.\n")
                            .append("- Contact your parent or doctor right away.\n");
                } else if (ratio < 0.80) {
                    startHomeSteps = true;
                    zone = "YELLOW";
                    decision.append("You are in the YELLOW zone.\n\n")
                            .append("Start Home Steps:\n")
                            .append("- Use your rescue inhaler as in the action plan.\n")
                            .append("- Slow down activity and rest.\n")
                            .append("- Re-check in 10 minutes.\n");
                } else {
                    startHomeSteps = true;
                    zone = "GREEN";
                    decision.append("You are in the GREEN zone.\n\n")
                            .append("Home Steps:\n")
                            .append("- Keep your controller medicine as usual.\n")
                            .append("- Watch your symptoms and tell an adult if you feel worse.\n");
                }
            } else {

                if (rapidRescue) {
                    startHomeSteps = true;
                    zone = "YELLOW";
                    decision.append("You used your rescue inhaler 3+ times in 3 hours.\n\n")
                            .append("Start Home Steps and tell your parent.\n")
                            .append("- Follow the YELLOW zone steps in your action plan.\n")
                            .append("- Re-check in 10 minutes.\n");
                } else {
                    startHomeSteps = true;
                    zone = "GREEN";
                    decision.append("No critical danger signs detected.\n\n")
                            .append("Home Steps:\n")
                            .append("- Use your rescue inhaler if you feel tight.\n")
                            .append("- Rest and avoid triggers.\n")
                            .append("- Re-check in 10 minutes if still uncomfortable.\n");
                }
            }
        }

        decision.append("\n\nSafety note: This is guidance, not a diagnosis. ")
                .append("If you feel very scared or get worse, call emergency.");

        tvDecision.setText(decision.toString());


        if (emergency) {
            sendParentAlert("ESCALATION_EMERGENCY",
                    "Triage recommended calling emergency.");
            stopRecheckTimer();
            tvTimer.setText("");
        } else {
            if (rapidRescue) {
                sendParentAlert("RAPID_RESCUE",
                        "Child used rescue medicine 3+ times in 3 hours.");
            }

            if (startHomeSteps) {
                if (firstRun) {
                    initialZone = zone;
                    firstRun = false;
                    startRecheckTimer();
                }
            } else {
                stopRecheckTimer();
                tvTimer.setText("");
            }
        }
    }


    private void startRecheckTimer() {
        stopRecheckTimer();

        final long totalMillis = 10 * 60 * 1000L;
        recheckTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long sec = seconds % 60;
                String text = String.format("Re-check in %02d:%02d", minutes, sec);
                tvTimer.setText(text);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Time to re-check your breathing.");
                showRecheckDialog();
            }
        };
        recheckTimer.start();
    }

    private void stopRecheckTimer() {
        if (recheckTimer != null) {
            recheckTimer.cancel();
            recheckTimer = null;
        }
    }


    private void showRecheckDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Re-check your breathing")
                .setMessage("Do you feel better now?\n\n"
                        + "Tap \"I'm better\" if your breathing is back to your normal GREEN zone.\n"
                        + "Tap \"Still not good / new danger signs\" if you still feel bad or have new danger signs.")
                .setPositiveButton("I'm better", (dialog, which) -> {
                    tvDecision.setText(
                            "Good job checking again.\n\n"
                                    + "Keep following your home steps and controller medicine.\n"
                                    + "Tell your parent if symptoms come back.");
                    tvTimer.setText("");
                    stopRecheckTimer();
                })
                .setNegativeButton("Still not good / new danger signs", (dialog, which) -> {
                    escalateAfterRecheck();
                })
                .setCancelable(false)
                .show();
    }

    private void escalateAfterRecheck() {
        String zoneInfo;
        if ("RED".equals(initialZone)) {
            zoneInfo = "You were already in the RED zone and are still not better.\n\n";
        } else if ("YELLOW".equals(initialZone)) {
            zoneInfo = "You were in the YELLOW zone and are still not better.\n\n";
        } else {
            zoneInfo = "Your breathing is still not better after home steps.\n\n";
        }

        StringBuilder msg = new StringBuilder();
        msg.append(zoneInfo)
                .append("Call Emergency Now.\n")
                .append("- Follow your RED zone steps from the action plan.\n")
                .append("- Use your rescue inhaler.\n")
                .append("- Contact your parent or doctor right away.\n\n")
                .append("Safety note: This is guidance, not a diagnosis. ")
                .append("If you feel very scared or get worse, call emergency.");

        tvDecision.setText(msg.toString());
        tvTimer.setText("");
        stopRecheckTimer();

        sendParentAlert("ESCALATION_AFTER_RECHECK",
                "Child was not better after 10-minute re-check. Emergency recommended.");
    }


    private void sendParentAlert(String type, String details) {
        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString(prefsPrefix + "alert_type", type)
                .putLong(prefsPrefix + "alert_time", System.currentTimeMillis())
                .putString(prefsPrefix + "alert_details", details)
                .apply();


        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this,
                    "Alert saved locally, but no parent linked.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
                                "Parent alert queued (" + type + ")",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to queue alert: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}