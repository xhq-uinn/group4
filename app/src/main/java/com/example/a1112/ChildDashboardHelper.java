package com.example.a1112;

import android.graphics.Color;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChildDashboardHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static void loadTodayZone(String childId, TextView zoneView) {
        if (childId == null || childId.isEmpty()) {
            zoneView.setText("N/A");
            zoneView.setTextColor(Color.DKGRAY);
            return;
        }

        zoneView.setText("…");
        zoneView.setTextColor(Color.DKGRAY);

        db.collection("children")
                .document(childId)
                .collection("pefLogs")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        zoneView.setText("N/A");
                        zoneView.setTextColor(Color.DKGRAY);
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String zone = doc.getString("zone");

                    if (zone == null) {
                        zoneView.setText("N/A");
                        zoneView.setTextColor(Color.DKGRAY);
                        return;
                    }

                    zoneView.setText(zone);

                    switch (zone) {
                        case "GREEN":
                            zoneView.setTextColor(Color.parseColor("#1B5E20"));
                            break;
                        case "YELLOW":
                            zoneView.setTextColor(Color.parseColor("#F9A825"));
                            break;
                        case "RED":
                            zoneView.setTextColor(Color.RED);
                            break;
                        default:
                            zoneView.setTextColor(Color.DKGRAY);
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    zoneView.setText("N/A");
                    zoneView.setTextColor(Color.DKGRAY);
                });
    }

    public static void loadWeeklyRescue(String childId, TextView weekView) {
        if (childId == null || childId.isEmpty()) {
            weekView.setText("N/A");
            return;
        }

        weekView.setText("…");

        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - 7L * 24L * 60L * 60L * 1000L;
        Date fromDate = new Date(sevenDaysAgo);

        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String type = doc.getString("type");
                        if (!"rescue".equals(type)) continue;

                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts == null) continue;

                        if (!ts.toDate().before(fromDate)) {
                            count++;
                        }
                    }

                    if (count == 0) {
                        weekView.setText("0 times");
                    } else if (count == 1) {
                        weekView.setText("1 time");
                    } else {
                        weekView.setText(count + " times");
                    }
                })
                .addOnFailureListener(e -> {
                    weekView.setText("N/A");
                });
    }


    public static void loadLastRescue(String childId, TextView lastView) {
        if (childId == null || childId.isEmpty()) {
            lastView.setText("N/A");
            return;
        }

        lastView.setText("…");

        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        lastView.setText("No rescue");
                        return;
                    }

                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    Timestamp ts = doc.getTimestamp("timestamp");
                    if (ts == null) {
                        lastView.setText("Unknown");
                        return;
                    }

                    Date date = ts.toDate();
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("MMM d, HH:mm", Locale.ENGLISH);
                    String text = sdf.format(date);
                    lastView.setText(text);
                })
                .addOnFailureListener(e -> lastView.setText("N/A"));
    }

    public static void loadRescueTrend(String childId, int days, LineChart chart) {
        if (childId == null || childId.isEmpty()) {
            chart.clear();
            return;
        }

        chart.clear();
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.setNoDataText("");

        long nowMs = System.currentTimeMillis();
        long dayMs = 24L * 60L * 60L * 1000L;
        long fromMs = nowMs - (days - 1L) * dayMs;
        Date fromDate = new Date(fromMs);

        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    float[] counts = new float[days];

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String type = doc.getString("type");
                        if (!"rescue".equals(type)) continue;

                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts == null) continue;

                        Date d = ts.toDate();
                        if (d.before(fromDate)) continue;

                        long diffDays = (nowMs - d.getTime()) / dayMs;
                        if (diffDays < 0 || diffDays >= days) continue;

                        int index = (int) (days - 1 - diffDays);
                        counts[index] += 1f;
                    }

                    SimpleDateFormat labelFormat =
                            new SimpleDateFormat("MM/dd", Locale.getDefault());

                    java.util.List<Entry> entries = new java.util.ArrayList<>();
                    java.util.List<String> labels = new java.util.ArrayList<>();

                    for (int i = 0; i < days; i++) {
                        long dayTime = fromMs + i * dayMs;
                        Date d = new Date(dayTime);
                        entries.add(new Entry(i, counts[i]));
                        labels.add(labelFormat.format(d));
                    }

                    if (entries.isEmpty()) {
                        chart.clear();
                        return;
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Rescue uses per day"); // NEW
                    dataSet.setDrawValues(false);
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleRadius(3f);
                    dataSet.setDrawCircleHole(false);
                    dataSet.setMode(LineDataSet.Mode.LINEAR);

                    dataSet.setColor(chart.getResources().getColor(R.color.LightPurple));
                    dataSet.setCircleColor(chart.getResources().getColor(R.color.Purple));

                    LineData lineData = new LineData(dataSet);
                    chart.setData(lineData);

                    XAxis xAxis = chart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                    xAxis.setLabelCount(Math.min(days, 6), true);
                    xAxis.setGranularity(1f);
                    xAxis.setGranularityEnabled(true);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    YAxis leftAxis = chart.getAxisLeft();
                    leftAxis.setAxisMinimum(0f);

                    chart.invalidate();
                })
                .addOnFailureListener(e -> {
                    chart.clear();
                });
    }


}
