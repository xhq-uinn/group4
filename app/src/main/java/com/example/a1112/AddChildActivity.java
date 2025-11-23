package com.example.a1112;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddChildActivity extends AppCompatActivity {

    private EditText nameEdit, ageEdit, noteEdit, usernameEdit, passwordEdit;

    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        // button binding
        nameEdit = findViewById(R.id.editChildName);
        ageEdit = findViewById(R.id.editChildAge);
        noteEdit = findViewById(R.id.editChildNote);
        usernameEdit = findViewById(R.id.editChildUsername);
        passwordEdit = findViewById(R.id.editChildPassword);
        saveButton = findViewById(R.id.saveChildButton);

        // initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChildInfo();
            }
        });
    }

    private void saveChildInfo() {
        String name = nameEdit.getText().toString().trim();
        String ageStr = ageEdit.getText().toString().trim();
        String note = noteEdit.getText().toString().trim();
        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        // Check required fields
        if (name.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Name and Age can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate age
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Age must be a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        // username & password required as a pair
        boolean hasUsername = !username.isEmpty();
        boolean hasPassword = !password.isEmpty();

        if (hasUsername ^ hasPassword) {
            Toast.makeText(this, "Username and Password must both be filled or both empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // If username present, check uniqueness
        if (hasUsername) {
            db.collection("children")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            saveChildToFirestore(name, age, note, username, password);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error checking username: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            // username empty → save with null username/password
            saveChildToFirestore(name, age, note, null, null);
        }
    }

    private void saveChildToFirestore(String name, int age, String note, String username, String password) {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = auth.getCurrentUser().getUid();

        // Verify parent role
        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !"Parent".equals(doc.getString("role"))) {
                        Toast.makeText(this, "Only parents can add children", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Prepare child data
                    Map<String, Object> childData = new HashMap<>();
                    childData.put("name", name);
                    childData.put("age", age);
                    childData.put("note", note);
                    childData.put("parentId", parentUid);
                    childData.put("hasCompletedOnboarding", false);
                    childData.put("username", username);  // can be null
                    childData.put("password", password);  // can be null
                    childData.put("sharedProviders", new ArrayList<>()); // keep consistency

                    // Save child
                    db.collection("children")
                            .add(childData)
                            .addOnSuccessListener(ref -> {

                                String childId = ref.getId();

                                // Update parent's linkedChildren
                                db.collection("users")
                                        .document(parentUid)
                                        .update("linkedChildren", FieldValue.arrayUnion(childId))
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Child added successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to update parent: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to save child: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });
    }



//    private void saveChildInfo() {
//        String name = nameEdit.getText().toString().trim();
//        String ageStr = ageEdit.getText().toString().trim();
//        String note = noteEdit.getText().toString().trim();
//
//        // Check for empty values
//        if (name.isEmpty() || ageStr.isEmpty()) {
//            Toast.makeText(this, "Name and Age can't be empty", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Convert age to integer
//        int age;
//        try {
//            age = Integer.parseInt(ageStr);
//        } catch (NumberFormatException e) {
//            Toast.makeText(this, "Age must be a valid number", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check if user is logged in
//        if (auth.getCurrentUser() == null) {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String parentUid = auth.getCurrentUser().getUid();
//
//        // Verify that current user is a Parent
//        db.collection("users")
//                .document(parentUid)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (!documentSnapshot.exists() || !"Parent".equals(documentSnapshot.getString("role"))) {
//                        Toast.makeText(this, "Only parents can add children", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // Prepare child data map
//                    Map<String, Object> childData = new HashMap<>();
//                    childData.put("name", name);
//                    childData.put("age", age); // stored as int
//                    childData.put("note", note);
//                    childData.put("parentId", parentUid);
//                    childData.put("hasCompletedOnboarding", false);
//
//                    // Save child to "children" collection
//                    db.collection("children")
//                            .add(childData)
//                            .addOnSuccessListener(documentReference -> {
//                                String childId = documentReference.getId(); // get new child ID
//
//                                // Update parent's linkedChildren array
//                                db.collection("users")
//                                        .document(parentUid)
//                                        .update("linkedChildren", com.google.firebase.firestore.FieldValue.arrayUnion(childId))
//                                        .addOnSuccessListener(aVoid -> {
//                                            Toast.makeText(AddChildActivity.this, "Child information has been saved", Toast.LENGTH_SHORT).show();
//                                            finish(); // return to previous page
//                                        })
//                                        .addOnFailureListener(e -> {
//                                            Toast.makeText(AddChildActivity.this, "Failed to update parent: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                        });
//                            })
//                            .addOnFailureListener(e -> {
//                                Toast.makeText(AddChildActivity.this, "Failed to save child: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                            });
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }


}
