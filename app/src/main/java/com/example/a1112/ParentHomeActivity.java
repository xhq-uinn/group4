package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

public class ParentHomeActivity extends AppCompatActivity {

    // UI
    private Button buttonAddChild;
    private Button buttonInviteProvider;
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

        // ====== Firebase ======
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();


        // ====== UI connect ======
        buttonAddChild = findViewById(R.id.btn_add_child);
        buttonInviteProvider = findViewById(R.id.btn_invite_provider);
        childrenRecyclerView = findViewById(R.id.recycler_children);
        buttonSignOut = findViewById(R.id.btn_signout);

        // ====== Recycler setup ======
        childrenRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        childAdapter = new ChildAdapter(childList);
        childrenRecyclerView.setAdapter(childAdapter);

        // ====== Load children ======
        loadChildren();

        // ====== Add child button ======
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

        // ====== Invite provider button ======
        buttonInviteProvider.setOnClickListener(v -> {
            createInviteCode();
        });



        buttonSignOut.setOnClickListener(v -> {
            auth.signOut();

            Intent intent = new Intent(ParentHomeActivity.this, MainActivity.class);
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

    // ---------------------
    // 🔹 LOAD CHILDREN LIST
    // ---------------------
    private void loadChildren() {
        childList.clear();
        String parentUid = auth.getCurrentUser().getUid();

        db.collection("parents")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(snap -> {
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

    // ---------------------
    // 🔹 PROVIDER INVITE CODE
    // ---------------------
    private void createInviteCode() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = user.getUid();
        String code = generateCode(8);

        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("parentId", parentId);
        inviteData.put("createdAt", FieldValue.serverTimestamp());
        inviteData.put("used", false);

        db.collection("invites")
                .document(code)
                .set(inviteData)
                .addOnSuccessListener(unused -> showInviteDialog(code))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // code generator
    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // dialog
    private void showInviteDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Invite Code")
                .setMessage("Share this code with your provider:\n\n" + code)
                .setPositiveButton("OK", null)
                .setNeutralButton("Share", (d, w) -> shareInviteCode(code))
                .show();
    }

    // system share
    private void shareInviteCode(String code) {
        String text = "Here is my SmartAir invite code: " + code;
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(Intent.createChooser(sendIntent, "Share invite code"));
    }
}





