package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log for debugging
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch; // Import WriteBatch for atomic writes

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar; // Import Calendar for date manipulation
import java.util.Date; // Import Date class
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit; // Import TimeUnit (though not directly used in the logic)

public class ProviderListActivity extends AppCompatActivity
        implements ProviderInviteAdapter.OnInviteActionListener {

    private static final String TAG = "ProviderListActivity";

    private RecyclerView recyclerView;
    private Button btnCreateInvite;
    private ProviderInviteAdapter adapter;
    private List<ProviderInvite> providerList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String childId;
    private String parentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_list);

        recyclerView = findViewById(R.id.recyclerViewProviders);
        btnCreateInvite = findViewById(R.id.btnCreateInvite);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        // check if the user is login
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentId = user.getUid();
        // get childid
        childId = getIntent().getStringExtra("childId");

        if (childId == null) {
            Toast.makeText(this, "Child ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // initialize RecyclerView components
        providerList = new ArrayList<>();
        adapter = new ProviderInviteAdapter(providerList, this, childId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // load existing provider invites from Firestore
        loadProviderInvites();

        // create a new invite code
        btnCreateInvite.setOnClickListener(v -> createInviteCode());
    }

    @Override
    public void onDeleteInvite(String inviteCode, String providerId, int position) {
        // Check if the invite has been accepted
        if (providerId != null && !providerId.isEmpty()) {
            Toast.makeText(ProviderListActivity.this, "Association already established. Revoke action is blocked.", Toast.LENGTH_LONG).show();
        } else {
            deleteInviteCode(inviteCode, position);
        }
    }
    private void deleteInviteCode(String code, int position) {
        WriteBatch batch = db.batch();

        // delete the sharingSettings document
        batch.delete(db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .document(code));

        // delete the invites metadata document
        batch.delete(db.collection("invites").document(code));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // On successful Firestore deletion, update the local list and UI
                    Toast.makeText(this, "Invite " + code + " revoked successfully.", Toast.LENGTH_SHORT).show();
                    providerList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to revoke invite batch: " + code, e);
                    Toast.makeText(this, "Failed to revoke invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProviderInvites() {
        providerList.clear();

        // query the 'sharingSettings' subcollection under the child's document
        db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String inviteCode = doc.getId(); // Document ID is the invite code
                        String providerId = doc.getString("providerId");

                        // Read all sharing fields
                        Map<String, Boolean> sharingFields = new HashMap<>();
                        // Detailed permissions, defaulting to false if missing
                        sharingFields.put("rescueLogs", doc.getBoolean("rescueLogs") != null ? doc.getBoolean("rescueLogs") : false);
                        sharingFields.put("controllerAdherence", doc.getBoolean("controllerAdherence") != null ? doc.getBoolean("controllerAdherence") : false);
                        sharingFields.put("symptoms", doc.getBoolean("symptoms") != null ? doc.getBoolean("symptoms") : false);
                        sharingFields.put("triggers", doc.getBoolean("triggers") != null ? doc.getBoolean("triggers") : false);
                        sharingFields.put("peakFlow", doc.getBoolean("peakFlow") != null ? doc.getBoolean("peakFlow") : false);
                        sharingFields.put("triageIncidents", doc.getBoolean("triageIncidents") != null ? doc.getBoolean("triageIncidents") : false);
                        sharingFields.put("summaryCharts", doc.getBoolean("summaryCharts") != null ? doc.getBoolean("summaryCharts") : false);

                        providerList.add(new ProviderInvite(inviteCode, providerId, sharingFields));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load invites: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void createInviteCode() {
        String code = generateCode(8);
        WriteBatch batch = db.batch();

        Map<String, Object> sharingSettingsData = new HashMap<>();
        sharingSettingsData.put("providerId", null);
        sharingSettingsData.put("parentId", parentId);


        // sharing fields, default to false
        sharingSettingsData.put("rescueLogs", false);
        sharingSettingsData.put("controllerAdherence", false);
        sharingSettingsData.put("symptoms", false);
        sharingSettingsData.put("triggers", false);
        sharingSettingsData.put("peakFlow", false);
        sharingSettingsData.put("triageIncidents", false);
        sharingSettingsData.put("summaryCharts", false);

        batch.set(db.collection("children")
                .document(childId)
                .collection("sharingSettings")
                .document(code), sharingSettingsData);

        // invites/{code} document
        Map<String, Object> inviteMetadata = new HashMap<>();
        inviteMetadata.put("childId", childId);
        inviteMetadata.put("parentId", parentId);

        // calculate creation time and expiry time
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date expiresAt = calendar.getTime();

        inviteMetadata.put("createdAt", now);
        inviteMetadata.put("expiresAt", expiresAt);

        // invite status defaults to unused
        inviteMetadata.put("used", false);
        inviteMetadata.put("usedByProviderId", null);

        batch.set(db.collection("invites").document(code), inviteMetadata);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Invite created: " + code, Toast.LENGTH_LONG).show();

                    loadProviderInvites();

                    // Navigate immediately to the settings page for configuration
                    Intent intent = new Intent(this, ChildShareSettingsActivity.class);
                    intent.putExtra("childId", childId);
                    intent.putExtra("inviteCode", code);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create invite batch", e);
                    Toast.makeText(this, "Failed to create invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}