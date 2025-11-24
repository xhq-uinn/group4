package com.example.a1112;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DailyCheckInActivity extends AppCompatActivity {

    // UI
    private RadioGroup radioAuthorGroup;
    private RadioGroup radioNightGroup;
    private RadioGroup radioActivityGroup;
    private RadioGroup radioCoughGroup;

    private CheckBox cbExercise, cbColdAir, cbDustPets, cbSmoke, cbIllness, cbOdors;
    private EditText notesEdit;
    private Button buttonSave, buttonCancel;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    //intent extras
    public static final String EXTRA_CHILD_ID = "childId";
    public static final String EXTRA_AUTHOR_ROLE = "authorRole"; //child/parent

    private String childId;
    private String authorRole;
    private TextView dateText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_check_in);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        authorRole = getIntent().getStringExtra(EXTRA_AUTHOR_ROLE);

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Missing child id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (authorRole == null || authorRole.isEmpty()) {
            authorRole = "child";
        }

        bindViews();
        setupListeners();
        String date = new SimpleDateFormat("MMM-dd-yyyy", Locale.getDefault())
                .format(new Date());
        dateText.setText("Today: " + date);
    }

    private void bindViews() {
        radioAuthorGroup = findViewById(R.id.radio_group_author);
        radioNightGroup = findViewById(R.id.radio_group_night_waking);
        radioActivityGroup = findViewById(R.id.radio_group_activity_limit);
        radioCoughGroup = findViewById(R.id.radio_group_cough_wheeze);

        cbExercise = findViewById(R.id.check_trigger_exercise);
        cbColdAir = findViewById(R.id.check_trigger_cold_air);
        cbDustPets = findViewById(R.id.check_trigger_dust_pets);
        cbSmoke = findViewById(R.id.check_trigger_smoke);
        cbIllness = findViewById(R.id.check_trigger_illness);
        cbOdors = findViewById(R.id.check_trigger_odors);

        notesEdit = findViewById(R.id.edit_notes);

        buttonSave = findViewById(R.id.button_save);
        buttonCancel = findViewById(R.id.button_cancel);

        dateText = findViewById(R.id.text_daily_date);
    }

    private void setupListeners() {
        buttonSave.setOnClickListener(v -> saveDailyCheckIn());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void saveDailyCheckIn() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }
        String parentUid = auth.getCurrentUser().getUid();

        // read UI
        String nightWaking = getNightWakingValue();
        String activityLimit = getActivityLimitValue();
        String coughWheeze = getCoughWheezeValue();
        ArrayList<String> triggers = getSelectedTriggers();
        String notes = notesEdit.getText().toString().trim();

        if (nightWaking == null || activityLimit == null || coughWheeze == null) {
            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
            return;
        }

        // choose who is writing
        int checkedAuthorId = radioAuthorGroup.getCheckedRadioButtonId();
        if (checkedAuthorId == R.id.radio_author_child) {
            authorRole = "child";
        } else if (checkedAuthorId == R.id.radio_author_parent) {
            authorRole = "parent";
        }

        // date
        String dateString = new SimpleDateFormat("MMM-dd-yyyy", Locale.getDefault())
                .format(new Date());

        // assign data
        Map<String, Object> data = new HashMap<>();
        data.put("childId", childId);
        data.put("parentUid", parentUid);
        data.put("authorRole", authorRole); // "child" / "parent"
        data.put("date", dateString);
        data.put("nightWaking", nightWaking);
        data.put("activityLimit", activityLimit);
        data.put("coughWheeze", coughWheeze);
        data.put("triggers", triggers);
        data.put("notes", notes);
        data.put("createdAt", FieldValue.serverTimestamp());

        //store into database
        db.collection("children")
                .document(childId)
                .collection("dailyCheckins")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Check-in saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getNightWakingValue() {
        int id = radioNightGroup.getCheckedRadioButtonId();
        if (id == R.id.radio_night_none) return "none";
        if (id == R.id.radio_night_once) return "once";
        if (id == R.id.radio_night_multiple) return "multiple";
        return null;
    }

    private String getActivityLimitValue() {
        int id = radioActivityGroup.getCheckedRadioButtonId();
        if (id == R.id.radio_activity_none) return "none";
        if (id == R.id.radio_activity_some) return "some";
        if (id == R.id.radio_activity_cannot_play) return "cannot_play";
        return null;
    }

    private String getCoughWheezeValue() {
        int id = radioCoughGroup.getCheckedRadioButtonId();
        if (id == R.id.radio_cough_none) return "none";
        if (id == R.id.radio_cough_some) return "some";
        if (id == R.id.radio_cough_a_lot) return "a_lot";
        return null;
    }

    private ArrayList<String> getSelectedTriggers() {
        ArrayList<String> triggers = new ArrayList<>();
        if (cbExercise.isChecked()) triggers.add("exercise");
        if (cbColdAir.isChecked()) triggers.add("cold_air");
        if (cbDustPets.isChecked()) triggers.add("dust_pets");
        if (cbSmoke.isChecked()) triggers.add("smoke");
        if (cbIllness.isChecked()) triggers.add("illness");
        if (cbOdors.isChecked()) triggers.add("odors");
        return triggers;
    }
}
