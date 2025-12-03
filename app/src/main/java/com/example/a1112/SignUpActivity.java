package com.example.a1112;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailField, passwordField;
    private EditText childNameField, childAgeField;
    private EditText childUsernameField, childPasswordField;   //child username & password

    private RadioGroup roleGroup;
    private LinearLayout childFields;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        childNameField = findViewById(R.id.childNameField);
        childAgeField = findViewById(R.id.childAgeField);

        childUsernameField = findViewById(R.id.childUsernameField);
        childPasswordField = findViewById(R.id.childPasswordField);

        roleGroup = findViewById(R.id.roleGroup);
        childFields = findViewById(R.id.childFields);

        registerButton = findViewById(R.id.registerButton);

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.roleParent) {
                childFields.setVisibility(View.VISIBLE);
            } else {
                childFields.setVisibility(View.GONE);
            }
        });

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String email = emailField.getText().toString().trim().toLowerCase();
        String password = passwordField.getText().toString().trim();

        String childName = childNameField.getText().toString().trim();
        String childAge = childAgeField.getText().toString().trim();

        String childUsername = childUsernameField.getText().toString().trim();
        String childPassword = childPasswordField.getText().toString().trim();

        int roleId = roleGroup.getCheckedRadioButtonId();
        if (roleId == -1) {
            Toast.makeText(this, "Select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton) findViewById(roleId)).getText().toString();

        // If Parent / Child → child info is needed
        if ((role.equals("Parent") || role.equals("Child")) &&
                (childName.isEmpty() || childAge.isEmpty())) {
            Toast.makeText(this, "Child name and age required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle username/password rules only for Parent signup
        boolean bothEmpty = childUsername.isEmpty() && childPassword.isEmpty();
        boolean bothFilled = !childUsername.isEmpty() && !childPassword.isEmpty();

        if (role.equals("Parent")) {
            if (!bothEmpty && !bothFilled) {
                Toast.makeText(this,
                        "Child username and password must BOTH be empty or BOTH be filled",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Step 1: Create FirebaseAuth user
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {

                    String uid = auth.getCurrentUser().getUid();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("role", role);
                    userData.put("hasCompletedOnboarding", false);
                    userData.put("linkedChildren", new ArrayList<>());

                    db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener(ignored -> {

                                switch (role) {
                                    case "Provider":
                                        goLogin();
                                        break;

                                    case "Child":
                                        createSelfChild(uid, childName, childAge, childUsername, childPassword);
                                        break;

                                    case "Parent":
                                        if (bothFilled) {
                                            checkUsernameAndCreateChild(uid, childName, childAge,
                                                    childUsername, childPassword);
                                        } else {
                                            // No child login, but still create child
                                            createChildWithoutLogin(uid, childName, childAge);
                                        }
                                        break;
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show());
    }

    private void checkUsernameAndCreateChild(
            String parentUid, String name, String age,
            String username, String password) {

        db.collection("children")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        Toast.makeText(this,
                                "Username already taken",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createChildWithLogin(parentUid, name, age, username, password);
                });
    }

    private void createChildWithLogin(
            String parentUid, String name, String age,
            String username, String password) {

        String childId = UUID.randomUUID().toString();

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", parentUid);
        child.put("username", username);
        child.put("password", password);
        child.put("hasCompletedOnboarding", false);

        db.collection("children").document(childId)
                .set(child)
                .addOnSuccessListener(ignored -> {
                    linkChildToParent(parentUid, childId);
                });
    }

    private void createChildWithoutLogin(String parentUid, String name, String age) {

        String childId = UUID.randomUUID().toString();

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", parentUid);
        child.put("username", null);
        child.put("password", null);
        child.put("hasCompletedOnboarding", false);

        db.collection("children").document(childId)
                .set(child)
                .addOnSuccessListener(ignored -> {
                    linkChildToParent(parentUid, childId);
                });
    }

    private void createSelfChild(String uid, String name, String age,
                                 String username, String password) {

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", null);
        child.put("username", username);
        child.put("password", password);
        child.put("hasCompletedOnboarding", false);

        db.collection("children").document(uid)  // Self-signup uses uid
                .set(child)
                .addOnSuccessListener(ignored -> goLogin());
    }

    private void linkChildToParent(String parentUid, String childId) {
        db.collection("users").document(parentUid)
                .update("linkedChildren", com.google.firebase.firestore.FieldValue.arrayUnion(childId))
                .addOnSuccessListener(ignored -> goLogin());
    }

    private void goLogin() {
        Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivityView.class));
        finish();
    }
}