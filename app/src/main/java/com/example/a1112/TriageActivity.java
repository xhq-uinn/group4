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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriageActivity extends AppCompatActivity {


    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;


    private String childId;
    private String parentId;


    private String prefsPrefix = "child_default_child_";


    private int pb = 350;


    private int lastPef = -1;


    private String redActionPlan = "You are in the RED zone.\n"
            + "- Follow your RED zone steps from the action plan.\n"
            + "- Use your rescue inhaler.\n"
            + "- Contact your parent or doctor right away.\n";

    private String yellowActionPlan = "You are in the YELLOW zone.\n"
            + "- Use your rescue inhaler as in the action plan.\n"
            + "- Slow down activity and rest.\n"
            + "- Re-check later and tell your parent.\n";

    private String greenActionPlan = "You are in the GREEN zone.\n"
            + "- Keep your controller medicine as usual.\n"
            + "- Watch your symptoms and tell an adult if you feel worse.\n";

    // UI
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


        loadChildConfigFromFirestore();


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


        sendParentAlert("TRIAGE_STARTED",
                "Child opened the triage screen.");

        btnRun.setOnClickListener(v -> runTriage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecheckTimer();
    }


    private void loadChildConfigFromFirestore() {
        db.collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;


                    Long pbVal = doc.getLong("pb");
                    if (pbVal != null && pbVal > 0) {
                        pb = pbVal.intValue();

                        SharedPreferences prefs =
                                getSharedPreferences("child_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putInt(prefsPrefix + "pb", pb)
                                .apply();
                    }


                    String pid = doc.getString("parentId");
                    if (pid != null && !pid.isEmpty()) {
                        parentId = pid;
                    }


                    String red = doc.getString("redActionPlan");
                    if (red != null && !red.trim().isEmpty()) {
                        redActionPlan = red;
                    }

                    String yellow = doc.getString("yellowActionPlan");
                    if (yellow != null && !yellow.trim().isEmpty()) {
                        yellowActionPlan = yellow;
                    }

                    String green = doc.getString("greenActionPlan");
                    if (green != null && !green.trim().isEmpty()) {
                        greenActionPlan = green;
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load child config: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


    private void runTriage() {


        boolean hasRedFlag =
                cbCantSpeak.isChecked()
                        || cbChestPull.isChecked()
                        || cbBlueLips.isChecked();

        //collect red flags for incident log
        List<String> flags = new ArrayList<>();
        if (cbCantSpeak.isChecked()) {
            flags.add("cant_speak");
        }
        if (cbChestPull.isChecked()) {
            flags.add("chest_pull");
        }
        if (cbBlueLips.isChecked()) {
            flags.add("blue_lips");
        }


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


        if (rapidRescue) {
            sendParentAlert("RAPID_RESCUE",
                    "Child used rescue inhaler 3+ times in 3 hours.");
        }


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


        if (hasRedFlag) {
            // emergency status alert
            sendParentAlert("EMERGENCY_STATUS",
                    "Red flag symptoms detected. Emergency status.");

            decision.append("⚠️ Red flag symptoms detected.\n\n")
                    .append("CALL EMERGENCY NOW.\n\n")
                    .append(redActionPlan)
                    .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                    .append("If you feel very scared or get worse, call emergency.");

            tvDecision.setText(decision.toString());

            //save to incident log(case emergency)

            saveIncidentLog(flags, "call_emergency", "start_triage", usedPef);


            startRecheckTimerForEmergency();
            return;
        }


        if (usedPef != null) {
            if (pb <= 0) {
                tvDecision.setText(
                        "Your parent has not set a Personal Best (PB) yet.\n\n" +
                                "Please ask your parent to set PB in their app.");
                tvTimer.setText("");
                stopRecheckTimer();
                return;
            }

            double ratio = (double) usedPef / pb;

            if (ratio < 0.50) {
                decision.append("PEF is in the RED zone (<50% of PB).\n\n")
                        .append(redActionPlan)
                        .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                        .append("If red flag symptoms appear, call emergency.");

            } else if (ratio < 0.80) {
                decision.append("PEF is in the YELLOW zone (50–79% of PB).\n\n")
                        .append(yellowActionPlan)
                        .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                        .append("If you feel worse or new danger signs appear, call emergency.");
            } else {
                decision.append("PEF is in the GREEN zone (>=80% of PB).\n\n")
                        .append(greenActionPlan)
                        .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                        .append("If you feel worse, tell an adult.");
            }

        } else {
            if (rapidRescue) {
                decision.append("You used your rescue inhaler 3+ times in 3 hours.\n\n")
                        .append(yellowActionPlan)
                        .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                        .append("If danger signs appear, call emergency.");
            } else {
                decision.append("No critical danger signs detected.\n\n")
                        .append(greenActionPlan)
                        .append("\n\nSafety note: This is guidance, not a diagnosis. ")
                        .append("If you feel worse, tell an adult.");
            }
        }

        tvDecision.setText(decision.toString());
        stopRecheckTimer();
        tvTimer.setText("");

        //save to incident log(case non-emergency)
        String guidance;
        if (usedPef != null && pb > 0) {
            double ratio = (double) usedPef / pb;
            if (ratio < 0.50) {
                guidance = "call_emergency";
            } else {
                guidance = "home_steps";
            }
        } else {
            guidance = "home_steps";
        }

        saveIncidentLog(flags, guidance, "start_triage", usedPef);

    }

    private void startRecheckTimerForEmergency() {
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
                tvTimer.setText("10 minutes passed. How do you feel?");
                showEmergencyRecheckDialog();
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

    private void showEmergencyRecheckDialog() {
        new AlertDialog.Builder(this)
                .setTitle("After 10 minutes")
                .setMessage("Do you feel better now?\n\n"
                        + "Tap \"I'm better\" if your breathing feels back to safe.\n"
                        + "Tap \"Still not better\" to fill the triage again.")
                .setPositiveButton("I'm better", (dialog, which) -> {
                    tvDecision.setText(
                            "Good job checking again.\n\n"
                                    + "Keep following your action plan and tell your parent.");
                    tvTimer.setText("");
                    stopRecheckTimer();

                    //save to incident log
                    List<String> flags = new ArrayList<>();
                    saveIncidentLog(
                            flags,
                            "home_steps",
                            "after_recheck_better",
                            lastPef > 0 ? lastPef : null
                    );
                })
                .setNegativeButton("Still not better", (dialog, which) -> {
                    resetTriageInputs();
                    tvDecision.setText(
                            "Please fill the triage questions again and tap \"Run\".\n\n"
                                    + "If you feel very scared or get worse, call emergency.");
                    tvTimer.setText("");
                    stopRecheckTimer();

                    //save to incident log
                    List<String> flags = new ArrayList<>();
                    saveIncidentLog(
                            flags,
                            "call_emergency",
                            "after_recheck_not_better",
                            lastPef > 0 ? lastPef : null
                    );
                })
                .setCancelable(false)
                .show();
    }

    private void resetTriageInputs() {
        cbCantSpeak.setChecked(false);
        cbChestPull.setChecked(false);
        cbBlueLips.setChecked(false);
        etRescueUses.setText("");
        etCurrentPef.setText("");
    }

    private void sendParentAlert(String type, String details) {
        SharedPreferences prefs = getSharedPreferences("child_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString(prefsPrefix + "alert_type", type)
                .putLong(prefsPrefix + "alert_time", System.currentTimeMillis())
                .putString(prefsPrefix + "alert_details", details)
                .apply();

        if (parentId == null || parentId.isEmpty()) {
            return;
        }

        Map<String, Object> alert = new HashMap<>();
        alert.put("childId", childId);
        alert.put("parentId", parentId);
        alert.put("type", type);
        alert.put("details", details);
        alert.put("timestamp", FieldValue.serverTimestamp());

        db.collection("alerts")
                .add(alert);
    }

    //save incident log to database
    private void saveIncidentLog(List<String> flags, String guidance, String userResponse, Integer pef) {
        if (childId == null || childId.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("flags", flags);              // red flags
        data.put("guidance", guidance);        // guidance shown
        data.put("userResponse", userResponse);// user response
        data.put("pef", pef);                  // optional PEF
        data.put("timestamp", FieldValue.serverTimestamp());

        db.collection("children")
                .document(childId)
                .collection("incidentLogs")
                .add(data);
    }

}
