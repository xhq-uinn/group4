package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Arrays;


public class ProviderHomeActivity extends AppCompatActivity {

    // Instance variables for UI components
    EditText inviteCodeEditText;
    Button submitButton;
    RecyclerView patientsRecyclerView;
    Button buttonSelectPatient;
    Button buttonSettings;
    Button buttonSignOut;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    // Stores data containing list of providers' patients
    List<String> patientList;
    String selectedPatient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components, data, and click listeners
        initializeViews();
        initializeData();
        setupPatientList();
        setupClickListeners();
    }

    void initializeViews() {

        // Initialize UI components

        inviteCodeEditText = findViewById(R.id.inviteCodeEditText);
        submitButton = findViewById(R.id.submitButton);
        patientsRecyclerView = findViewById(R.id.patientsRecyclerView);
        buttonSelectPatient = findViewById(R.id.buttonSelectPatient);
        buttonSettings = findViewById(R.id.buttonSettings);
        buttonSignOut = findViewById(R.id.buttonSignOut);

        // Setup RecyclerView layout manager
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    void initializeData() {
        patientList = new ArrayList<>();

        loadPatientsFromDatabase();
    }

    void setupPatientList() {
        PatientAdapter adapter = new PatientAdapter(patientList, new PatientAdapter.OnPatientClickListener() {
            @Override
            public void onPatientClick(String patientName) {
                selectedPatient = patientName;
                buttonSelectPatient.setEnabled(true);
                buttonSelectPatient.setText("Select Patient: " + patientName);
                Toast.makeText(ProviderHomeActivity.this, "Selected: " + patientName, Toast.LENGTH_SHORT).show();
            }
        });

        patientsRecyclerView.setAdapter(adapter);
    }

    void setupClickListeners() {


        // Invite code submission
        submitButton.setOnClickListener(v -> {
            String inviteCode = inviteCodeEditText.getText().toString().trim();
            if (inviteCode.isEmpty()) {
                Toast.makeText(this, "Please enter an invite code", Toast.LENGTH_SHORT).show();
            } else {
                // Implement code validation logic here
                validateInviteCode(inviteCode);
                ;
            }
        });


        buttonSelectPatient.setOnClickListener(v -> {
            if (selectedPatient != null) {
                // Implement routing to patient details screen
                Toast.makeText(this, "Showing details for: " + selectedPatient, Toast.LENGTH_SHORT).show();
            }
        });


        buttonSettings.setOnClickListener(v -> {
            // Implement routing to Settings activity
            Toast.makeText(this, "Settings screen coming soon", Toast.LENGTH_SHORT).show();
        });


        buttonSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProviderHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        });
    }

    private void loadPatientsFromDatabase() {
    }

    private void validateInviteCode(String inviteCode) {

    }
}