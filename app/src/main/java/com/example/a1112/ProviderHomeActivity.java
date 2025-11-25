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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
    private ListenerRegistration patientsListener;



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
        //buttonSettings = findViewById(R.id.buttonSettings); this line error so i changed it
        buttonSignOut = findViewById(R.id.buttonSignOut);

        // Setup RecyclerView layout manager
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    void initializeData() {
        patientList = new ArrayList<>();
        setupRealTimePatientUpdates();
    }

    void setupPatientList() {
        PatientAdapter adapter = new PatientAdapter(patientList, new PatientAdapter.OnPatientClickListener() {
            @Override
            public void onPatientClick(String patientName) {
                selectedPatient = patientName;
                buttonSelectPatient.setEnabled(true);
                buttonSelectPatient.setText("Selected Patient: " + patientName);
                Toast.makeText(ProviderHomeActivity.this, "Selected: " + patientName, Toast.LENGTH_SHORT).show();
            }
        });

        patientsRecyclerView.setAdapter(adapter);
    }

    void setupClickListeners() {


        // Invite code submission
        submitButton.setOnClickListener(v -> {
            String inviteCode = inviteCodeEditText.getText().toString().trim();
            //display message for empty input
            if (inviteCode.isEmpty()) {
                Toast.makeText(this, "Please enter an invite code", Toast.LENGTH_SHORT).show();
                return;
            }

            // disable the button while checking and display "validating"
            submitButton.setEnabled(false);
            submitButton.setText("Validating...");

            // Check if sumbitted invite code matches an invites id in the database
            db.collection("invites").document(inviteCode)
                    .get()
                    .addOnCompleteListener(task -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Submit");

                        // Perform subsequent checks if we find a match to determine validity like (expiration, if used)
                        if (task.isSuccessful() && task.getResult().exists()) {
                            //get all the information about the invite code instance in the database
                            DocumentSnapshot inviteDoc = task.getResult();
                            Boolean used = inviteDoc.getBoolean("used");
                            String parentId = inviteDoc.getString("parentId");
                            Date createdAt = inviteDoc.getDate("createdAt");

                            // Check if invite is valid

                            //already used invite code
                            if (used != null && used) {
                                Toast.makeText(this, "This invite code has already been used", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Check if invite has expired (7 days)
                            if (isInviteExpired(createdAt)) {
                                Toast.makeText(this, "This invite code has expired", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String providerId = getCurrentProviderId();
                            //valid invite code, then we link the provider to all children of the parent.
                            linkParentChildrenToProvider(parentId, inviteCode, providerId);

                        } else {
                            //Display message for when the invite code does not match an invite id in database
                            Toast.makeText(this, "Invalid invite code", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Submit");
                        Toast.makeText(this, "Error validating code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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

    private void setupRealTimePatientUpdates() {
        String providerId = getCurrentProviderId();
        if (providerId == null) return;

        // A real-time listener for all children who have the current provider in their list of sharedProviders
        patientsListener = db.collection("children")
                .whereArrayContains("sharedProviders", providerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading patients", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //No error, we process the updated data and modify the list to be displayed accordingly
                    if (value != null) {
                        patientList.clear();
                        // go over all matches to our query(children linked to current provider)
                        //extract the child's name and add it to patient list
                        for (QueryDocumentSnapshot document : value) {
                            String patientName = document.getString("name");
                            if (patientName != null) {
                                patientList.add(patientName);
                            }
                        }

                        // Then just update the recyclerview
                        if (patientsRecyclerView.getAdapter() != null) {
                            patientsRecyclerView.getAdapter().notifyDataSetChanged();
                        }

                        // Persistent display message if there are currently no children in the list
                        if (patientList.isEmpty()) {
                            Toast.makeText(this, "No patients found. Use invite codes to add patients.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void linkParentChildrenToProvider(String parentId, String inviteCode, String providerId) {
        if (providerId == null) {
            Toast.makeText(this, "Error: Provider not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find all children linked to the parent
        db.collection("children")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {

                        // Add provider to each child's sharedproviders list
                        for (QueryDocumentSnapshot childDoc : task.getResult()) {
                            String childId = childDoc.getId();

                            // Update child to include this provider in their providers list
                            db.collection("children").document(childId)
                                    .update("sharedProviders", FieldValue.arrayUnion(providerId))
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error linking patient: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                        //In the invites collection, change "used" field value to True
                        markInviteAsUsed(inviteCode);



                    } else {
                        //error message if the parent is not linked to any children
                        Toast.makeText(this, "No children found for this invite", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error finding children: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //gets the id of the user
    private String getCurrentProviderId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }
    //marks an invite as used when used by a provider
    private void markInviteAsUsed(String inviteCode) {
        db.collection("invites").document(inviteCode)
                .update("used", true)
                .addOnSuccessListener(aVoid -> {
                    inviteCodeEditText.setText("");
                    Toast.makeText(this, "Patients linked successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    //checks if an invite has expired(7 days gone by) and returns True if expired
    private boolean isInviteExpired(Date createdAt) {

        long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds
        //checks if current time to invite creation time has been longer than 7 days
        return (System.currentTimeMillis() - createdAt.getTime()) > sevenDaysInMillis;
    }

    //overriding default onDestroy method to add the functionality of removing our real-time listener when activity is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (patientsListener != null) {
            patientsListener.remove();
        }
    }

}
