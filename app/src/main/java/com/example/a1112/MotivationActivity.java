package com.example.a1112;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MotivationActivity extends AppCompatActivity {

    private String childId;
    private TextView tvStreaks, tvBadges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motivation);

        childId = getIntent().getStringExtra("childId");

        tvStreaks = findViewById(R.id.tvStreaks);
        tvBadges = findViewById(R.id.tvBadges);

//        //loading
//        tvStreaks.setText("Loading...");
//        tvBadges.setText("");


        new MotivationCalculator().updateAllMotivation(childId, () -> {
            loadMotivationUI();
        });
    }

    private void loadMotivationUI() {

        FirebaseFirestore.getInstance()
                .collection("children")
                .document(childId)
                .collection("motivation")
                .document("status")
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        tvStreaks.setText("No data yet");
                        return;
                    }

                    int c = doc.getLong("controllerStreak").intValue();
                    int t = doc.getLong("techniqueStreak").intValue();

                    tvStreaks.setText(
                            "Controller Streak: " + c + "\n" +
                                    "Technique Streak: " + t
                    );

                    List<String> badges = (List<String>) doc.get("badges");
                    if (badges == null || badges.isEmpty()) {
                        tvBadges.setText("No badges yet");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (String b : badges)
                            sb.append("• ").append(b).append("\n");
                        tvBadges.setText(sb.toString());
                    }
                });
    }
}