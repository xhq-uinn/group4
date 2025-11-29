package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OnboardingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Button finishButton;

    private String childId; // for child login
    private boolean isChild = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        finishButton = findViewById(R.id.finishButton);

        // Check if launched from child login
        childId = getIntent().getStringExtra("childId");
        if (childId != null) {
            isChild = true;
        }

        finishButton.setOnClickListener(v -> completeOnboarding());
    }

    private void completeOnboarding() {

        if (isChild) {
            // child logiin
            db.collection("children")
                    .document(childId)
                    .update("hasCompletedOnboarding", true)
                    .addOnSuccessListener(aVoid -> {
                        Intent i = new Intent(this, ChildHomeActivity.class);
                        i.putExtra("childId", childId);
                        startActivity(i);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Child onboarding update failed", Toast.LENGTH_SHORT).show()
                    );

        }
        else {
            // parent/provider login
            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

            if (uid == null) {
                Toast.makeText(this, "Error: user not logged in", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");

                            db.collection("users")
                                    .document(uid)
                                    .update("hasCompletedOnboarding", true)
                                    .addOnSuccessListener(aVoid -> {
                                        if ("parent".equalsIgnoreCase(role)) {
                                            startActivity(new Intent(this, ParentHomeActivity.class));
                                        } else if ("provider".equalsIgnoreCase(role)) {
                                            startActivity(new Intent(this, ProviderHomeActivity.class));
                                        } else {
                                            Toast.makeText(this, "Unknown role type", Toast.LENGTH_SHORT).show();
                                        }
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Onboarding update failed", Toast.LENGTH_SHORT).show()
                                    );
                        } else {
                            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
