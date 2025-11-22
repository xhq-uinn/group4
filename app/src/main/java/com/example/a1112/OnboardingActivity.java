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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        finishButton = findViewById(R.id.finishButton);

        // click 'Finish Setup'，update fields in database & enter homepage
        finishButton.setOnClickListener(v -> completeOnboarding());
    }

    private void completeOnboarding() {
        String uid = auth.getCurrentUser().getUid();

        // in Firestore mark onboarding complete
        db.collection("users").document(uid)
                .update("hasCompletedOnboarding", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Setup complete", Toast.LENGTH_SHORT).show();

                    // go to home according to roles
                    goToRoleHome(uid);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update onboarding: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    private void goToRoleHome(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                        // back to sign in
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    String role = doc.getString("role");
                    if (role == null) {
                        Toast.makeText(this, "No role found", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    Intent intent;
                    switch (role) {
                        case "Parent":
                            intent = new Intent(this, ParentHomeActivity.class);
                            break;
                        case "Child":
                            intent = new Intent(this, ChildHomeActivity.class);
                            break;
                        case "Provider":
                            intent = new Intent(this, ProviderHomeActivity.class);
                            break;
                        default:
                            Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();

                            intent = new Intent(this, LoginActivity.class);
                            break;
                    }

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load role, going back to login", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }
}