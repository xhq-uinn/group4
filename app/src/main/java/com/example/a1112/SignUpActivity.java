package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
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

        // 显示/隐藏子女输入框
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
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton) findViewById(selectedRoleId)).getText().toString();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Sign up failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String uid = user.getUid();

                    // 添加到 users
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("email", email);
                    userInfo.put("role", role);
                    userInfo.put("hasCompletedOnboarding", false);

                    db.collection("users").document(uid)
                            .set(userInfo)
                            .addOnSuccessListener(aVoid -> {
                                if (role.equals("Parent") || role.equals("Child")) {
                                    addChild(uid, role, childName, childAge);
                                } else {
                                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                }
                            });
                });
    }

    private void addChild(String uid, String role, String name, String age) {
        String childId = UUID.randomUUID().toString();

        Map<String, Object> child = new HashMap<>();
        child.put("name", name);
        child.put("age", age);
        child.put("parentId", role.equals("Parent") ? uid : null);
        child.put("sharedProviders", new ArrayList<>());

        db.collection("children").document(childId)
                .set(child)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save child.", Toast.LENGTH_SHORT).show());
    }
}