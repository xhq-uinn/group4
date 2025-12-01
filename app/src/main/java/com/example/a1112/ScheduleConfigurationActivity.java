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
        try {
            int monday = Integer.parseInt(mondayDoses.getText().toString());
            int tuesday = Integer.parseInt(tuesdayDoses.getText().toString());
            int wednesday = Integer.parseInt(wednesdayDoses.getText().toString());
            int thursday = Integer.parseInt(thursdayDoses.getText().toString());
            int friday = Integer.parseInt(fridayDoses.getText().toString());
            int saturday = Integer.parseInt(saturdayDoses.getText().toString());
            int sunday = Integer.parseInt(sundayDoses.getText().toString());

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
