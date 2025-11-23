package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


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
    List<String> patientChildIds;
    String selectedPatient = null;
    PatientAdapter adapter;

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
        loadPatientsFromDatabase();
    }

    void initializeViews() {

        // Initialize UI components

        inviteCodeEditText = findViewById(R.id.inviteCodeEditText);
        submitButton = findViewById(R.id.submitButton);
        patientsRecyclerView = findViewById(R.id.patientsRecyclerView);
        buttonSelectPatient = findViewById(R.id.buttonSelectPatient);
//        buttonSettings = findViewById(R.id.buttonSettings);
        buttonSignOut = findViewById(R.id.buttonSignOut);

        // Setup RecyclerView layout manager
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    void initializeData() {
        patientList = new ArrayList<>();
        patientChildIds = new ArrayList<>();
    }

    void setupPatientList() {
        adapter = new PatientAdapter(patientList, new PatientAdapter.OnPatientClickListener() {
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


//        buttonSettings.setOnClickListener(v -> {
//            // Implement routing to Settings activity
//            Toast.makeText(this, "Settings screen coming soon", Toast.LENGTH_SHORT).show();
//        });


        buttonSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProviderHomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        });
    }

    private void loadPatientsFromDatabase() {
        if (adapter == null) return;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String providerId = user.getUid();

        db.collection("users")
                .document(providerId)
                .collection("linkedChildren")
                .get()
                .addOnSuccessListener(snapshot -> {

                    patientList.clear();
                    patientChildIds.clear();

                    if (snapshot.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot linkDoc : snapshot.getDocuments()) {
                        String childId = linkDoc.getString("childId");
                        if (childId == null) continue;

                        patientChildIds.add(childId);

                        // Fetch child data from children -> childId
                        db.collection("children")
                                .document(childId)
                                .get()
                                .addOnSuccessListener(childDoc -> {
                                    if (childDoc.exists()) {
                                        String name = childDoc.getString("name");
                                        if (name == null) name = "(Unnamed Child)";
                                        patientList.add(name);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed loading patients: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //provider accepts invite
    private void validateInviteCode(String inviteCode) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String providerId = user.getUid();

        db.collection("invites")
                .document(inviteCode)    // Because code is the docID
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this, "Invalid invite code", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Boolean used = doc.getBoolean("used");
                    if (used != null && used) {
                        Toast.makeText(this, "This code has already been used.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String parentId = doc.getString("parentId");
                    String childId = doc.getString("childId");

                    if (parentId == null || childId == null) {
                        Toast.makeText(this, "Invite missing data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Create link in provider's linkedChildren
                    Map<String, Object> linkData = new HashMap<>();
                    linkData.put("parentId", parentId);
                    linkData.put("childId", childId);
                    linkData.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(providerId)
                            .collection("linkedChildren")
                            .add(linkData)
                            .addOnSuccessListener(ref -> {

                                //Mark invite as used
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("used", true);
                                updates.put("usedByProviderId", providerId);

                                doc.getReference().update(updates);

                                Toast.makeText(this, "Invite Accepted", Toast.LENGTH_SHORT).show();
                                inviteCodeEditText.setText("");

                                //Reload patient list
                                loadPatientsFromDatabase();

                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error validating code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}