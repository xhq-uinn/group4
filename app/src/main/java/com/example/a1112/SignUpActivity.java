package com.example.a1112;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailField, passwordField, childNameField, childAgeField;
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
        roleGroup = findViewById(R.id.roleGroup);
        childFields = findViewById(R.id.childFields);
        registerButton = findViewById(R.id.registerButton);

        // Only Parent & Child sees child name/age fields
        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.roleParent || checkedId == R.id.roleChild) {
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

        int selectedRoleId = roleGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "select a role needed", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton) findViewById(selectedRoleId)).getText().toString();

        if ((role.equals("Parent") || role.equals("Child")) &&
                (childName.isEmpty() || childAge.isEmpty())) {
            Toast.makeText(this, "child name and age needed", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Sign up fail", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String uid = user.getUid();

                    // Add base user data
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("email", email);
                    userInfo.put("role", role);
                    userInfo.put("hasCompletedOnboarding", false);
                    userInfo.put("linkedChildren", new ArrayList<>());

                    db.collection("users").document(uid)
                            .set(userInfo)
                            .addOnSuccessListener(aVoid -> {
                                switch (role) {
                                    case "Parent":
                                        addChildFromParent(uid, childName, childAge);
                                        break;

                                    case "Child":
                                        addChildSelfSignup(uid, childName, childAge);
                                        break;

                                    case "Provider":
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                        break;
                                }
                            });
                });
    }

    // Parent creates child
    private void addChildFromParent(String parentUid, String name, String age) {
        String childId = UUID.randomUUID().toString();

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", parentUid);
        child.put("username", null);
        child.put("password", null);
        child.put("additionalNote", null);
        child.put("sharedProviders", new ArrayList<>());

        //Add onboarding status
        child.put("hasCompletedOnboarding", false);

        db.collection("children").document(childId)
                .set(child)
                .addOnSuccessListener(aVoid -> {

                    db.collection("users").document(parentUid)
                            .update("linkedChildren", com.google.firebase.firestore.FieldValue.arrayUnion(childId))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Parent Sign up successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Child created but failed to link to parent", Toast.LENGTH_SHORT).show()
                            );

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save child.", Toast.LENGTH_SHORT).show());
    }

    // Child self signup (has own login)
    private void addChildSelfSignup(String uid, String name, String age) {

        String childId = uid;  // child’s own UID used as childId

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", null);
        child.put("username", emailField.getText().toString().trim());
        child.put("password", passwordField.getText().toString().trim());
        child.put("sharedProviders", new ArrayList<>());

        // Child must also complete onboarding
        child.put("hasCompletedOnboarding", false);

        db.collection("children").document(childId)
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Child Sign up successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }
}