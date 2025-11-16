package com.example.a1112;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OnboardingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        finishButton = findViewById(R.id.finishButton);

        // 点击“Finish Setup”后，更新 Firestore 字段并进入主页
        finishButton.setOnClickListener(v -> completeOnboarding());
    }

    private void completeOnboarding() {
        String uid = auth.getCurrentUser().getUid();

        // 在 Firestore 中标记 onboarding 已完成
        db.collection("users").document(uid)
                .update("hasCompletedOnboarding", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Setup complete!", Toast.LENGTH_SHORT).show();

                    // 跳转主界面（可统一跳 MainActivity，或根据角色跳不同界面）
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update onboarding: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}