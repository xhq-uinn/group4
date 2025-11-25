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

    private String childId; // child login 用
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
            // CASE 1 — CHILD LOGIN
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

        } else {
            // CASE 2 — PARENT / PROVIDER LOGIN
            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

            if (uid == null) {
                Toast.makeText(this, "Error: user not logged in", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            db.collection("users")
                    .document(uid)
                    .update("hasCompletedOnboarding", true)
                    .addOnSuccessListener(aVoid -> {
                        startActivity(new Intent(this, ParentHomeActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Onboarding update failed", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
