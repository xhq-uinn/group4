package com.example.a1112;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TechniqueHelperActivity extends AppCompatActivity {

    private TextView tvStepTitle, tvStepDescription, tvCountdown;
    private PlayerView videoStep;
    private Button btnNextStep, btnRestart;
    private LinearLayout llTechniqueQuality;
    private Button btnHighQuality, btnNormalQuality, btnNeedAssistance;

    private ExoPlayer player;
    private List<Step> steps;
    private int currentStepIndex = 0;
    private CountDownTimer countDownTimer;

    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technique_helper);

        childId = getIntent().getStringExtra("childId");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Error: no child selected", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize UI components
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepDescription = findViewById(R.id.tvStepDescription);
        tvCountdown = findViewById(R.id.tvCountdown);
        videoStep = findViewById(R.id.videoStep);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnRestart = findViewById(R.id.btnRestart);
        llTechniqueQuality = findViewById(R.id.llTechniqueQuality);
        btnHighQuality = findViewById(R.id.btnHighQuality);
        btnNormalQuality = findViewById(R.id.btnNormalQuality);
        btnNeedAssistance = findViewById(R.id.btnNeedAssistance);

        btnRestart.setVisibility(View.GONE);
        llTechniqueQuality.setVisibility(View.GONE);

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        videoStep.setPlayer(player);

        // Initialize steps
        steps = new ArrayList<>();
        steps.add(new Step("Prepare Your Inhaler", "Remove the mouthpiece cap and shake before use.",
                "android.resource://" + getPackageName() + "/" + R.raw.video1, 0));
        steps.add(new Step("Prepare the Spacer", "Remove the cap and ensure the inside is clean.",
                "android.resource://" + getPackageName() + "/" + R.raw.video2, 0));
        steps.add(new Step("Prime the Inhaler", "If it is your first time using the inhaler, or if you haven’t used it in a while, prime it according to the instructions.",
                "android.resource://" + getPackageName() + "/" + R.raw.video3, 0));
        steps.add(new Step("Attach the Spacer", "Attach your spacer to the inhaler when ready.",
                "android.resource://" + getPackageName() + "/" + R.raw.video4, 0));
        steps.add(new Step("Position Yourself", "Stand or sit up straight before using your inhaler.",
                "android.resource://" + getPackageName() + "/" + R.raw.video5, 0));
        steps.add(new Step("Exhale Fully", "Breathe out completely to empty your lungs.",
                "android.resource://" + getPackageName() + "/" + R.raw.video6, 0));
        steps.add(new Step("Seal the Spacer", "Put the spacer’s mouthpiece in your mouth between your teeth and above your tongue, then close your lips around the mouthpiece to form a tight seal.",
                "android.resource://" + getPackageName() + "/" + R.raw.video7, 0));
        steps.add(new Step("Inhale the Medicine", "Press down firmly on your inhaler to release one puff into the spacer and breathe in as slowly and deeply as you can. (A whistling sound means you are breathing too quickly.)",
                "android.resource://" + getPackageName() + "/" + R.raw.video8, 0));
        steps.add(new Step("Hold Your Breath", "Hold your breath for approximately a count of ten.",
                "android.resource://" + getPackageName() + "/" + R.raw.video9, 10));
        steps.add(new Step("Exhale Slowly", "Breathe out slowly through your mouth.",
                "android.resource://" + getPackageName() + "/" + R.raw.video10, 0));
        steps.add(new Step("Second Puff if Needed", "If your medicine requires two puffs, follow your inhaler instructions to know how long to wait between doses.",
                "android.resource://" + getPackageName() + "/" + R.raw.video11, 60));

        // Load first step
        loadStep(currentStepIndex);

        // Next Step click
        btnNextStep.setOnClickListener(v -> goToNextStep());

        // Restart button click (jump to Step 5)
        btnRestart.setOnClickListener(v -> {
            currentStepIndex = 4; // Step 5 index
            btnRestart.setVisibility(View.GONE);
            btnNextStep.setEnabled(true);
            btnNextStep.setVisibility(View.VISIBLE);
            llTechniqueQuality.setVisibility(View.GONE);
            videoStep.setVisibility(View.VISIBLE);
            loadStep(currentStepIndex);
        });
    }

    private void loadStep(int index) {
        Step step = steps.get(index);
        tvStepTitle.setText("Step " + (index + 1) + ": " + step.title);
        tvStepDescription.setText(step.description);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(step.videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        tvCountdown.setVisibility(View.GONE);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    if (step.countdownSeconds > 0) {
                        startCountdown(step.countdownSeconds);
                    }
                    player.removeListener(this);
                }
            }
        });
    }

    private void startCountdown(int seconds) {
        tvCountdown.setVisibility(View.VISIBLE);
        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Hold/Wait: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Done!");
            }
        }.start();
    }

    private void goToNextStep() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            tvCountdown.setVisibility(View.GONE);
        }

        currentStepIndex++;
        if (currentStepIndex < steps.size()) {
            loadStep(currentStepIndex);
        } else {
            // Last step completed
            btnNextStep.setVisibility(View.GONE);
            btnRestart.setVisibility(View.VISIBLE);
            videoStep.setVisibility(View.GONE);
            tvStepTitle.setText("Technique Completed!");
            tvStepDescription.setText("Please select the session quality.");
            llTechniqueQuality.setVisibility(View.VISIBLE);

            btnHighQuality.setOnClickListener(v -> saveSessionQuality("High Quality"));
            btnNormalQuality.setOnClickListener(v -> saveSessionQuality("Normal"));
            btnNeedAssistance.setOnClickListener(v -> saveSessionQuality("Need Assistance"));
        }
    }

    private void saveSessionQuality(String quality) {
        llTechniqueQuality.setVisibility(View.GONE);
        btnRestart.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("quality", quality);
        sessionData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("children")
                .document(childId)
                .collection("technique_sessions")
                .add(sessionData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Session quality saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    private static class Step {
        String title;
        String description;
        String videoUrl;
        int countdownSeconds;

        Step(String title, String description, String videoUrl, int countdownSeconds) {
            this.title = title;
            this.description = description;
            this.videoUrl = videoUrl;
            this.countdownSeconds = countdownSeconds;
        }
    }
}
