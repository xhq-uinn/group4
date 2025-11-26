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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import android.content.SharedPreferences;
import com.google.firebase.Timestamp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.a1112.Child;

public class ParentHomeActivity extends AppCompatActivity {

    // UI
    private Button buttonAddChild;
    private Button buttonInviteProvider;
    private RecyclerView childrenRecyclerView;
    private Button buttonSignOut;


    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration alertsListener;

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
        buttonInviteProvider = findViewById(R.id.btn_invite_provider);
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

        // Add child button
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

        // Invite provider button
        buttonInviteProvider.setOnClickListener(v -> {
            createInviteCode();
        });


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
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            // No user logged in, clear list
            childList.clear();
            childAdapter.notifyDataSetChanged();
            return;
        }

        String parentUid = user.getUid();

        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Validate parent account
                    if (!documentSnapshot.exists() || !"Parent".equals(documentSnapshot.getString("role"))) {
                        childList.clear();
                        childAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Safely read linkedChildren
                    List<String> linkedChildren;
                    Object obj = documentSnapshot.get("linkedChildren");
                    if (obj instanceof List<?>) {
                        linkedChildren = new ArrayList<>();
                        for (Object o : (List<?>) obj) {
                            if (o instanceof String) {
                                linkedChildren.add((String) o);
                            }
                        }
                    } else {
                        linkedChildren = new ArrayList<>();
                    }

                    // Parent always has at least one child, so no empty check
                    childList.clear();

                    // Fetch each child
                    for (String childId : linkedChildren) {
                        db.collection("children")
                                .document(childId)
                                .get()
                                .addOnSuccessListener(childDoc -> {
                                    if (childDoc.exists()) {
                                        // Handle age as int
                                        int age = 0;
                                        Object ageObj = childDoc.get("age");
                                        if (ageObj instanceof Long) {
                                            age = ((Long) ageObj).intValue();
                                        } else if (ageObj instanceof Double) {
                                            age = ((Double) ageObj).intValue();
                                        }

                                        // Handle other fields
                                        String name = childDoc.getString("name");
                                        String note = childDoc.getString("note");
                                        String username = childDoc.getString("username");
                                        String parentId = childDoc.getString("parentId");
                                        Boolean hasCompletedOnboarding = childDoc.getBoolean("hasCompletedOnboarding");
                                        if (hasCompletedOnboarding == null) hasCompletedOnboarding = false;

                                        List<String> sharedProviders = (List<String>) childDoc.get("sharedProviders");
                                        if (sharedProviders == null) sharedProviders = new ArrayList<>();

                                        // Create Child object
                                        Child child = new Child(
                                                childDoc.getId(),
                                                name,
                                                age,
                                                note,
                                                username,
                                                parentId,
                                                hasCompletedOnboarding,
                                                sharedProviders
                                        );

                                        childList.add(child);
                                        childAdapter.notifyDataSetChanged();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed to load child: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load linked children: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void showEditChildDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Child");

        // Create a vertical layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        final EditText etName = new EditText(this);
        etName.setHint("Name");
        etName.setText(child.getName()); // Set current name
        layout.addView(etName);

        // Age input
        final EditText etAge = new EditText(this);
        etAge.setHint("Age");
        etAge.setInputType(InputType.TYPE_CLASS_NUMBER); // Only allow numbers
        etAge.setText(String.valueOf(child.getAge())); // Convert int to String safely
        layout.addView(etAge);

        // PB input （add here）
        final EditText etPb = new EditText(this);
        etPb.setHint("PB (optional)");
        etPb.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPb);

        // Red Action Plan
        final EditText etRedAction = new EditText(this);
        etRedAction.setHint("Red zone action plan (optional)");
        layout.addView(etRedAction);

        // 🟡 Yellow Action Plan
        final EditText etYellowAction = new EditText(this);
        etYellowAction.setHint("Yellow zone action plan (optional)");
        layout.addView(etYellowAction);

        // Green Action Plan
        final EditText etGreenAction = new EditText(this);
        etGreenAction.setHint("Green zone action plan (optional)");
        layout.addView(etGreenAction);

        // Note input
        final EditText etNote = new EditText(this);
        etNote.setHint("Note");
        etNote.setText(child.getNote() != null ? child.getNote() : ""); // Avoid null
        layout.addView(etNote);

        // Set the layout in the dialog
        builder.setView(layout);

        // Save button
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newAgeStr = etAge.getText().toString().trim();
            String newPbStr  = etPb.getText().toString().trim();
            String newNote = etNote.getText().toString().trim();

            String redActionStr    = etRedAction.getText().toString().trim();
            String yellowActionStr = etYellowAction.getText().toString().trim();
            String greenActionStr  = etGreenAction.getText().toString().trim();

            // Validate required fields
            if (!newName.isEmpty() && !newAgeStr.isEmpty()) {
                updateChild(child.getId(), newName, newAgeStr, newNote, newPbStr, redActionStr, yellowActionStr, greenActionStr);//changed here
            } else {
                Toast.makeText(this, "Name and Age are required", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    // Edit Child Method
    private void updateChild(String childId, String name, String ageStr, String note, String pbStr, String redActionStr,
                             String yellowActionStr,
                             String greenActionStr) {//changed here
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Age must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare updated data
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("age", age);
        data.put("note", note);

        if (!pbStr.isEmpty()) {
            try {
                int pb = Integer.parseInt(pbStr);
                if (pb > 0) {
                    data.put("pb", pb);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "PB must be a number", Toast.LENGTH_SHORT).show();
            }
        }

        if (!redActionStr.isEmpty()) {
            data.put("redActionPlan", redActionStr);
        }

        if (!yellowActionStr.isEmpty()) {
            data.put("yellowActionPlan", yellowActionStr);
        }

        if (!greenActionStr.isEmpty()) {
            data.put("greenActionPlan", greenActionStr);
        }

        db.collection("children")
                .document(childId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Child updated", Toast.LENGTH_SHORT).show();
                    loadChildren();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Delete Child Method
    private void deleteChild(Child child) {
        String parentUid = auth.getCurrentUser().getUid();

        // Delete the child document from Firestore
        db.collection("children")
                .document(child.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove the child ID from the parent's linkedChildren array
                    db.collection("users")
                            .document(parentUid)
                            .update("linkedChildren", FieldValue.arrayRemove(child.getId()))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Child deleted", Toast.LENGTH_SHORT).show();

                                // Remove the child from the local list to immediately update the UI
                                childList.remove(child);
                                childAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to update parent: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete child: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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




    // Provider invite code
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

    //for FCM
    private void startAlertListener() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        String parentUid = user.getUid();

        final SharedPreferences prefs = getSharedPreferences("parent_prefs", MODE_PRIVATE);
        final String keyLastSeen = "last_alert_seen_" + parentUid;
        final long lastSeenFinal = prefs.getLong(keyLastSeen, 0L);

        if (alertsListener != null) {
            alertsListener.remove();
            alertsListener = null;
        }

        alertsListener = db.collection("alerts")
                .whereEqualTo("parentId", parentUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snap == null) return;

                    long newLastSeen = lastSeenFinal;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {

                            Timestamp ts = dc.getDocument().getTimestamp("timestamp");
                            long tsMillis = (ts != null) ? ts.toDate().getTime() : System.currentTimeMillis();

                            if (tsMillis <= lastSeenFinal) {
                                continue;
                            }

                            String type = dc.getDocument().getString("type");
                            String details = dc.getDocument().getString("details");
                            String childId = dc.getDocument().getString("childId");

                            showAlertFromChild(childId, type, details);

                            if (tsMillis > newLastSeen) {
                                newLastSeen = tsMillis;
                            }
                        }
                    }
                    if (newLastSeen > lastSeenFinal) {
                        prefs.edit().putLong(keyLastSeen, newLastSeen).apply();
                    }
                });
    }

    private void showAlertFromChild(String childId, String type, String details) {
        final String safeType;
        final String safeDetails;
        final String safeChildId;

        if (type == null) {
            safeType = "UNKNOWN";
        } else {
            safeType = type;
        }

        if (details == null || details.isEmpty()) {
            safeDetails = "Your child has a new asthma alert (" + safeType + ").";
        } else {
            safeDetails = details;
        }

        if (childId == null || childId.isEmpty()) {
            safeChildId = "unknown";
        } else {
            safeChildId = childId;
        }

        db.collection("children")
                .document(safeChildId)
                .get()
                .addOnSuccessListener(doc -> {
                    String childName = safeChildId;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            childName = name;
                        }
                    }
                    String message = "Child: " + childName + "\n"
                            + "Type: " + safeType + "\n\n"
                            + safeDetails;

                    new AlertDialog.Builder(this)
                            .setTitle("Alert from your child")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    String message = "Child: " + safeChildId + "\n"
                            + "Type: " + safeType + "\n\n"
                            + safeDetails;

                    new AlertDialog.Builder(this)
                            .setTitle("Alert from your child")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    //for fcm and always be the last function
    @Override
    protected void onStart() {
        super.onStart();
        startAlertListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alertsListener != null) {
            alertsListener.remove();
            alertsListener = null;
        }
    }
}





