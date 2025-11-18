package com.example.a1112;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddChildActivity extends AppCompatActivity {

    private EditText nameEdit, dobEdit, noteEdit;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        // button binding
        nameEdit = findViewById(R.id.editChildName);
        dobEdit = findViewById(R.id.editChildDOB);
        noteEdit = findViewById(R.id.editChildNote);
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
        String dob = dobEdit.getText().toString().trim();
        String note = noteEdit.getText().toString().trim();

        if (name.isEmpty() || dob.isEmpty()) {
            Toast.makeText(this, "Name and DOB can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = auth.getCurrentUser().getUid();

        Map<String, Object> childData = new HashMap<>();
        childData.put("name", name);
        childData.put("dob", dob);
        childData.put("note", note);

        // save to Firestore
        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .add(childData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddChildActivity.this, "Child information has been saved", Toast.LENGTH_SHORT).show();
                    finish(); // return to previous page
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddChildActivity.this, "Failure to be saved: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
