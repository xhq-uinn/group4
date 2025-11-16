package com.example.a1112;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChildShareSettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String parentUid;
    private String childId;

    // Checkbox
    private CheckBox cbRescue, cbController, cbSymptoms, cbTriggers, cbPeak, cbTriage, cbSummary;

    // Shared tag textViews
    private TextView tagRescue, tagController, tagSymptoms, tagTriggers, tagPeak, tagTriage, tagSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_share_settings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        parentUid = auth.getCurrentUser().getUid();

        // 从 ParentHome 获取 childId（必须）
        childId = getIntent().getStringExtra("childId");

        initViews();
        loadShareSettings();
        setupToggleListeners();
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
}
