package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText emailField;
    private Button resetButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailField = findViewById(R.id.emailField);
        resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 1: Find this email in users collection
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String role = snap.getDocuments().get(0).getString("role");

                        // Step 2: Only Parent or Provider can reset password
                        if (!"Parent".equals(role) && !"Provider".equals(role)) {
                            Toast.makeText(this, "Only Parent/Provider can reset password.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Step 3: This email belongs to a Firebase Auth user → send reset email
                        auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}