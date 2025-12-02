package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailField, adultPasswordField;
    private EditText childUsernameField, childPasswordField;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Adult login fields
        emailField = findViewById(R.id.emailField);
        adultPasswordField = findViewById(R.id.adultPasswordField);

        // Child login fields
        childUsernameField = findViewById(R.id.childUsernameField);
        childPasswordField = findViewById(R.id.childPasswordField);

        loginButton = findViewById(R.id.loginButton);


        loginButton.setOnClickListener(v -> handleLogin());

        findViewById(R.id.signupLink).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private void handleLogin() {

        String email = emailField.getText().toString().trim();
        String adultPassword = adultPasswordField.getText().toString().trim();

        String childUsername = childUsernameField.getText().toString().trim();
        String childPassword = childPasswordField.getText().toString().trim();

        // parent/provider's email&password both filled
        if (!email.isEmpty() && !adultPassword.isEmpty()) {

            // any of child username or password must be empty
            if (!childUsername.isEmpty() || !childPassword.isEmpty()) {
                Toast.makeText(this, "child login or parent login, not both", Toast.LENGTH_SHORT).show();
                return;
            }

            loginAdult(email, adultPassword);
            return;
        }

        //child login
        if (!childUsername.isEmpty() && !childPassword.isEmpty()) {

            if (!email.isEmpty() || !adultPassword.isEmpty()) {
                Toast.makeText(this, "child login or parent login, not both", Toast.LENGTH_SHORT).show();
                return;
            }

            loginChild(childUsername, childPassword);
            return;
        }

        Toast.makeText(this, "Please fill in login info", Toast.LENGTH_SHORT).show();
    }

    // Firebase auth login
    private void loginAdult(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {
                    String uid = auth.getCurrentUser().getUid();

                    db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Boolean done = doc.getBoolean("hasCompletedOnboarding");
                                if (done == null) done = false;

                                // check onboarding
                                if (!done) {
                                    Intent i = new Intent(this, OnboardingActivity.class);
                                    startActivity(i);
                                    finish();
                                    return;
                                }

                                // go to correct home
                                String role = doc.getString("role");
                                if (role == null) {
                                    Toast.makeText(this, "Role missing", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Intent intent;
                                switch (role) {
                                    case "Parent":
                                        intent = new Intent(this, ParentHomeActivity.class);
                                        break;
                                    case "Provider":
                                        intent = new Intent(this, ProviderHomeActivity.class);
                                        break;
                                    default:
                                        Toast.makeText(this, "Invalid role", Toast.LENGTH_SHORT).show();
                                        return;
                                }

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error loading role", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login fail", Toast.LENGTH_SHORT).show());
    }

    // Firestore child login
    private void loginChild(String username, String password) {
        db.collection("children")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Toast.makeText(this, "Wrong child username or password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //get child id
                    String childId = snap.getDocuments().get(0).getId();

                    //check onboarding
                    Boolean done = snap.getDocuments().get(0).getBoolean("hasCompletedOnboarding");
                    if (done == null || !done) {
                        Intent intent = new Intent(this, OnboardingActivity.class);
                        intent.putExtra("childId", childId);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    //go to ChildHomeActivity
                    Intent intent = new Intent(this, ChildHomeActivity.class);
                    intent.putExtra("childId", childId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
                );
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true); //disable back button when signed out
    }
}
//    private void loginChild(String username, String password) {
//        db.collection("children")
//                .whereEqualTo("username", username)
//                .whereEqualTo("password", password)
//                .get()
//                .addOnSuccessListener(snap -> {
//                    if (snap.isEmpty()) {
//                        String childId = snap.getDocuments().get(0).getId();
//
//                        db.collection("users").document(childId)
//                                .get()
//                                .addOnSuccessListener(doc -> {
//                                    Boolean done = doc.getBoolean("hasCompletedOnboarding");
//
//                                    if (done == null || !done) {
//                                        startActivity(new Intent(this, OnboardingActivity.class));
//                                    } else {
//                                        startActivity(new Intent(this, ChildHomeActivity.class));
//                                    }
//                                });
//                        Toast.makeText(this, "Wrong child username or password", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(this, "Child Login Success", Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(this, ChildHomeActivity.class));
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
//    }
//}