package com.example.a1112;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class ProviderReportActivity extends AppCompatActivity {

    // UI components
    private TextView textTitle, rescueFrequencyText, controllerAdherenceText, noZoneDataText, noSymptomDataText, periodDisplayText;
    private TextView  noTriageDataText;
    private BarChart symptomBurdenChart;
    private LineChart zoneDistributionChart;
    private Button buttonExportPdf, buttonHome;
    private Spinner timePeriodSpinner, providerSpinner;
    private LinearLayout reportContent, triageIncidentsContainer;

    private FirebaseFirestore db;
    private String childId;
    private String childName;

    private int reportPeriodMonths = -3;

    // store list of shared providers with their provider IDs and emails
    private List<ProviderInfo> sharedProviders = new ArrayList<>();
    private String selectedProviderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_report);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupTimePeriodSpinner();
        loadSharedProviders();
        setupClickListeners();
        displayReportInfo();
    }

    private void initializeViews() {
        rescueFrequencyText = findViewById(R.id.rescueFrequencyText);
        controllerAdherenceText = findViewById(R.id.controllerAdherenceText);
        textTitle = findViewById(R.id.textTitle);
        noZoneDataText = findViewById(R.id.noZoneDataText);
        noSymptomDataText = findViewById(R.id.noSymptomDataText);
        periodDisplayText = findViewById(R.id.periodDisplayText);
        noTriageDataText = findViewById(R.id.noTriageDataText);
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner);
        providerSpinner = findViewById(R.id.providerSpinner);
        symptomBurdenChart = findViewById(R.id.symptomBurdenChart);
        zoneDistributionChart = findViewById(R.id.zoneDistributionChart);
        buttonExportPdf = findViewById(R.id.buttonExportPdf);
        buttonHome = findViewById(R.id.buttonHome);
        reportContent = findViewById(R.id.report_content);
        triageIncidentsContainer = findViewById(R.id.triageIncidentsContainer);

        textTitle.setText(String.format("%s's Provider Report", childName));
    }


    //set up a spinner to choose last x months to be viewed and update the report
    private void setupTimePeriodSpinner() {

        String[] timePeriods = {"3 months", "4 months", "5 months", "6 months"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, timePeriods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        timePeriodSpinner.setAdapter(adapter);

        timePeriodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //when a spinner option is selected change the report period and period display and reload data
                reportPeriodMonths = -3 - position;
                periodDisplayText.setText(String.format("Last %d months", -reportPeriodMonths));

                displayReportInfo();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // load all shared providers from the sharedProviders list field and get their emails
    private void loadSharedProviders() {
        db.collection("children").document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {
                    sharedProviders.clear();

                    // get the list of provider IDs from the sharedProviders field
                    List<String> providerIds = (List<String>) childDoc.get("sharedProviders");

                    if (providerIds == null || providerIds.isEmpty()) {
                        setupProviderSpinner(new ArrayList<>());
                        return;
                    }

                    // for each provider ID fetch their email from the users collection
                    final int[] loadedCount = {0};
                    for (String providerId : providerIds) {
                        db.collection("users").document(providerId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String email = userDoc.getString("email");
                                    if (email != null) {
                                        sharedProviders.add(new ProviderInfo(providerId, email));
                                    }

                                    loadedCount[0]++;
                                    // once all providers are loaded setup the spinner
                                    if (loadedCount[0] == providerIds.size()) {
                                        setupProviderSpinner(sharedProviders);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == providerIds.size()) {
                                        setupProviderSpinner(sharedProviders);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading providers", Toast.LENGTH_SHORT).show();
                });
    }

    // setup the provider spinner with list of provider emails
    private void setupProviderSpinner(List<ProviderInfo> providers) {
        List<String> displayNames = new ArrayList<>();

        if (providers.isEmpty()) {
            displayNames.add("No providers shared");
        } else {
            for (ProviderInfo provider : providers) {
                displayNames.add(provider.email);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        providerSpinner.setAdapter(adapter);

        providerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // when provider is selected update the selected ID and reload triage incidents
                if (position < sharedProviders.size()) {
                    selectedProviderId = sharedProviders.get(position).providerId;
                    displayTriageIncidents();
                } else {
                    selectedProviderId = "";
                    hideTriageIncidents();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private void setupClickListeners() {
        buttonExportPdf.setOnClickListener(v -> createPdf());
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProviderReportActivity.this, ParentHomeActivity.class);
            startActivity(intent);
        });
    }



    private void displayReportInfo() {
        displayRescueFrequency();
        displayControllerAdherence();
        getSymptomBurdenInfo();
        getZoneDistributionInfo();
        displayTriageIncidents();
    }

    //get all rescue logs within the given last months to now and calculate the average rescue per day
    private void displayRescueFrequency() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, reportPeriodMonths);
        Date startDate = cal.getTime();

        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .get()
                .addOnSuccessListener(result -> {
                    int rescueCount = result.size();
                    long diff = new Date().getTime() - startDate.getTime();
                    long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                    if (days > 0) {
                        float avgPerDay = (float) rescueCount / days;
                        rescueFrequencyText.setText(String.format("%.2f", avgPerDay));
                    } else {
                        rescueFrequencyText.setText("0.00");
                    }
                });
    }


    //get all controller logs and planned controller days within the given last months to now and calculate the adherence percentage
    private void displayControllerAdherence() {
        db.collection("controllerSchedules")
                .document(childId)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.exists()) {
                        controllerAdherenceText.setText("No schedule configured");
                        return;
                    }
                    Map<String, Object> schedule = result.getData();

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, reportPeriodMonths);
                    Date startDate = cal.getTime();

                    db.collection("medicineLogs")
                            .whereEqualTo("childId", childId)
                            .whereEqualTo("type", "controller")
                            .whereGreaterThanOrEqualTo("timestamp", startDate)
                            .get()
                            .addOnSuccessListener(logs -> {
                                int totalPlannedDays = 0;
                                int completedDays = 0;

                                //iterate over all needed the dates and count days with doses and completed ones
                                Calendar iterator = Calendar.getInstance();
                                iterator.setTime(startDate);
                                Date today = new Date();

                                while (iterator.getTime().before(today)) {
                                    int dayOfWeek = iterator.get(Calendar.DAY_OF_WEEK);
                                    int requiredDoses = getDaysRequiredDoses(schedule, dayOfWeek);

                                    // only count days that have planned doses
                                    if (requiredDoses > 0) {
                                        totalPlannedDays++;
                                        int actualDoses = countDosesForDay(logs, iterator.getTime());
                                        if (actualDoses >= requiredDoses) {
                                            completedDays++;
                                        }
                                    }
                                    iterator.add(Calendar.DAY_OF_YEAR, 1);
                                }


                                if (totalPlannedDays > 0) {
                                    double adherencePercentage = ((double) completedDays / totalPlannedDays) * 100;
                                    controllerAdherenceText.setText(String.format("%.0f%%", adherencePercentage));
                                } else {
                                    controllerAdherenceText.setText("No planned doses in period");
                                }
                            });
                });
    }


    //given the childs schedule and the day of the week returns number of doses planned
    private int getDaysRequiredDoses(Map<String, Object> schedule, int dayOfWeek) {
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
        Object value = schedule.get(fieldName);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    //get all controller doses logged on the given day and return the count
    private int countDosesForDay(QuerySnapshot logs, Date day) {
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


    //go over all daily checkins in the time frame and count days with symptoms and calculate days without
    private void getSymptomBurdenInfo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, reportPeriodMonths);
        Date startDate = cal.getTime();

        db.collection("children").document(childId).collection("dailyCheckins")
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .get()
                .addOnSuccessListener(result -> {
                    HashSet<String> symptomDays = new HashSet<>();
                    HashSet<String> allCheckinDays = new HashSet<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

                    for (DocumentSnapshot doc : result.getDocuments()) {
                        if (doc.exists()) {
                            Date createdAt = doc.getDate("createdAt");
                            if (createdAt != null) {
                                String dayString = dateFormat.format(createdAt);
                                allCheckinDays.add(dayString);

                                String activityLimit = doc.getString("activityLimit");
                                String nightWaking = doc.getString("nightWaking");
                                String coughWheeze = doc.getString("coughWheeze");
                                if ((activityLimit != null && !activityLimit.equals("none")) ||
                                        (nightWaking != null && !nightWaking.equals("none")) ||
                                        (coughWheeze != null && !coughWheeze.equals("none"))) {
                                    symptomDays.add(dayString);
                                }
                            }
                        }
                    }
                    int daysWithSymptoms = symptomDays.size();
                    int daysWithoutSymptoms = allCheckinDays.size() - daysWithSymptoms;

                    setupSymptomBurdenChart(daysWithSymptoms, daysWithoutSymptoms);
                });
    }

    //get all pef logs in the time frame and group them by zone and display the distribution
    private void getZoneDistributionInfo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, reportPeriodMonths);
        Date startDate = cal.getTime();

        db.collection("children").document(childId).collection("pefLogs")
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    List<Entry> entries = new ArrayList<>();
                    List<String> dateLabels = new ArrayList<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

                    for (DocumentSnapshot doc : result.getDocuments()) {
                        if (doc.exists()) {
                            Date date = doc.getDate("date");
                            String zone = doc.getString("zone");
                            if (date != null && zone != null) {
                                int zoneValue = 0;
                                if (zone.equalsIgnoreCase("Green")) {
                                    zoneValue = 1;
                                } else if (zone.equalsIgnoreCase("Yellow")) {
                                    zoneValue = 2;
                                } else if (zone.equalsIgnoreCase("Red")) {
                                    zoneValue = 3;
                                }
                                entries.add(new Entry(entries.size(), zoneValue));
                                dateLabels.add(dateFormat.format(date));
                            }
                        }
                    }
                    setupZoneDistributionChart(entries, dateLabels);
                });
    }

    // check if selected provider has triage sharing enabled and display last 5 triage incidents
    private void displayTriageIncidents() {
        // if no provider selected hide the section
        if (selectedProviderId == null || selectedProviderId.isEmpty()) {
            hideTriageIncidents();
            return;
        }

        // find the sharing settings document for this provider to check if triage is enabled
        db.collection("children").document(childId).collection("sharingSettings")
                .whereEqualTo("providerId", selectedProviderId)
                .get()
                .addOnSuccessListener(settingsDocs -> {
                    if (settingsDocs.isEmpty()) {
                        hideTriageIncidents();
                        return;
                    }

                    DocumentSnapshot settingsDoc = settingsDocs.getDocuments().get(0);
                    Boolean triageEnabled = settingsDoc.getBoolean("triageIncidents");

                    // only show if triage incidents are shared
                    if (triageEnabled == null || !triageEnabled) {
                        hideTriageIncidents();
                        return;
                    }

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, reportPeriodMonths);
                    Date startDate = cal.getTime();

                    // get all triage incidents from incidentLogs in timeframe
                    db.collection("children").document(childId).collection("incidentLogs")
                            .whereGreaterThanOrEqualTo("timestamp", startDate)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(result -> {
                                if (result.isEmpty()) {
                                    showNoTriageData();
                                    return;
                                }

                                // clear previous incident views and add new ones
                                triageIncidentsContainer.removeAllViews();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

                                for (QueryDocumentSnapshot doc : result) {
                                    // create a text view for each incident with timestamp guidance and flags
                                    TextView incidentView = new TextView(this);
                                    Date timestamp = doc.getDate("timestamp");
                                    String guidance = doc.getString("guidance");
                                    List<String> flags = (List<String>) doc.get("flags");

                                    StringBuilder incidentText = new StringBuilder();
                                    incidentText.append(timestamp != null ? dateFormat.format(timestamp) : "Unknown date");

                                    if (guidance != null) {
                                        incidentText.append(" | Guidance: ").append(guidance);
                                    }

                                    if (flags != null && !flags.isEmpty()) {
                                        incidentText.append(" | Flags: ").append(String.join(", ", flags));
                                    }

                                    incidentText.append("\n");

                                    incidentView.setText(incidentText.toString());
                                    incidentView.setTextSize(14);
                                    incidentView.setPadding(0, 16, 0, 16);

                                    triageIncidentsContainer.addView(incidentView);
                                }

                                // show the triage section
                                triageIncidentsContainer.setVisibility(View.VISIBLE);
                                noTriageDataText.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error loading triage incidents", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideTriageIncidents();
                });
    }

    // hide all triage incident views when sharing is disabled
    private void hideTriageIncidents() {
        triageIncidentsContainer.setVisibility(View.GONE);
        noTriageDataText.setVisibility(View.VISIBLE);
    }

    // show message when no triage incidents exist but sharing is enabled
    private void showNoTriageData() {
        triageIncidentsContainer.setVisibility(View.GONE);
        noTriageDataText.setText("No triage incidents recorded");
        noTriageDataText.setVisibility(View.VISIBLE);
    }

    //given count of days with symptoms (bar1) and without symptoms (bar2) display a bar chart
    private void setupSymptomBurdenChart(int daysWithSymptoms, int daysWithoutSymptoms) {
        // if no symptom data hide chart and show no data message
        if (daysWithSymptoms + daysWithoutSymptoms == 0) {
            symptomBurdenChart.setVisibility(View.GONE);
            noSymptomDataText.setVisibility(View.VISIBLE);
            return;
        }

        // create bar entries for days with and without symptoms
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, daysWithSymptoms));
        entries.add(new BarEntry(1f, daysWithoutSymptoms));

        // create dataset and set bar colors orange for with symptoms and green for without
        BarDataSet set = new BarDataSet(entries, "Symptom Burden");
        set.setColors(new int[]{getResources().getColor(R.color.Orange), getResources().getColor(R.color.Green)});

        // create bar data and set bar width
        BarData data = new BarData(set);
        data.setBarWidth(0.5f);

        // configure chart appearance and axis settings
        symptomBurdenChart.setData(data);
        symptomBurdenChart.getDescription().setEnabled(false);
        symptomBurdenChart.getLegend().setEnabled(false);
        symptomBurdenChart.getAxisRight().setEnabled(false);
        symptomBurdenChart.getAxisLeft().setAxisMinimum(0f);

        // set x axis labels for the two bars
        XAxis xAxis = symptomBurdenChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Days Reported With Symptoms", "Days Reported Without Symptoms"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        // refresh chart to display
        symptomBurdenChart.invalidate();
    }

    //given list of entries and dates for zone values display a line chart
    private void setupZoneDistributionChart(List<Entry> entries, List<String> dateLabels) {
        // if no zone data hide chart and show no data message
        if (entries.isEmpty()) {
            zoneDistributionChart.setVisibility(View.GONE);
            noZoneDataText.setVisibility(View.VISIBLE);
            return;
        }

        // create line dataset and set colors and line style
        LineDataSet dataSet = new LineDataSet(entries, "Zone Distribution");
        dataSet.setCircleColor(getResources().getColor(R.color.Purple));
        dataSet.setColor(getResources().getColor(R.color.LightPurple));
        dataSet.setLineWidth(2f);

        //display sample point circles differently or not at all based on number of entries for better visibility
        if (entries.size() > 50) {
            dataSet.setCircleRadius(2f);
            dataSet.setDrawCircles(entries.size() <= 100);
            dataSet.setDrawCircleHole(false);
        } else if (entries.size() > 20) {
            dataSet.setCircleRadius(3f);
            dataSet.setDrawCircles(true);
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleRadius(1.5f);
        } else {
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircles(true);
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleRadius(2f);
        }

        //doesnt display y value for each sample point
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "";
            }
        });

        // set line data to chart and configure basic appearance
        LineData lineData = new LineData(dataSet);
        zoneDistributionChart.setData(lineData);
        zoneDistributionChart.getDescription().setEnabled(false);
        zoneDistributionChart.getAxisRight().setEnabled(false);
        zoneDistributionChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        // set x axis to show dates with a max of 6 labels
        if (!dateLabels.isEmpty()) {
            zoneDistributionChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateLabels));
            zoneDistributionChart.getXAxis().setLabelCount(Math.min(dateLabels.size(), 6), true);
        }

        // configure y axis to show zone names instead of numbers
        zoneDistributionChart.getAxisLeft().setAxisMinimum(0f);
        zoneDistributionChart.getAxisLeft().setAxisMaximum(4f);
        zoneDistributionChart.getAxisLeft().setGranularity(1f);
        zoneDistributionChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                if (value == 1f) return "Green";
                if (value == 2f) return "Yellow";
                if (value == 3f) return "Red";
                return "";
            }
        });

        // refresh chart to display
        zoneDistributionChart.invalidate();
    }

    //create a pdf and store it in the device using the layout
    private void createPdf() {
        //hide buttons before creating and make sure layout updates
        buttonExportPdf.setVisibility(View.GONE);
        buttonHome.setVisibility(View.GONE);
        reportContent.invalidate();
        reportContent.requestLayout();

        //create a bitmap of the layout and a pdf with same dimensions and draw the bitmap onto the pdf
        Bitmap bitmap = Bitmap.createBitmap(reportContent.getWidth(), reportContent.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        reportContent.draw(canvas);

        //restore button visibility
        buttonExportPdf.setVisibility(View.VISIBLE);
        buttonHome.setVisibility(View.VISIBLE);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas pageCanvas = page.getCanvas();
        pageCanvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();
        String dateString = dateFormat.format(today);


        //create file and write the pdf to file and prompt user to share using a sharing intent
        String fileName = childName + "'s ProviderReport_" + dateString + ".pdf";
        File filePath = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            document.writeTo(new FileOutputStream(filePath));
            Toast.makeText(this, "PDF saved to " + filePath.getAbsolutePath(), Toast.LENGTH_LONG).show();
            sharePdf(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        document.close();
    }

    //share pdf using a sharing intent
    private void sharePdf(File file) {
        //convert to uri so apps can access it
        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

        //create sharing intent with reading permissions for apps that handle pdfs
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share PDF"));
    }


}