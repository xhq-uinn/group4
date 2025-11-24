package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class ChildShareSettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String parentUid;
    private String childId;

    // Checkbox
    private CheckBox cbRescue, cbController, cbSymptoms, cbTriggers, cbPeak, cbTriage, cbSummary;

    // Shared tag textViews
    private TextView tagRescue, tagController, tagSymptoms, tagTriggers, tagPeak, tagTriage, tagSummary;

    private Button buttonInviteProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_share_settings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        parentUid = auth.getCurrentUser().getUid();

        // get childId from parentHome
        childId = getIntent().getStringExtra("childId");

        //initialize buttonInviteProvider
        buttonInviteProvider = findViewById(R.id.btnInviteProvider);

        initViews();
        loadShareSettings();
        setupToggleListeners();

        // Invite provider button
        buttonInviteProvider.setOnClickListener(v -> {
            createInviteCode(childId);
        });
    }

    private void initViews() {
        cbRescue = findViewById(R.id.cbRescueLogs);
        cbController = findViewById(R.id.cbController);
        cbSymptoms = findViewById(R.id.cbSymptoms);
        cbTriggers = findViewById(R.id.cbTriggers);
        cbPeak = findViewById(R.id.cbPeakFlow);
        cbTriage = findViewById(R.id.cbTriage);
        cbSummary = findViewById(R.id.cbSummary);

        tagRescue = findViewById(R.id.tvRescueSharedTag);
        tagController = findViewById(R.id.tvControllerSharedTag);
        tagSymptoms = findViewById(R.id.tvSymptomsSharedTag);
        tagTriggers = findViewById(R.id.tvTriggersSharedTag);
        tagPeak = findViewById(R.id.tvPeakFlowSharedTag);
        tagTriage = findViewById(R.id.tvTriageSharedTag);
        tagSummary = findViewById(R.id.tvSummarySharedTag);
    }

    private void loadShareSettings() {
        DocumentReference docRef = db.collection("parents")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("settings")
                .document("sharing");

        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                cbRescue.setChecked(doc.getBoolean("rescue") != null && doc.getBoolean("rescue"));
                cbController.setChecked(doc.getBoolean("controller") != null && doc.getBoolean("controller"));
                cbSymptoms.setChecked(doc.getBoolean("symptoms") != null && doc.getBoolean("symptoms"));
                cbTriggers.setChecked(doc.getBoolean("triggers") != null && doc.getBoolean("triggers"));
                cbPeak.setChecked(doc.getBoolean("peakflow") != null && doc.getBoolean("peakflow"));
                cbTriage.setChecked(doc.getBoolean("triage") != null && doc.getBoolean("triage"));
                cbSummary.setChecked(doc.getBoolean("summary") != null && doc.getBoolean("summary"));
            }

            updateSharedTags();
        });
    }

    private void setupToggleListeners() {
        cbRescue.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("rescue", isChecked));
        cbController.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("controller", isChecked));
        cbSymptoms.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("symptoms", isChecked));
        cbTriggers.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("triggers", isChecked));
        cbPeak.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("peakflow", isChecked));
        cbTriage.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("triage", isChecked));
        cbSummary.setOnCheckedChangeListener((buttonView, isChecked) -> updateFirestore("summary", isChecked));
    }

    private void updateFirestore(String field, boolean value) {
        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("settings")
                .document("sharing")
                .update(field, value)
                .addOnSuccessListener(aVoid -> updateSharedTags());
    }

    private void updateSharedTags() {
        tagRescue.setVisibility(cbRescue.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagController.setVisibility(cbController.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagSymptoms.setVisibility(cbSymptoms.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagTriggers.setVisibility(cbTriggers.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagPeak.setVisibility(cbPeak.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagTriage.setVisibility(cbTriage.isChecked() ? TextView.VISIBLE : TextView.GONE);
        tagSummary.setVisibility(cbSummary.isChecked() ? TextView.VISIBLE : TextView.GONE);
    }

    //inviteProvider

    private void createInviteCode(String childId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = user.getUid();
        String code = generateCode(8);

        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("parentId", parentId);
        inviteData.put("childId", childId);
        inviteData.put("createdAt", FieldValue.serverTimestamp());
        inviteData.put("used", false);
        inviteData.put("usedByProviderId", null);

        db.collection("invites")
                .document(code)
                .set(inviteData)
                .addOnSuccessListener(unused -> showInviteDialog(code))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // code generator
    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // dialog
    private void showInviteDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Invite Code")
                .setMessage("Share this code with your provider:\n\n" + code)
                .setPositiveButton("OK", null)
                .setNeutralButton("Share", (d, w) -> shareInviteCode(code))
                .show();
    }

    // system share
    private void shareInviteCode(String code) {
        String text = "Here is my SmartAir invite code: " + code;
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(Intent.createChooser(sendIntent, "Share invite code"));
    }


}
