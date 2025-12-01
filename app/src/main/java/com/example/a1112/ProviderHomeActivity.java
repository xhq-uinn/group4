package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProviderHomeActivity extends AppCompatActivity {

    // Instance variables for UI components

    EditText inviteCodeEditText;
    Button submitButton, buttonViewDetails;
    RecyclerView patientsRecyclerView;
    Button buttonSignOut;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration patientsListener;

    // Stores data containing list of providers' patients

    List<Child> patientList;
    Child selectedChild = null;

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
        inviteCodeEditText = findViewById(R.id.inviteCodeEditText);
        submitButton = findViewById(R.id.submitButton);
        buttonViewDetails = findViewById(R.id.buttonViewDetails);
        patientsRecyclerView = findViewById(R.id.patientsRecyclerView);
        buttonSignOut = findViewById(R.id.buttonSignOut);

        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    void initializeData() {
        patientList = new ArrayList<>();
        setupRealTimePatientUpdates();
    }

    void setupPatientList() {
        PatientAdapter adapter = new PatientAdapter(
                patientList,
                child -> {
                    selectedChild = child;

                    Toast.makeText(this, "Selected: " + child.getName(), Toast.LENGTH_SHORT).show();

                    buttonViewDetails.setEnabled(true);
                    buttonViewDetails.setText("View Details for " + child.getName());
                });
        patientsRecyclerView.setAdapter(adapter);
    }

    void setupClickListeners() {

        // Provider submits invite code
        submitButton.setOnClickListener(v -> {
            String inviteCode = inviteCodeEditText.getText().toString().trim();

            if (inviteCode.isEmpty()) {
                Toast.makeText(this, "Please enter an invite code", Toast.LENGTH_SHORT).show();
                return;
            }

            submitButton.setEnabled(false);
            submitButton.setText("Validating...");

            db.collection("invites").document(inviteCode)
                    .get()
                    .addOnCompleteListener(task -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Submit");

                        if (!task.isSuccessful()) {
                            Toast.makeText(this, "Error validating code", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot inviteDoc = task.getResult();

                        if (!inviteDoc.exists()) {
                            Toast.makeText(this, "Invalid invite code", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Boolean used = inviteDoc.getBoolean("used");
                        String parentId = inviteDoc.getString("parentId");
                        String childId = inviteDoc.getString("childId");
                        Date createdAt = inviteDoc.getDate("createdAt");

                        if (used != null && used) {
                            Toast.makeText(this, "This code has already been used", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (createdAt == null || isInviteExpired(createdAt)) {
                            Toast.makeText(this, "Invite code expired", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String providerId = getCurrentProviderId();
                        linkChildToProvider(childId, inviteCode, providerId);
                    })
                    .addOnFailureListener(e -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Submit");
                        Toast.makeText(this, "Error validating code", Toast.LENGTH_SHORT).show();
                    });
        });

        buttonViewDetails.setOnClickListener(v -> {
            if (selectedChild != null) {
                Intent intent = new Intent(ProviderHomeActivity.this, ProviderMainActivity.class);
                intent.putExtra("PROVIDER_ID", getCurrentProviderId());
                intent.putExtra("CHILD_ID", selectedChild.getId());
                intent.putExtra("CHILD_NAME", selectedChild.getName());
                startActivity(intent);
            }
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

        patientsListener = db.collection("children")
                .whereArrayContains("sharedProviders", providerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value == null) return;

                    patientList.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        String childId = doc.getId();
                        String childName = doc.getString("name");

                        Child child = new Child();
                        child.setId(childId);
                        child.setName(childName);

                        patientList.add(child);

                    }

                    if (patientsRecyclerView.getAdapter() != null) {
                        patientsRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private void linkChildToProvider(String childId, String inviteCode, String providerId) {
        if (providerId == null) {
            Toast.makeText(this, "Error: Provider not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("children")
                .document(childId)
                .update("sharedProviders", FieldValue.arrayUnion(providerId))
                .addOnSuccessListener(aVoid -> updateInviteAndSharingSettings(childId, inviteCode, providerId))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error linking patient: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void updateInviteAndSharingSettings(String childId, String inviteCode, String providerId) {
        WriteBatch batch = db.batch();

        // Update invites/{inviteCode} (Mark as used)
        batch.update(
                db.collection("invites").document(inviteCode),
                "used", true,
                "usedByProviderId", providerId
        );

        // Update children/{childId}/sharingSettings/{inviteCode} (Set providerId)
        // This is the missing piece to populate the providerId field in the sharing settings document.
        batch.update(
                db.collection("children")
                        .document(childId)
                        .collection("sharingSettings")
                        .document(inviteCode),
                "providerId", providerId
        );

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    inviteCodeEditText.setText("");
                    Toast.makeText(this, "Child linked! Settings updated.", Toast.LENGTH_SHORT).show();
                    // setupRealTimePatientUpdates() 会自动触发，更新列表
                })
                .addOnFailureListener(e -> {
                    Log.e("ProviderHome", "Batch write failed: " + e.getMessage());
                    Toast.makeText(this, "Error updating invite records.", Toast.LENGTH_SHORT).show();
                });
    }
    private String getCurrentProviderId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }


    private void markInviteAsUsed(String inviteCode, String providerId) {
        db.collection("invites").document(inviteCode)
                .update(
                        "used", true,
                        "usedByProviderId", providerId
                )
                .addOnSuccessListener(aVoid -> {
                    inviteCodeEditText.setText("");
                    Toast.makeText(this, "Child linked!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating invite", Toast.LENGTH_SHORT).show());
    }

    private boolean isInviteExpired(Date createdAt) {
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        return System.currentTimeMillis() - createdAt.getTime() > sevenDaysMs;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (patientsListener != null) {
            patientsListener.remove();
        }
    }
}