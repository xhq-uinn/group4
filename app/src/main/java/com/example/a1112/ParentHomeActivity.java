package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.a1112.Child;

public class ParentHomeActivity extends AppCompatActivity {

    // UI
    private Button buttonAddChild;
    private Button buttonLinkChild;
//    private Button buttonInviteProvider;
    private RecyclerView childrenRecyclerView;
    private Button buttonSignOut;


    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // children list
    private List<Child> childList = new ArrayList<>();
    private ChildAdapter childAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();


        // UI connect
        buttonAddChild = findViewById(R.id.btn_add_child);
//        buttonInviteProvider = findViewById(R.id.btn_invite_provider);
        buttonLinkChild = findViewById(R.id.btn_link_child);
        childrenRecyclerView = findViewById(R.id.recycler_children);
        buttonSignOut = findViewById(R.id.btn_signout);

        // Recycler setup
        childrenRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList, new ChildAdapter.OnChildActionListener() {
            @Override
            public void onEditChild(Child child) {
                showEditChildDialog(child);
            }

            @Override
            public void onDeleteChild(Child child) {
                showDeleteChildDialog(child);
            }

            @Override
            public void onShareSettings(Child child) {
                Intent intent = new Intent(ParentHomeActivity.this, ChildShareSettingsActivity.class);
                intent.putExtra("childId", child.getId());
                intent.putExtra("childName", child.getName());
                startActivity(intent);
            }
        });
        childrenRecyclerView.setAdapter(childAdapter);

        // Load children
//        loadChildren();

        // Add child button
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

//        // Invite provider button
//        buttonInviteProvider.setOnClickListener(v -> {
//            createInviteCode();
//        });

        //Link child button
        buttonLinkChild.setOnClickListener(v -> showLinkChildDialog());

        //sign out
        buttonSignOut.setOnClickListener(v -> {
            auth.signOut();

            Intent intent = new Intent(ParentHomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            finish();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren(); // refresh on return
    }

    //Load children list
    private void loadChildren() {
        //check user
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            childList.clear();
            childAdapter.notifyDataSetChanged();
            return;
        }

        String parentUid = user.getUid();

        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(snap -> {
                    childList.clear();   //clear first
                    for (QueryDocumentSnapshot doc : snap) {
                        Child child = new Child(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("dob"),
                                doc.getString("note")
                        );
                        childList.add(child);
                    }
                    childAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load children", Toast.LENGTH_SHORT).show()
                );
    }



    //delete child method
    private void showEditChildDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Child");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Name");
        etName.setText(child.getName());
        layout.addView(etName);

        final EditText etDOB = new EditText(this);
        etDOB.setHint("DOB (YYYY-MM-DD)");
        etDOB.setText(child.getDob());
        layout.addView(etDOB);

        final EditText etNote = new EditText(this);
        etNote.setHint("Note");
        etNote.setText(child.getNote());
        layout.addView(etNote);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newDob = etDOB.getText().toString().trim();
            String newNote = etNote.getText().toString().trim();

            if (!newName.isEmpty() && !newDob.isEmpty()) {
                updateChild(child.getId(), newName, newDob, newNote);
            } else {
                Toast.makeText(this, "Name and DOB required", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    // ---------------------
    // 🔹 EDIT CHILD METHODS
    // ---------------------
    private void updateChild(String childId, String name, String dob, String note) {
        String parentUid = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("dob", dob);
        data.put("note", note);

        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Child updated", Toast.LENGTH_SHORT).show();
                    loadChildren();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    private void showDeleteChildDialog(Child child) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Child")
                .setMessage("Are you sure you want to delete " + child.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteChild(child))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChild(Child child) {
        String parentUid = auth.getCurrentUser().getUid();
        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .document(child.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Child deleted", Toast.LENGTH_SHORT).show();
                    loadChildren();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    //link child methods
    private void showLinkChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Link Child");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Child Name");
        layout.addView(etName);

        final EditText etDOB = new EditText(this);
        etDOB.setHint("DOB (YYYY-MM-DD)");
        layout.addView(etDOB);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        builder.setView(layout);

        builder.setPositiveButton("Link", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String dob = etDOB.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (!name.isEmpty() && !dob.isEmpty() && !email.isEmpty()) {
                linkChildByNameDobEmail(name, dob, email);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void linkChildByNameDobEmail(String name, String dob, String email) {
        db.collection("children")
                .whereEqualTo("name", name)
                .whereEqualTo("dob", dob)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String childId = snapshot.getDocuments().get(0).getId();
                        addChildToParent(childId, name, dob, email);
                    } else {
                        Toast.makeText(this, "No matching child found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void addChildToParent(String childId, String name, String dob, String email) {
        String parentUid = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("dob", dob);
        data.put("email", email);

        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Child linked successfully!", Toast.LENGTH_SHORT).show();
                    loadChildren(); // 刷新列表
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }



    // Provider invite code
//    private void createInviteCode() {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String parentId = user.getUid();
//        String code = generateCode(8);
//
//        Map<String, Object> inviteData = new HashMap<>();
//        inviteData.put("parentId", parentId);
//        inviteData.put("createdAt", FieldValue.serverTimestamp());
//        inviteData.put("used", false);
//
//        db.collection("invites")
//                .document(code)
//                .set(inviteData)
//                .addOnSuccessListener(unused -> showInviteDialog(code))
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    // code generator
//    private String generateCode(int length) {
//        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
//        SecureRandom random = new SecureRandom();
//        StringBuilder sb = new StringBuilder(length);
//        for (int i = 0; i < length; i++) {
//            sb.append(chars.charAt(random.nextInt(chars.length())));
//        }
//        return sb.toString();
//    }
//
//    // dialog
//    private void showInviteDialog(String code) {
//        new AlertDialog.Builder(this)
//                .setTitle("Invite Code")
//                .setMessage("Share this code with your provider:\n\n" + code)
//                .setPositiveButton("OK", null)
//                .setNeutralButton("Share", (d, w) -> shareInviteCode(code))
//                .show();
//    }
//
//    // system share
//    private void shareInviteCode(String code) {
//        String text = "Here is my SmartAir invite code: " + code;
//        Intent sendIntent = new Intent(Intent.ACTION_SEND);
//        sendIntent.setType("text/plain");
//        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
//
//        startActivity(Intent.createChooser(sendIntent, "Share invite code"));
//    }
}





