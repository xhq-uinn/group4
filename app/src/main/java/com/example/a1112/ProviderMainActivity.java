package com.example.a1112;

import static androidx.fragment.app.FragmentManager.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

public class ProviderMainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private String providerId, childId, childName;

    private ListenerRegistration sharingListener;

    private TextView title;
    private TextView rescueLogsNotShared, noRescueLogsText;
    private RecyclerView rescueLogsRecyclerView;
    private List<MedicineLog> rescueLogs = new ArrayList<>();
    private MedicineLogAdapter rescueLogsAdapter;

    private TextView controllerAdherenceNotShared, controllerAdherenceSummary;
    private TextView symptomsNotShared, symptomsSummary, noSymptomsText;

    private TextView triggersNotShared, triggersSummary;

    private TextView peakFlowNotShared, peakFlowSummary;

    private TextView triageNotShared, triageDisplay, noTriageText;

    private TextView rescueTrendNotShared, rescueTrendDisplay;
    private LineChart providerChartTrend;

    private Button buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_main);

        db = FirebaseFirestore.getInstance();
        providerId = getIntent().getStringExtra("PROVIDER_ID");
        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        initializeViews();
        setupRecyclerViews();
        setupClickListeners();

        setupSharedSettingsListener();
    }

    private void initializeViews() {
        title = findViewById(R.id.title);
        title.setText(childName + "'s Details");

        rescueLogsNotShared = findViewById(R.id.rescueLogsNotShared);
        rescueLogsRecyclerView = findViewById(R.id.rescueLogsRecyclerView);
        noRescueLogsText = findViewById(R.id.noRescueLogsText);

        controllerAdherenceNotShared = findViewById(R.id.controllerAdherenceNotShared);
        controllerAdherenceSummary = findViewById(R.id.controllerAdherenceSummary);

        symptomsNotShared = findViewById(R.id.symptomsNotShared);
        symptomsSummary = findViewById(R.id.symptomsSummary);
        noSymptomsText = findViewById(R.id.noSymptomsText);

        triggersNotShared = findViewById(R.id.triggersNotShared);
        triggersSummary = findViewById(R.id.triggersSummary);

        peakFlowNotShared = findViewById(R.id.peakFlowNotShared);
        peakFlowSummary = findViewById(R.id.peakFlowSummary);

        triageNotShared = findViewById(R.id.triageNotShared);
        triageDisplay = findViewById(R.id.triageDisplay);
        noTriageText = findViewById(R.id.noTriageText);

        rescueTrendNotShared = findViewById(R.id.rescueTrendNotShared);
        rescueTrendDisplay = findViewById(R.id.rescueTrendDisplay);
        providerChartTrend = findViewById(R.id.providerChartTrend);

        buttonBack = findViewById(R.id.buttonBack);
    }

    private void setupRecyclerViews() {
        // Setup rescue logs RecyclerView
        rescueLogsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        rescueLogsAdapter = new MedicineLogAdapter(rescueLogs);
        rescueLogsRecyclerView.setAdapter(rescueLogsAdapter);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupSharedSettingsListener() {
        sharingListener = db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .whereEqualTo("providerId", providerId)
                .addSnapshotListener((result, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to sharing settings.", error);

                        // Replace hideAllInfo() with direct calls to hide all sections
                        hideAllSectionsDueToError();
                        return;
                    }

                    if (result == null || result.isEmpty()) {
                        // If no matching document is found (shouldn't happen if accepted), hide everything
                        // Replace hideAllInfo() with direct calls to hide all sections
                        hideAllSectionsDueToError();
                        return;
                    }

                    // get the settings instance and update all visibilities accordingly
                    DocumentSnapshot doc = result.getDocuments().get(0);

                    // We now rely entirely on the individual sharing settings.
                    // If a field is missing (null), the updateXxxVisibility method will hide that section.
                    updateRescueLogsVisibility(doc.getBoolean("rescueLogs"));
                    updateControllerAdherenceVisibility(doc.getBoolean("controllerAdherence"));
                    updateSymptomsVisibility(doc.getBoolean("symptoms"));
                    updateTriggersVisibility(doc.getBoolean("triggers"));
                    updatePeakFlowVisibility(doc.getBoolean("peakFlow"));
                    updateTriageVisibility(doc.getBoolean("triageIncidents"));
                    updateRescueTrendVisibility(doc.getBoolean("summaryCharts"));
                });
    }

    // NEW: Dedicated helper to hide all sections in case of error or missing document
    private void hideAllSectionsDueToError() {
        // Pass 'false' to all update functions to ensure everything is hidden.
        updateRescueLogsVisibility(false);
        updateControllerAdherenceVisibility(false);
        updateSymptomsVisibility(false);
        updateTriggersVisibility(false);
        updatePeakFlowVisibility(false);
        updateTriageVisibility(false);
        updateRescueTrendVisibility(false);
    }

    //show or hide rescue logs and display message depending on sharing settings
    private void updateRescueLogsVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            rescueLogsNotShared.setVisibility(View.GONE);
            loadRescueLogs();
        } else {
            rescueLogsNotShared.setVisibility(View.VISIBLE);
            rescueLogsRecyclerView.setVisibility(View.GONE);
            noRescueLogsText.setVisibility(View.GONE);
        }
    }

    //load the childs last 25 rescueLogs
    private void loadRescueLogs() {
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .addOnSuccessListener(docs -> {
                    rescueLogs.clear();
                    for (QueryDocumentSnapshot doc : docs) {
                        MedicineLog log = doc.toObject(MedicineLog.class);
                        log.setId(doc.getId());
                        rescueLogs.add(log);
                    }

                    if (rescueLogs.isEmpty()) {
                        rescueLogsRecyclerView.setVisibility(View.GONE);
                        noRescueLogsText.setVisibility(View.VISIBLE);
                    } else {
                        rescueLogsRecyclerView.setVisibility(View.VISIBLE);
                        noRescueLogsText.setVisibility(View.GONE);
                        rescueLogsAdapter.updateLogs(rescueLogs);
                    }
                });
    }

    //show or hide controllerAdherence and display message depending on sharing settings
    private void updateControllerAdherenceVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            controllerAdherenceNotShared.setVisibility(View.GONE);
            calculateControllerAdherence();
        } else {
            controllerAdherenceNotShared.setVisibility(View.VISIBLE);
            controllerAdherenceSummary.setVisibility(View.GONE);
        }
    }

    //Find number of planned days completed in last 7 days by comparing logged doses and parent configured schedule
    private void calculateControllerAdherence() {

        db.collection("controllerSchedules")
                .document(childId)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.exists()) {
                        controllerAdherenceSummary.setVisibility(View.VISIBLE);
                        controllerAdherenceSummary.setText("No schedule configured");
                        return;
                    }

                    Map<String, Object> schedule = result.getData();

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    Date sevenDaysAgo = cal.getTime();

                    db.collection("medicineLogs")
                            .whereEqualTo("childId", childId)
                            .whereEqualTo("type", "controller")
                            .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
                            .get()
                            .addOnSuccessListener(logs -> {

                                int completedDays = calculateCompletedDays(schedule, logs);
                                controllerAdherenceSummary.setVisibility(View.VISIBLE);
                                controllerAdherenceSummary.setText("Last " + completedDays + "/7 days completed.");
                            });
                });
    }

    //show or hide symptoms and display message depending on sharing settings
    private void updateSymptomsVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            symptomsNotShared.setVisibility(View.GONE);
            loadSymptomsSummary();
        } else {
            symptomsNotShared.setVisibility(View.VISIBLE);
            symptomsSummary.setVisibility(View.GONE);
            noSymptomsText.setVisibility(View.GONE);
        }
    }

    //check last 7 days of daily checkins, count appearance of symptoms and display them
    private void loadSymptomsSummary() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgo = cal.getTime();

        db.collection("children")
                .document(childId)
                .collection("dailyCheckins")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgo)
                .get()
                .addOnSuccessListener(docs -> {

                    int activityLimitedDays = 0;
                    int nightWakingDays = 0;
                    int coughWheezeDays = 0;

                    for (QueryDocumentSnapshot doc : docs) {
                        String activityLimit = doc.getString("activityLimit");
                        if (activityLimit != null && !"none".equals(activityLimit)) {
                            activityLimitedDays++;
                        }

                        String nightWaking = doc.getString("nightWaking");
                        if (nightWaking != null && !"none".equals(nightWaking)) {
                            nightWakingDays++;
                        }

                        String coughWheeze = doc.getString("coughWheeze");
                        if (coughWheeze != null && !"none".equals(coughWheeze)) {
                            coughWheezeDays++;
                        }
                    }


                    StringBuilder summary = new StringBuilder("Last 7 days:\n");

                    if (activityLimitedDays > 0) summary.append("• Activity limited: ").append(activityLimitedDays).append(" days\n");
                    if (nightWakingDays > 0) summary.append("• Night waking: ").append(nightWakingDays).append(" days\n");
                    if (coughWheezeDays > 0) summary.append("• Cough/Wheeze: ").append(coughWheezeDays).append(" days\n");

                    if (summary.toString().equals("Last 7 days:\n")) {
                        summary.append("No symptoms reported");
                    }

                    if (docs.isEmpty()) {
                        noSymptomsText.setVisibility(View.VISIBLE);
                        symptomsSummary.setVisibility(View.GONE);
                    } else {
                        symptomsSummary.setVisibility(View.VISIBLE);
                        noSymptomsText.setVisibility(View.GONE);
                        symptomsSummary.setText(summary.toString());
                    }
                });
    }

    //show or hide Triggers and display message depending on sharing settings
    private void updateTriggersVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            triggersNotShared.setVisibility(View.GONE);
            loadTriggersSummary();
        } else {
            triggersNotShared.setVisibility(View.VISIBLE);
            triggersSummary.setVisibility(View.GONE);
        }
    }

    //check last 7 days of daily checkins, count appearance of triggers and display them
    private void loadTriggersSummary() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgo = cal.getTime();

        db.collection("children")
                .document(childId)
                .collection("dailyCheckins")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgo)
                .get()
                .addOnSuccessListener(docs -> {
                    int exercise = 0, coldAir = 0, dustPets = 0, smoke = 0, illness = 0, odors = 0;

                    for (QueryDocumentSnapshot doc : docs) {
                        List<String> triggers = (List<String>) doc.get("triggers");
                        if (triggers != null) {
                            for (String trigger : triggers) {
                                switch (trigger) {
                                    case "exercise": exercise++; break;
                                    case "cold_air": coldAir++; break;
                                    case "dust_pets": dustPets++; break;
                                    case "smoke": smoke++; break;
                                    case "illness": illness++; break;
                                    case "odors": odors++; break;
                                }
                            }
                        }
                    }

                    StringBuilder summary = new StringBuilder("Last 7 days:\n");
                    if (exercise > 0) summary.append("• Exercise: ").append(exercise).append(" days\n");
                    if (coldAir > 0) summary.append("• Cold air: ").append(coldAir).append(" days\n");
                    if (dustPets > 0) summary.append("• Dust/Pets: ").append(dustPets).append(" days\n");
                    if (smoke > 0) summary.append("• Smoke: ").append(smoke).append(" days\n");
                    if (illness > 0) summary.append("• Illness: ").append(illness).append(" days\n");
                    if (odors > 0) summary.append("• Odors: ").append(odors).append(" days\n");

                    if (summary.toString().equals("Last 7 days:\n")) {
                        summary.append("No triggers reported");
                    }

                    triggersSummary.setVisibility(View.VISIBLE);
                    triggersSummary.setText(summary.toString());
                });
    }

    //show or hide PEF and display message depending on sharing settings
    private void updatePeakFlowVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            peakFlowNotShared.setVisibility(View.GONE);
            loadPeakFlowSummary();
        } else {
            peakFlowNotShared.setVisibility(View.VISIBLE);
            peakFlowSummary.setVisibility(View.GONE);
        }
    }

    //get most recent pef reading and display its information
    private void loadPeakFlowSummary() {
        db.collection("children")
                .document(childId)
                .collection("pefLogs")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(docs -> {
                    if (docs.isEmpty()) {
                        peakFlowSummary.setVisibility(View.VISIBLE);
                        peakFlowSummary.setText("No PEF readings recorded");
                        return;
                    }

                    DocumentSnapshot doc = docs.getDocuments().get(0);

                    Number pef = (Number) doc.get("pef");
                    String zone = doc.getString("zone");

                    Date date = doc.getDate("date");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String dateString = dateFormat.format(date);

                    String summary = dateString + ": ";
                    if (pef != null) {
                        summary += childName + " recorded a pef of " + pef.intValue();
                        if (zone != null) {
                            summary += " (" + zone + " Zone)";
                        }
                    }

                    peakFlowSummary.setVisibility(View.VISIBLE);
                    peakFlowSummary.setText(summary);
                });
    }

    //show or hide Triage incidents and display message depending on sharing settings
    private void updateTriageVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            triageNotShared.setVisibility(View.GONE);
            loadTriageDisplay();
        } else {
            triageNotShared.setVisibility(View.VISIBLE);
            triageDisplay.setVisibility(View.GONE);
            noTriageText.setVisibility(View.GONE);
        }
    }

    //Get last 5 of the childs triage incidents and display the details
    private void loadTriageDisplay() {
        db.collection("children")
                .document(childId)
                .collection("incidentLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        noTriageText.setVisibility(View.VISIBLE);
                        triageDisplay.setVisibility(View.GONE);
                        return;
                    }

                    StringBuilder summary = new StringBuilder("Last 5 incidents:\n");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : result) {
                        Date timestamp = doc.getDate("timestamp");
                        String guidance = doc.getString("guidance");
                        List<String> flags = (List<String>) doc.get("flags");

                        summary.append(dateFormat.format(timestamp));
                        if (guidance != null) {
                            summary.append(" | Guidance:").append(guidance);

                        }
                        if (flags != null && !flags.isEmpty()) {
                            summary.append(" | Flags:").append(String.join(", ", flags));
                        }
                        summary.append("\n");
                    }

                    triageDisplay.setVisibility(View.VISIBLE);
                    noTriageText.setVisibility(View.GONE);
                    triageDisplay.setText(summary.toString());
                });
    }

    //show or hide escue trend and display message depending on sharing settings
    private void updateRescueTrendVisibility(Boolean isShared) {
        if (isShared != null && isShared) {
            rescueTrendNotShared.setVisibility(View.GONE);
            providerChartTrend.setVisibility(View.VISIBLE);

            // Load rescue trend for last 7 days
            loadRescueTrendChart();
        } else {
            rescueTrendNotShared.setVisibility(View.VISIBLE);
            providerChartTrend.setVisibility(View.GONE);
        }
    }


    private void loadRescueTrendChart() {
        providerChartTrend.clear();
        providerChartTrend.getDescription().setEnabled(false);
        providerChartTrend.getAxisRight().setEnabled(false);
        providerChartTrend.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        providerChartTrend.setNoDataText("");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgo = calendar.getTime();

        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
                .get()
                .addOnSuccessListener(docs -> {
                    float[] dailyCounts = new float[7];

                    for (QueryDocumentSnapshot doc : docs) {
                        Date logDate = doc.getDate("timestamp");
                        if (logDate != null) {
                            long diff = System.currentTimeMillis() - logDate.getTime();
                            int daysAgo = (int) (diff / (1000 * 60 * 60 * 24));

                            if (daysAgo >= 0 && daysAgo < 7) {
                                dailyCounts[6 - daysAgo]++; // Reverse order: oldest to newest
                            }
                        }
                    }

                    updateRescueTrendChart(dailyCounts);
                    rescueTrendDisplay.setText("Rescue usage trend loaded");
                })
                .addOnFailureListener(e -> {
                    rescueTrendDisplay.setText("Error loading trend data");
                });
    }

    private void updateRescueTrendChart(float[] dailyCounts) {
        if (dailyCounts == null || dailyCounts.length == 0) {
            providerChartTrend.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        long dayMs = 24L * 60L * 60L * 1000L;
        long fromMs = nowMs - 6L * dayMs;

        SimpleDateFormat labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (int i = 0; i < dailyCounts.length; i++) {
            long dayTime = fromMs + i * dayMs;
            Date d = new Date(dayTime);
            entries.add(new Entry(i, dailyCounts[i]));
            labels.add(labelFormat.format(d));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Rescue uses per day");
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        dataSet.setColor(getResources().getColor(R.color.LightPurple));
        dataSet.setCircleColor(getResources().getColor(R.color.Purple));

        LineData lineData = new LineData(dataSet);
        providerChartTrend.setData(lineData);

        XAxis xAxis = providerChartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(Math.min(labels.size(), 7), true);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = providerChartTrend.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        providerChartTrend.invalidate();
    }


    //given the childs schedule and logs returns number of completed planned controller days
    private int calculateCompletedDays(Map<String, Object> schedule, com.google.firebase.firestore.QuerySnapshot logs) {
        if (schedule == null || logs == null) return 0;

        int completedDays = 0;
        Calendar calendar = Calendar.getInstance();

        //go over each of the last 7 days and check in if taken controller doses matches planned required doses
        for (int i = 0; i < 7; i++) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            Date currentDay = calendar.getTime();

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int requiredDoses = getDaysRequiredDoses(schedule, dayOfWeek);
            int actualDoses = countDosesForDay(logs, currentDay);

            // Check if adherence met
            if (actualDoses == requiredDoses) {
                completedDays++;
            }
        }

        return completedDays;
    }

    //given the childs schedule and the day of the week returns number of doses planned
    private int getDaysRequiredDoses(Map<String, Object> schedule, int dayOfWeek) {
        //get the number of doses from the schedule for the parameter's day of week
        switch (dayOfWeek) {
            case Calendar.SUNDAY:    return getDoses(schedule, "sundayDoses");
            case Calendar.MONDAY:    return getDoses(schedule, "mondayDoses");
            case Calendar.TUESDAY:   return getDoses(schedule, "tuesdayDoses");
            case Calendar.WEDNESDAY: return getDoses(schedule, "wednesdayDoses");
            case Calendar.THURSDAY:  return getDoses(schedule, "thursdayDoses");
            case Calendar.FRIDAY:    return getDoses(schedule, "fridayDoses");
            case Calendar.SATURDAY:  return getDoses(schedule, "saturdayDoses");
            default: return 0;
        }
    }

    //helper to get number of required doses from schedule and make sure its an integer
    private int getDoses(Map<String, Object> schedule, String fieldName) {

        //make sure we get it as an integer because user can input a number so big its stored as long
        Object value = schedule.get(fieldName);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    //get all controller doses logged on the given day and return the count
    private int countDosesForDay(com.google.firebase.firestore.QuerySnapshot logs, Date day) {
        int count = 0;
        Calendar dayCalendar = Calendar.getInstance();
        dayCalendar.setTime(day);

        for (QueryDocumentSnapshot logDoc : logs) {
            Date logDate = logDoc.getDate("timestamp");
            if (logDate == null) continue;

            Calendar logCalendar = Calendar.getInstance();
            logCalendar.setTime(logDate);

            if (logCalendar.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                    logCalendar.get(Calendar.DAY_OF_YEAR) == dayCalendar.get(Calendar.DAY_OF_YEAR)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharingListener != null) {
            sharingListener.remove();
        }
    }
}