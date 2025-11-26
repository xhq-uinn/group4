package com.example.a1112;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private EditText startDateEdit, endDateEdit;
    private Button filterBtn, exportBtn;
    private LinearLayout resultsLayout;

    // Symptom checkboxes
    private CheckBox checkNightWaking, checkActivityLimit, checkCoughWheeze;

    // Trigger checkboxes
    private CheckBox checkExercise, checkColdAir, checkDustPets, checkSmoke, checkIllness, checkOdors;

    private String childId;
    private ArrayList<Map<String, Object>> loadedRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childId");

        bindViews();
        setupFilterButton();
        setupExportButton();
    }

    /** Bind all UI components */
    private void bindViews() {
        startDateEdit = findViewById(R.id.edit_start_date);
        endDateEdit = findViewById(R.id.edit_end_date);
        filterBtn = findViewById(R.id.button_filter);
        exportBtn = findViewById(R.id.button_export_pdf);
        resultsLayout = findViewById(R.id.layout_results);

        // Symptoms
        checkNightWaking = findViewById(R.id.check_symptom_nightwaking);
        checkActivityLimit = findViewById(R.id.check_symptom_activitylimit);
        checkCoughWheeze = findViewById(R.id.check_symptom_coughwheeze);

        // Triggers
        checkExercise = findViewById(R.id.check_trigger_exercise);
        checkColdAir = findViewById(R.id.check_trigger_coldair);
        checkDustPets = findViewById(R.id.check_trigger_dustpets);
        checkSmoke = findViewById(R.id.check_trigger_smoke);
        checkIllness = findViewById(R.id.check_trigger_illness);
        checkOdors = findViewById(R.id.check_trigger_odors);
    }

    /** Setup filter button click listener */
    private void setupFilterButton() {
        filterBtn.setOnClickListener(v -> loadFilteredData());
    }

    /** Setup export button click listener */
    private void setupExportButton() {
        exportBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                return;
            }
            exportPdf();
        });
    }

    /** Load data from Firestore with filters applied */
    private void loadFilteredData() {
        String start = startDateEdit.getText().toString().trim();
        String end = endDateEdit.getText().toString().trim();

        // Parse start and end dates as numbers: YYYYMMDD
        long startNum = parseDateToNumber(start);
        long endNum = parseDateToNumber(end);

        // Collect selected symptoms
        List<String> selectedSymptoms = new ArrayList<>();
        if (checkNightWaking.isChecked()) selectedSymptoms.add("nightWaking");
        if (checkActivityLimit.isChecked()) selectedSymptoms.add("activityLimit");
        if (checkCoughWheeze.isChecked()) selectedSymptoms.add("coughWheeze");

        // Collect selected triggers
        List<String> selectedTriggers = new ArrayList<>();
        if (checkExercise.isChecked()) selectedTriggers.add("exercise");
        if (checkColdAir.isChecked()) selectedTriggers.add("cold_air");
        if (checkDustPets.isChecked()) selectedTriggers.add("dust_pets");
        if (checkSmoke.isChecked()) selectedTriggers.add("smoke");
        if (checkIllness.isChecked()) selectedTriggers.add("illness");
        if (checkOdors.isChecked()) selectedTriggers.add("odors");

        db.collection("children")
                .document(childId)
                .collection("dailyCheckins")
                .get()
                .addOnSuccessListener(query -> {
                    resultsLayout.removeAllViews();
                    loadedRecords.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String dateStr = doc.getString("date");
                        long docNum = parseDateToNumber(dateStr);

                        // Skip if outside start/end range
                        if (docNum < startNum || docNum > endNum) continue;

                        // Symptom matching
                        boolean matchSymptom = selectedSymptoms.isEmpty();
                        for (String s : selectedSymptoms) {
                            String value = doc.getString(s);
                            if (value != null && !value.equalsIgnoreCase("none")) {
                                matchSymptom = true;
                                break;
                            }
                        }

                        // Trigger matching
                        boolean matchTrigger = selectedTriggers.isEmpty();
                        List<String> triggers = (List<String>) doc.get("triggers");
                        if (triggers != null && !triggers.isEmpty() && !selectedTriggers.isEmpty()) {
                            for (String t : selectedTriggers) {
                                if (triggers.contains(t)) {
                                    matchTrigger = true;
                                    break;
                                }
                            }
                        }

                        if (!matchSymptom && !matchTrigger) continue;

                        Map<String, Object> record = doc.getData();
                        loadedRecords.add(record);
                        addRecordView(record);
                    }

                    Toast.makeText(this, "Loaded " + loadedRecords.size() + " records", Toast.LENGTH_SHORT).show();
                });
    }

    /** Convert a date string like "Nov/01/2025" to number YYYYMMDD for easy comparison */
    private long parseDateToNumber(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd/yyyy", Locale.ENGLISH);
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH 从0开始
            int day = cal.get(Calendar.DAY_OF_MONTH);
            return year * 10000L + month * 100 + day;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


    private void addRecordView(Map<String, Object> record) {
        StringBuilder display = new StringBuilder();

        // Date and author
        display.append("Date: ").append(record.get("date")).append("\n");
        display.append("Author: ").append(record.get("authorRole") != null ? record.get("authorRole") : "Unknown").append("\n");

        // Symptoms
        display.append("Night waking: ").append(record.get("nightWaking") != null ? record.get("nightWaking") : "none").append("\n");
        display.append("Activity limit: ").append(record.get("activityLimit") != null ? record.get("activityLimit") : "none").append("\n");
        display.append("Cough/Wheeze: ").append(record.get("coughWheeze") != null ? record.get("coughWheeze") : "none").append("\n");

        // Triggers
        List<String> triggers = (List<String>) record.get("triggers");
        if (triggers != null && !triggers.isEmpty()) {
            display.append("Triggers: ").append(String.join(", ", triggers)).append("\n");
        } else {
            display.append("Triggers: none\n");
        }

        // Notes
        display.append("Notes: ").append(record.get("notes") != null ? record.get("notes") : "").append("\n");

        TextView tv = new TextView(this);
        tv.setText(display.toString());
        tv.setPadding(16, 16, 16, 16);
        tv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        resultsLayout.addView(tv);
    }

    /** Export currently loaded records to a text file */
    private void exportPdf() {
        if (loadedRecords.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdfDir = new File(Environment.getExternalStorageDirectory(), "CheckInReports");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            File file = new File(pdfDir, "report_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(file);

            for (Map<String, Object> r : loadedRecords) {
                fos.write(("Date: " + r.get("date") + "\n").getBytes());
                fos.write(("Author: " + r.get("authorRole") + "\n").getBytes());
                fos.write(("Night waking: " + r.get("nightWaking") + "\n").getBytes());
                fos.write(("Activity limit: " + r.get("activityLimit") + "\n").getBytes());
                fos.write(("Cough/Wheeze: " + r.get("coughWheeze") + "\n").getBytes());
                fos.write(("Triggers: " + r.get("triggers") + "\n").getBytes());
                fos.write(("Notes: " + r.get("notes") + "\n\n").getBytes());
            }

            fos.close();
            Toast.makeText(this, "PDF exported!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
