package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText emailField, passwordField;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);

        // Login button
        loginButton.setOnClickListener(v -> loginUser());

        // Sign up link
        findViewById(R.id.signupLink).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        // Forgot password link
        findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String uid = user.getUid();


        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if onboarding is completed
                    Boolean hasCompleted = doc.getBoolean("hasCompletedOnboarding");
                    if (hasCompleted == null || !hasCompleted) {
                        // not completed: OnboardingActivity
                        startActivity(new Intent(this, OnboardingActivity.class));
                        finish();
                        return;
                    }

                    // completed: go to role-home
                    String role = doc.getString("role");
                    if (role == null) {
                        Toast.makeText(this, "No role found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent;
                    switch (role) {
                        case "Parent":
                            intent = new Intent(this, ParentHomeActivity.class);
                            break;
                        case "Child":
                            intent = new Intent(this, ChildMainActivity.class);
                            break;
                        case "Provider":
                            intent = new Intent(this, ProviderMainActivity.class);
                            break;
                        default:
                            Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                            return;
                    }

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show());

    });
    }
}