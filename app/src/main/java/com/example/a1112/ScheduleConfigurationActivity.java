package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ScheduleConfigurationActivity extends AppCompatActivity{
    private EditText mondayDoses, tuesdayDoses, wednesdayDoses, thursdayDoses, fridayDoses, saturdayDoses, sundayDoses;
    private Button buttonSaveSchedule, buttonBack;

    private FirebaseFirestore db;
    private String currentChildId;
    private String currentChildName;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_configuration);

        Intent intent = getIntent();
        currentChildId = intent.getStringExtra("CHILD_ID");
        currentChildName = intent.getStringExtra("CHILD_NAME");
        userType = intent.getStringExtra("USER_TYPE");

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadExistingSchedule();
    }

    private void initializeViews() {
        mondayDoses = findViewById(R.id.mondayDoses);
        tuesdayDoses = findViewById(R.id.tuesdayDoses);
        wednesdayDoses = findViewById(R.id.wednesdayDoses);
        thursdayDoses = findViewById(R.id.thursdayDoses);
        fridayDoses = findViewById(R.id.fridayDoses);
        saturdayDoses = findViewById(R.id.saturdayDoses);
        sundayDoses = findViewById(R.id.sundayDoses);

        buttonSaveSchedule = findViewById(R.id.buttonSaveSchedule);
        buttonBack = findViewById(R.id.buttonBack);
    }

    private void setupClickListeners() {
        buttonSaveSchedule.setOnClickListener(v -> saveSchedule());

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleConfigurationActivity.this, MedicineLogActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("CHILD_NAME", currentChildName);
            intent.putExtra("USER_TYPE", userType);
            startActivity(intent);
            finish();
        });
    }

    //get current schedule and set input text to start on those values
    private void loadExistingSchedule() {
        db.collection("controllerSchedules").document(currentChildId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mondayDoses.setText(String.valueOf(documentSnapshot.getLong("mondayDoses").intValue()));
                        tuesdayDoses.setText(String.valueOf(documentSnapshot.getLong("tuesdayDoses").intValue()));
                        wednesdayDoses.setText(String.valueOf(documentSnapshot.getLong("wednesdayDoses").intValue()));
                        thursdayDoses.setText(String.valueOf(documentSnapshot.getLong("thursdayDoses").intValue()));
                        fridayDoses.setText(String.valueOf(documentSnapshot.getLong("fridayDoses").intValue()));
                        saturdayDoses.setText(String.valueOf(documentSnapshot.getLong("saturdayDoses").intValue()));
                        sundayDoses.setText(String.valueOf(documentSnapshot.getLong("sundayDoses").intValue()));
                    }
                });
    }

    //store schedule on firestore database if no inputs cause error
    private void saveSchedule() {

        //get input field text and make sure they're all filled in
        String mondayString = mondayDoses.getText().toString().trim();
        String tuesdayString = tuesdayDoses.getText().toString().trim();
        String wednesdayString = wednesdayDoses.getText().toString().trim();
        String thursdayString = thursdayDoses.getText().toString().trim();
        String fridayString = fridayDoses.getText().toString().trim();
        String saturdayString = saturdayDoses.getText().toString().trim();
        String sundayString = sundayDoses.getText().toString().trim();

        if (mondayString.isEmpty() || tuesdayString.isEmpty() || wednesdayString.isEmpty() ||
                thursdayString.isEmpty() || fridayString.isEmpty() || saturdayString.isEmpty() ||
                sundayString.isEmpty()) {
            Toast.makeText(this, "Please fill in all days", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int monday = Integer.parseInt(mondayString);
            int tuesday = Integer.parseInt(tuesdayString);
            int wednesday = Integer.parseInt(wednesdayString);
            int thursday = Integer.parseInt(thursdayString);
            int friday = Integer.parseInt(fridayString);
            int saturday = Integer.parseInt(saturdayString);
            int sunday = Integer.parseInt(sundayString);

            Map<String, Object> scheduleData = new HashMap<>();
            scheduleData.put("childId", currentChildId);
            scheduleData.put("mondayDoses", monday);
            scheduleData.put("tuesdayDoses", tuesday);
            scheduleData.put("wednesdayDoses", wednesday);
            scheduleData.put("thursdayDoses", thursday);
            scheduleData.put("fridayDoses", friday);
            scheduleData.put("saturdayDoses", saturday);
            scheduleData.put("sundayDoses", sunday);

            db.collection("controllerSchedules").document(currentChildId)
                    .set(scheduleData)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Schedule saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving schedule", Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}
