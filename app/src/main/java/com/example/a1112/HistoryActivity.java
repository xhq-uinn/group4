package com.example.a1112;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private CheckBox checkNightWaking, checkActivityLimit, checkCoughWheeze;
    private CheckBox checkSymptomAny;

    private CheckBox checkExercise, checkColdAir, checkDustPets, checkSmoke, checkIllness, checkOdors;
    private CheckBox checkTriggerAny;

    private String childId;
    private ArrayList<Map<String, Object>> loadedRecords = new ArrayList<>();

    private static final List<String> ALL_SYMPTOM_KEYS = Arrays.asList(
            "nightWaking", "activityLimit", "coughWheeze"
    );
    private static final List<String> ALL_TRIGGER_VALUES = Arrays.asList(
            "exercise", "cold_air", "dust_pets", "smoke", "illness", "odors"
    );

    private static final String DATE_PATTERN = "MMM-dd-yyyy";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childId");

        bindViews();
        setupFilterButton();
        setupExportButton();

        startDateEdit.setHint("Start date (e.g., Aug-01-2025, default: 6 months ago)");
        endDateEdit.setHint("End date (e.g., Nov-30-2025, default: today)");
    }

    private void bindViews() {
        startDateEdit = findViewById(R.id.edit_start_date);
        endDateEdit = findViewById(R.id.edit_end_date);
        filterBtn = findViewById(R.id.button_filter);
        exportBtn = findViewById(R.id.button_export_pdf);
        resultsLayout = findViewById(R.id.layout_results);

        checkSymptomAny = findViewById(R.id.check_symptom_any);
        checkNightWaking = findViewById(R.id.check_symptom_nightwaking);
        checkActivityLimit = findViewById(R.id.check_symptom_activitylimit);
        checkCoughWheeze = findViewById(R.id.check_symptom_coughwheeze);

        checkTriggerAny = findViewById(R.id.check_trigger_any);
        checkExercise = findViewById(R.id.check_trigger_exercise);
        checkColdAir = findViewById(R.id.check_trigger_coldair);
        checkDustPets = findViewById(R.id.check_trigger_dustpets);
        checkSmoke = findViewById(R.id.check_trigger_smoke);
        checkIllness = findViewById(R.id.check_trigger_illness);
        checkOdors = findViewById(R.id.check_trigger_odors);
    }

    private void setupFilterButton() {
        filterBtn.setOnClickListener(v -> loadFilteredData());
    }

    private void setupExportButton() {
        exportBtn.setOnClickListener(v -> exportPdf());
    }

    private Date parseDateToDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;

        Date date = null;

        try {
            SimpleDateFormat sdfEn = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
            date = sdfEn.parse(dateStr);
        } catch (ParseException e) {
            try {
                SimpleDateFormat sdfCn = new SimpleDateFormat(DATE_PATTERN, Locale.CHINA);
                date = sdfCn.parse(dateStr);
            } catch (ParseException ex) {
                try {
                    SimpleDateFormat sdfDigit = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
                    date = sdfDigit.parse(dateStr);
                } catch (ParseException finalEx) {
                    return null;
                }
            }
        }
        return date;
    }

    private long parseDateToNumber(@NonNull String dateStr) {
        Date date = parseDateToDate(dateStr);
        if (date == null) return 0;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return year * 10000L + month * 100 + day;
    }


    private void loadFilteredData() {
        String start = startDateEdit.getText().toString().trim();
        String end = endDateEdit.getText().toString().trim();

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 23);
        todayCal.set(Calendar.MINUTE, 59);
        todayCal.set(Calendar.SECOND, 59);

        Calendar sixMonthsAgoCal = (Calendar) todayCal.clone();
        sixMonthsAgoCal.add(Calendar.MONTH, -6);
        sixMonthsAgoCal.set(Calendar.HOUR_OF_DAY, 0);
        sixMonthsAgoCal.set(Calendar.MINUTE, 0);
        sixMonthsAgoCal.set(Calendar.SECOND, 0);

        // Parse user input dates, or use defaults (6 months ago to today)
        Date startDateObj = parseDateToDate(start.isEmpty() ? new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(sixMonthsAgoCal.getTime()) : start);
        Date endDateObj = parseDateToDate(end.isEmpty() ? new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(todayCal.getTime()) : end);

        if (startDateObj == null || endDateObj == null) {
            Toast.makeText(this, "Invalid date format. Please use a valid date format (e.g., Nov-26-2025).", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDateObj);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDateObj);

        if (endCal.before(startCal)) {
            Toast.makeText(this, "End date cannot be before start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Max range limit 6 months
        Calendar sixMonthLimit = (Calendar) startCal.clone();
        sixMonthLimit.add(Calendar.MONTH, 6);

        if (endCal.after(sixMonthLimit)) {
            Toast.makeText(this, "The date range cannot exceed six months. Please adjust your dates.", Toast.LENGTH_LONG).show();
            return;
        }

        // Min range limit 3 months
        Calendar threeMonthLimit = (Calendar) startCal.clone();
        threeMonthLimit.add(Calendar.MONTH, 3);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);

        if (endCal.before(threeMonthLimit)) {
            Toast.makeText(this, "The date range must be at least three months long. Please adjust your dates.", Toast.LENGTH_LONG).show();
            return;
        }

        long startNum = parseDateToNumber(start.isEmpty() ? new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(sixMonthsAgoCal.getTime()) : start);
        long endNum = parseDateToNumber(end.isEmpty() ? new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(todayCal.getTime()) : end);


        boolean isSymptomFilterActive = checkSymptomAny.isChecked() ||
                checkNightWaking.isChecked() ||
                checkActivityLimit.isChecked() ||
                checkCoughWheeze.isChecked();

        boolean isTriggerFilterActive = checkTriggerAny.isChecked() ||
                checkExercise.isChecked() ||
                checkColdAir.isChecked() ||
                checkDustPets.isChecked() ||
                checkSmoke.isChecked() ||
                checkIllness.isChecked() ||
                checkOdors.isChecked();

        if (!isSymptomFilterActive && !isTriggerFilterActive) {
            resultsLayout.removeAllViews();
            loadedRecords.clear();
            Toast.makeText(this, "Please select at least one filter.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine actual symptom keys to check
        List<String> selectedSymptoms = new ArrayList<>();
        if (!checkSymptomAny.isChecked()) {
            if (checkNightWaking.isChecked()) selectedSymptoms.add("nightWaking");
            if (checkActivityLimit.isChecked()) selectedSymptoms.add("activityLimit");
            if (checkCoughWheeze.isChecked()) selectedSymptoms.add("coughWheeze");
        }
        List<String> symptomKeysToCheck = checkSymptomAny.isChecked() ? ALL_SYMPTOM_KEYS : selectedSymptoms;

        // Determine actual trigger values to check
        List<String> selectedTriggers = new ArrayList<>();
        if (!checkTriggerAny.isChecked()) {
            if (checkExercise.isChecked()) selectedTriggers.add("exercise");
            if (checkColdAir.isChecked()) selectedTriggers.add("cold_air");
            if (checkDustPets.isChecked()) selectedTriggers.add("dust_pets");
            if (checkSmoke.isChecked()) selectedTriggers.add("smoke");
            if (checkIllness.isChecked()) selectedTriggers.add("illness");
            if (checkOdors.isChecked()) selectedTriggers.add("odors");
        }
        List<String> triggerValuesToCheck = checkTriggerAny.isChecked() ? ALL_TRIGGER_VALUES : selectedTriggers;


        db.collection("children")
                .document(childId)
                .collection("dailyCheckins")
                .get()
                .addOnSuccessListener(query -> {
                    resultsLayout.removeAllViews();
                    loadedRecords.clear(); // Clear previous data


                    for (QueryDocumentSnapshot doc : query) {
                        String dateStr = doc.getString("date");
                        if (dateStr == null) continue;

                        long docNum = parseDateToNumber(dateStr);

                        if (docNum < startNum || docNum > endNum) continue;

                        Map<String, Object> recordData = doc.getData();

                        // Initialize record filter results: Passes if filter is not active
                        boolean recordPassesSymptomFilter = !isSymptomFilterActive;
                        boolean recordPassesTriggerFilter = !isTriggerFilterActive;

                        if (isSymptomFilterActive) {
                            for (String s : symptomKeysToCheck) {
                                String val = (String) recordData.get(s);
                                if (val != null && !val.equalsIgnoreCase("none")) {
                                    recordPassesSymptomFilter = true;
                                    break;
                                }
                            }
                        }

                        if (isTriggerFilterActive) {

                            Object triggersObject = recordData.get("triggers");
                            List<String> triggers = null;

                            if (triggersObject instanceof List) {
                                try {
                                    // Attempt runtime cast, handles List<Object> containing Strings
                                    triggers = (List<String>) triggersObject;
                                } catch (ClassCastException e) {
                                    // Failed to cast, likely mixed types in DB array
                                    triggers = null;
                                }
                            }

                            if (triggers != null) {
                                for (String t : triggerValuesToCheck) {
                                    if (triggers.contains(t)) {
                                        recordPassesTriggerFilter = true; // Match found
                                        break;
                                    }
                                }
                            }
                        }

                        if (recordPassesSymptomFilter && recordPassesTriggerFilter) {
                            loadedRecords.add(recordData);
                        }
                    }


                    // Sorting: Sort by date descending (newest first)
                    Collections.sort(loadedRecords, (r1, r2) -> {
                        String dateStr1 = (String) r1.get("date");
                        String dateStr2 = (String) r2.get("date");

                        if (dateStr1 == null && dateStr2 == null) return 0;
                        if (dateStr1 == null) return 1;
                        if (dateStr2 == null) return -1;

                        Date date1 = parseDateToDate(dateStr1);
                        Date date2 = parseDateToDate(dateStr2);

                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;

                        return date2.compareTo(date1);
                    });

                    // Display sorted records in the UI
                    for (Map<String, Object> record : loadedRecords) {
                        addRecordView(record);
                    }

                    Toast.makeText(this, "Loaded " + loadedRecords.size() + " records", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void addRecordView(Map<String, Object> record) {
        StringBuilder display = new StringBuilder();

        display.append("Date: ").append(record.get("date")).append("\n");
        display.append("Author: ").append(record.get("authorRole") != null ? record.get("authorRole") : "Unknown").append("\n");

        display.append("Night waking: ").append(record.get("nightWaking") != null ? record.get("nightWaking") : "none").append("\n");
        display.append("Activity limit: ").append(record.get("activityLimit") != null ? record.get("activityLimit") : "none").append("\n");
        display.append("Cough/Wheeze: ").append(record.get("coughWheeze") != null ? record.get("coughWheeze") : "none").append("\n");

        List<String> triggers = (List<String>) record.get("triggers");
        if (triggers != null && !triggers.isEmpty()) {
            display.append("Triggers: ").append(String.join(", ", triggers)).append("\n");
        } else {
            display.append("Triggers: none\n");
        }

        display.append("Notes: ").append(record.get("notes") != null ? record.get("notes") : "").append("\n");

        TextView tv = new TextView(this);
        tv.setText(display.toString());
        tv.setPadding(16, 16, 16, 16);
        tv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        tv.setLayoutParams(params);

        resultsLayout.addView(tv);
    }

    private void exportPdf() {
        if (loadedRecords.isEmpty()) {
            Toast.makeText(this, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();

        int pageHeight = 842;
        int pageWidth = 595;

        int x = 40;
        int y = 40;
        int lineHeight = 16;
        int maxLinesPerPage = (pageHeight - 80) / lineHeight;
        int currentLine = 0;

        Paint paint = new Paint();
        paint.setTextSize(10);

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(14);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        List<String> allLines = new ArrayList<>();

        for (Map<String, Object> r : loadedRecords) {
            allLines.add("--- Check-in Record ---");
            allLines.add("Date: " + r.get("date"));
            allLines.add("Author: " + r.get("authorRole"));
            allLines.add("Night waking: " + (r.get("nightWaking") != null ? r.get("nightWaking") : "none"));
            allLines.add("Activity limit: " + (r.get("activityLimit") != null ? r.get("activityLimit") : "none"));
            allLines.add("Cough/Wheeze: " + (r.get("coughWheeze") != null ? r.get("coughWheeze") : "none"));

            List<String> triggers = (List<String>) r.get("triggers");
            String triggersStr = (triggers != null && !triggers.isEmpty()) ? String.join(", ", triggers) : "none";
            allLines.add("Triggers: " + triggersStr);

            allLines.add("Notes: " + (r.get("notes") != null ? r.get("notes") : ""));
            allLines.add("");
        }

        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.drawText("Child Check-in History Report", x, y, titlePaint);
            y += 2 * lineHeight;


            for (String line : allLines) {
                if (currentLine >= maxLinesPerPage) {
                    document.finishPage(page);

                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();

                    y = 40;
                    currentLine = 0;

                    canvas.drawText("Child Check-in History Report (Cont.)", x, y, titlePaint);
                    y += 2 * lineHeight;
                }

                canvas.drawText(line, x, y, line.startsWith("---") ? titlePaint : paint);

                y += lineHeight;
                currentLine++;
            }

            document.finishPage(page);

            File pdfDir = new File(getExternalFilesDir(null), "CheckInReports");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            File file = new File(pdfDir, "report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);

            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, "PDF exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
}