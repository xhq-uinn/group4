package com.example.a1112;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TechniqueHelperActivity extends AppCompatActivity {

    private static final String TAG = "TechniqueHelperActivity";

    // UI components
    private TextView tvStepTitle, tvStepDescription, tvCountdown;
    private PlayerView videoStep;
    private Button btnRestart;
    private Button btnFinish;
    private LinearLayout llTechniqueQuality;
    private Button btnHighQuality, btnNeedAssistance;

    private ExoPlayer player;
    private List<Step> steps;
    private int currentStepIndex = 0;
    private CountDownTimer countDownTimer;

    private String childId;
    private FirebaseFirestore db;
    private String sessionId; // track the current session in Firestore
    private boolean isLowQuality = false; // Tracks if the user failed any step
    private boolean isFinished = false; // Tracks if the session reached the end


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

        db = FirebaseFirestore.getInstance();
        initializeSession();

        // Initialize UI components
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepDescription = findViewById(R.id.tvStepDescription);
        tvCountdown = findViewById(R.id.tvCountdown);
        videoStep = findViewById(R.id.videoStep);

        btnRestart = findViewById(R.id.btnRestart);
        btnFinish = findViewById(R.id.btnFinish);
        llTechniqueQuality = findViewById(R.id.llTechniqueQuality);
        btnHighQuality = findViewById(R.id.btnHighQuality);
        btnNeedAssistance = findViewById(R.id.btnNeedAssistance);

        // UI setup
        btnRestart.setVisibility(View.GONE);
        btnFinish.setVisibility(View.GONE);
        llTechniqueQuality.setVisibility(View.VISIBLE);

        // initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        videoStep.setPlayer(player);

        // initialize steps
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

        loadStep(currentStepIndex);
        setupListeners();
    }

    private void initializeSession() {
        // Only generate a new ID and create the session on initial launch
        sessionId = UUID.randomUUID().toString();
        isLowQuality = false; // Reset quality for new session
        isFinished = false;
        updateSessionQuality("high-quality", true);
        Log.i(TAG, "New technique session started with ID: " + sessionId);
    }

    private void updateSessionQuality(String quality, boolean isInitialCreation) {
        Map<String, Object> sessionUpdate = new HashMap<>();
        sessionUpdate.put("quality", quality);

        // Use the flag isFinished to determine the 'completed' status
        sessionUpdate.put("completed", isFinished);

        if (isInitialCreation) {
            sessionUpdate.put("timestamp", FieldValue.serverTimestamp());
        }

        DocumentReference docRef = db.collection("children")
                .document(childId)
                .collection("technique_sessions")
                .document(sessionId);

        docRef.set(sessionUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Session quality updated to: " + quality + ", Completed: " + isFinished))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update session quality: " + e.getMessage()));
    }

    private void setupListeners() {

        btnHighQuality.setOnClickListener(v -> {
            Log.d(TAG, "Step " + (currentStepIndex + 1) + " marked as High Quality. Proceeding.");
            goToNextStep();
        });

        btnNeedAssistance.setOnClickListener(v -> {
            isLowQuality = true; // Set local flag for low quality

            updateSessionQuality("low-quality", false);

            Toast.makeText(this, "Marked Step " + (currentStepIndex + 1) + " as LOW Quality. Session is now Low Quality.", Toast.LENGTH_SHORT).show();
            goToNextStep();
        });

        btnRestart.setOnClickListener(v -> {
            // reset state for new attempt using the SAME session ID
            isLowQuality = false; // Reset quality flag
            isFinished = false;
            currentStepIndex = 4; // Step 5

            btnRestart.setVisibility(View.GONE);
            btnFinish.setVisibility(View.GONE);
            videoStep.setVisibility(View.VISIBLE);
            llTechniqueQuality.setVisibility(View.VISIBLE);

            // Update Firestore: Reset quality to high-quality (for the attempt) and mark completed=false
            updateSessionQuality("high-quality", false);

            loadStep(currentStepIndex);
        });

        // Finish button click - Explicitly marks session as COMPLETED
        btnFinish.setOnClickListener(v -> {
            isFinished = true; // Mark session as finished
            String finalQuality = isLowQuality ? "low-quality" : "high-quality";

            // write the final quality and set completed=true in Firestore
            updateSessionQuality(finalQuality, false);

            // final UI
            tvStepTitle.setText("Session Finished!");
            tvStepDescription.setText("Your technique session has been saved with quality: " + finalQuality);
            btnRestart.setVisibility(View.GONE);
            btnFinish.setVisibility(View.GONE);
            Toast.makeText(this, "Session Completed! Thank you.", Toast.LENGTH_LONG).show();

        });
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

            llTechniqueQuality.setVisibility(View.GONE); // hide High/Low quality buttons
            btnRestart.setVisibility(View.VISIBLE); // restart button
            btnFinish.setVisibility(View.VISIBLE); // finish button
            videoStep.setVisibility(View.GONE);

            String status = isLowQuality ? "needs practice" : "looks good";
            tvStepTitle.setText("Review Complete!");
            tvStepDescription.setText("Your technique " + status + ". Choose to Finish and Save, or Restart from Step 5.");
        }
    }



    private void loadStep(int index) {
        // Update button text for the final step
        if (index == steps.size() - 1) {
            btnHighQuality.setText("Yes, High Quality (Continue to Review)");
            btnNeedAssistance.setText("Need Assistance (Low Quality & Continue to Review)");
        } else {
            btnHighQuality.setText("Yes, High Quality (Next Step)");
            btnNeedAssistance.setText("Need Assistance (Low Quality & Next Step)");
        }

        Step step = steps.get(index);
        tvStepTitle.setText("Step " + (index + 1) + ": " + step.title);
        tvStepDescription.setText(step.description);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(step.videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.seekTo(0);
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

    @Override
    protected void onPause() {
        super.onPause();
        // if the session is incomplete (not finished), mark it as low quality upon exit.
        if (!isFinished) {
            updateSessionQuality("low-quality", false);
            Log.w(TAG, "Session incomplete. Quality set to low-quality on pause.");
        }
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (player != null) {
            player.release();
        }
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