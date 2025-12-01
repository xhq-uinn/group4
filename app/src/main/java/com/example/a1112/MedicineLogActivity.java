package com.example.a1112;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicineLogActivity extends AppCompatActivity {

    // UI components
    private TextView childNameText, logsHeaderText, noLogsText, unitTypeTitle;
    private Button buttonRescueLogs, buttonControllerLogs, buttonSubmitLog, buttonRateLastDose, buttonFlagLow, buttonHome, buttonAlertConfiguration, buttonScheduleConfiguration;

    private Spinner medicineSpinner, medicineTypeSpinner, unitTypeSpinner;
    private EditText customMedicineInput, doseCountInput;
    private RecyclerView logsRecyclerView;

    private FirebaseFirestore db;
    private String currentChildId;
    private String currentChildName;
    private String userType;
    private String currentViewType = "rescue"; //default it to rescue and user can change whenever

    private List<MedicineLog> logs = new ArrayList<>();
    private List<Medicine> medicines = new ArrayList<>();
    private MedicineLogAdapter adapter;

    private boolean fromInventory = false; // to distinguish custom medicine or selected from inventory

    private int maxRapidRescues = 3;    // default: 3 uses
    private int rapidRescuesTimeframe = 3;        // default: 3 hours

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_log);

        //get all the childs data from the intent that routed to medicine log page
        Intent intent = getIntent();
        currentChildId = intent.getStringExtra("CHILD_ID");
        currentChildName = intent.getStringExtra("CHILD_NAME");
        userType = intent.getStringExtra("USER_TYPE"); // "parent" or "child"

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupHiddenInputFields();
        setupClickListeners();
        loadRapidRescueSettings();
        loadMedicines();
        loadLogs(currentViewType);
    }

    private void initializeViews() {
        childNameText = findViewById(R.id.childNameText);
        logsHeaderText = findViewById(R.id.logsHeaderText);
        noLogsText = findViewById(R.id.noLogsText);
        buttonRescueLogs = findViewById(R.id.buttonRescueLogs);
        buttonControllerLogs = findViewById(R.id.buttonControllerLogs);
        buttonSubmitLog = findViewById(R.id.buttonSubmitLog);
        buttonRateLastDose = findViewById(R.id.buttonRateLastDose);
        buttonFlagLow = findViewById(R.id.buttonFlagLow);
        buttonAlertConfiguration = findViewById(R.id.buttonAlertConfiguration);
        buttonScheduleConfiguration = findViewById(R.id.buttonScheduleConfiguration);
        buttonHome = findViewById(R.id.buttonHome);
        medicineTypeSpinner = findViewById(R.id.medicineTypeSpinner);
        medicineSpinner = findViewById(R.id.medicineSpinner);
        unitTypeSpinner = findViewById(R.id.unitTypeSpinner);
        unitTypeTitle = findViewById(R.id.unitTypeTitle);
        customMedicineInput = findViewById(R.id.customMedicineInput);
        doseCountInput = findViewById(R.id.doseCountInput);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);

        if (currentChildName != null) {
            childNameText.setText(currentChildName + "'s Medicine Log");
        }


        // only show flag low and pre/post check for child users and
        if ("child".equals(userType)) {
            buttonFlagLow.setVisibility(View.VISIBLE);
            buttonRateLastDose.setVisibility(View.VISIBLE);
        }
        else
        {
            buttonAlertConfiguration.setVisibility(View.VISIBLE);
            buttonScheduleConfiguration.setVisibility(View.VISIBLE);
        }

        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineLogAdapter(logs);
        logsRecyclerView.setAdapter(adapter);
    }


    //when user selects other for medicine, make custom name input and unit type selector visible
    private void setupHiddenInputFields() {
        medicineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Other (type name)")) {
                    customMedicineInput.setVisibility(View.VISIBLE);
                    unitTypeSpinner.setVisibility(View.VISIBLE);
                    unitTypeTitle.setVisibility(View.VISIBLE);
                    fromInventory = false;
                } else {
                    customMedicineInput.setVisibility(View.GONE);
                    unitTypeSpinner.setVisibility(View.GONE);
                    unitTypeTitle.setVisibility(View.GONE);
                    fromInventory = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void setupClickListeners() {

        // displays recent rescue/controller logs based on button click
        buttonRescueLogs.setOnClickListener(v -> {
            currentViewType = "rescue";
            logsHeaderText.setText("RECENT RESCUE LOGS");
            loadLogs("rescue");
        });
        buttonControllerLogs.setOnClickListener(v -> {
            currentViewType = "controller";
            logsHeaderText.setText("RECENT CONTROLLER LOGS");
            loadLogs("controller");
        });

        buttonSubmitLog.setOnClickListener(v -> submitLog());

        buttonRateLastDose.setOnClickListener(v -> rateLastDose());

        buttonFlagLow.setOnClickListener(v -> showFlagLowDialog());

        buttonAlertConfiguration.setOnClickListener(v -> showRapidRescueSettings());

        buttonScheduleConfiguration.setOnClickListener(v -> {
            Intent intent = new Intent(MedicineLogActivity.this, ScheduleConfigurationActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("CHILD_NAME", currentChildName);
            intent.putExtra("USER_TYPE", userType);
            startActivity(intent);
        });

        buttonHome.setOnClickListener(v -> {
            Intent intent;

            if ("parent".equals(userType)) {
                // Go to Parent Home
                intent = new Intent(MedicineLogActivity.this, ParentHomeActivity.class);
            }
            else
            {
                intent = new Intent(MedicineLogActivity.this, ChildHomeActivity.class);
                intent.putExtra("childId", currentChildId);
                intent.putExtra("childName", currentChildName);
            }


            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        });
    }

    // Load medicines from inventory
    private void loadMedicines() {
        db.collection("medicines")
                .whereEqualTo("childId", currentChildId)
                .get()
                .addOnSuccessListener(docs -> {
                    //reinitialize the list
                    medicines.clear();
                    List<String> medicineNames = new ArrayList<>();

                    //always have other option
                    medicineNames.add("Other (type name)");

                    for (QueryDocumentSnapshot doc : docs) {
                        Medicine med = doc.toObject(Medicine.class);
                        med.setId(doc.getId());
                        medicines.add(med);
                        medicineNames.add(med.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, medicineNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    medicineSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading medicines", Toast.LENGTH_SHORT).show();
                });
    }

    // load last 25 logs just so the list isnt too long
    private void loadLogs(String type) {
        db.collection("medicineLogs")
                .whereEqualTo("childId", currentChildId)
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .addOnSuccessListener(docs -> {
                    logs.clear();
                    for (QueryDocumentSnapshot doc : docs) {
                        MedicineLog log = doc.toObject(MedicineLog.class);
                        log.setId(doc.getId());
                        logs.add(log);
                    }

                    updateLogsDisplay();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading logs", Toast.LENGTH_SHORT).show();
                });
    }

    // get all log details after a submission
    private void submitLog() {

        String selectedMedicine = medicineSpinner.getSelectedItem().toString();
        String selectedMedicineType = medicineTypeSpinner.getSelectedItem().toString();
        String unitType = null;
        String medicineName;
        String medicineId = null;



        if (selectedMedicine.equals("Other (type name)")) {
            medicineName = customMedicineInput.getText().toString().trim();
            if (medicineName.isEmpty()) {
                Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show();
                return;
            }

            unitType = unitTypeSpinner.getSelectedItem().toString();
        }
        else
        {
            medicineName = selectedMedicine;
            //loop through medicines to find the id of the selected medicine and its unit type
            for (Medicine med : medicines) {
                if (med.getName().equals(medicineName)) {
                    medicineId = med.getId();
                    unitType = med.getUnitType();
                    break;
                }
            }
        }

        //when a medicine is logged from inventory get its un
        if (fromInventory) {
            for (Medicine med : medicines) {
                if (med.getName().equals(medicineName)) {
                    String actualType = med.getType();

                    if (!actualType.equals(selectedMedicineType)) {
                        Toast.makeText(this,
                                "Cannot log " + actualType + " medicine as " + selectedMedicineType + ". Please select correct type.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    break;
                }
            }
        }


        // get dose count
        String doseStr = doseCountInput.getText().toString().trim();
        if (doseStr.isEmpty()) {
            Toast.makeText(this, "Please enter dose count", Toast.LENGTH_SHORT).show();
            return;
        }

        //make sure doseStr is a number and convert it to int
        int doseCount;

        try {
            doseCount = Integer.parseInt(doseStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid dose count", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doseCount <= 0) {
            Toast.makeText(this, "Please enter dose count greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }


        // save log to database
        saveLogData(medicineId, medicineName, selectedMedicineType, doseCount, unitType);
    }

    // Save log to database
    private void saveLogData(String medicineId, String medicineName, String type, int doseCount, String unitType) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("childId", currentChildId);
        logData.put("medicineId", medicineId);
        logData.put("medicineName", medicineName);
        logData.put("type", type);
        logData.put("unitType", unitType);
        logData.put("doseCount", doseCount);
        logData.put("timestamp", new Date());
        logData.put("loggedBy", userType);

        db.collection("medicineLogs")
                .add(logData)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Dose logged!", Toast.LENGTH_SHORT).show();
                    // reset the form so user can add new logs
                    doseCountInput.setText("");
                    customMedicineInput.setText("");
                    medicineTypeSpinner.setSelection(0);
                    medicineSpinner.setSelection(0);

                    //if child logged a controller dose, go to technique helper
                    if ("controller".equals(type) && "child".equals(userType)) {
                        Intent intent = new Intent(MedicineLogActivity.this, TechniqueHelperActivity.class);
                        intent.putExtra("childId", currentChildId);
                        intent.putExtra("childName", currentChildName);
                        startActivity(intent);
                    }

                    // reload the logs to show the updated logs if the user is viewing the same log type
                    if (type.equals(currentViewType)) {
                        loadLogs(currentViewType);
                    }

                    //whenever a rescue use is logged check if we pass threshold of
                    // x uses within x hours for an alert to parent
                    if ("rescue".equals(type)) {
                        checkForRapidRescuesAlert(currentChildId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error logging dose", Toast.LENGTH_SHORT).show();
                });
    }

    // rating of last dose (pre/post check)
    private void rateLastDose() {

        // get most recent log
        db.collection("medicineLogs")
                .whereEqualTo("childId", currentChildId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        Toast.makeText(this, "No doses to rate", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //convert the log to an object and add doc id as object field's id because our obejct stores id as field
                    MedicineLog recentLogObject = result.getDocuments().get(0).toObject(MedicineLog.class);
                    recentLogObject.setId(result.getDocuments().get(0).getId());

                    //show rating popup for that recent log
                    showRatingDialog(recentLogObject);
                });
    }

    // Show rating dialog
    private void showRatingDialog(MedicineLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate: " + log.getMedicineName());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        // spinners to rate breath before and after
        TextView beforeRating = new TextView(this);
        beforeRating.setText("Breath rating BEFORE medicine (1-5):");
        layout.addView(beforeRating);

        Spinner beforeRatingsSpinner = createSpinner(R.array.breath_ratings);
        layout.addView(beforeRatingsSpinner);

        TextView afterRating = new TextView(this);
        afterRating.setText("Breath rating AFTER medicine (1-5):");
        afterRating.setPadding(0, 20, 0, 0);
        layout.addView(afterRating);

        Spinner afterRatingSpinner = createSpinner(R.array.breath_ratings);
        layout.addView(afterRatingSpinner);

        // spinner to rate feeling after (better/same/worse)
        TextView afterFeeling = new TextView(this);
        afterFeeling.setText("How do you feel AFTER?");
        afterFeeling.setPadding(0, 20, 0, 0);
        layout.addView(afterFeeling);

        Spinner afterFeelingSpinner = createSpinner(R.array.feeling_choices);
        layout.addView(afterFeelingSpinner);

        builder.setView(layout);
        builder.setPositiveButton("Submit Rating", (dialog, which) -> {
            String feeling = afterFeelingSpinner.getSelectedItem().toString();
            saveRating(log.getId(), feeling); // make sure to store post feeling in database

        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // stores post dose feeling in database
    private void saveRating(String logId, String feeling) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("postFeeling", feeling); // Just one simple field!

        db.collection("medicineLogs").document(logId)
                .update(updates)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Rating saved!", Toast.LENGTH_SHORT).show();

                    // when post check feeling is worse send alert
                    if ("Worse".equals(feeling)) {
                        db.collection("medicineLogs").document(logId).get()
                                .addOnSuccessListener(doc -> {
                                    MedicineLog log = doc.toObject(MedicineLog.class);
                                    if (log != null) {
                                        worseAfterDoseAlert(log);
                                    }
                                });
                    }

                    loadLogs(currentViewType); //reload logs to show the rating for the medicine log item in the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving rating", Toast.LENGTH_SHORT).show();
                });
    }

    // display the dialog to flag medicine as low
    private void showFlagLowDialog() {
        if (medicines.isEmpty()) {
            Toast.makeText(this, "No medicines in inventory", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Flag Medicine as Low");

        //spinner for child to select medicine to flag as low

        Spinner medicineSpinner = new Spinner(this);
        List<String> medicineNames = new ArrayList<>();
        for (Medicine med : medicines) {
            medicineNames.add(med.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, medicineNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medicineSpinner.setAdapter(adapter);

        builder.setView(medicineSpinner);

        //if confirmed call function to flag that medicine as low in inventory's database
        builder.setPositiveButton("Flag as Low", (dialog, which) -> {
            String selectedName = medicineSpinner.getSelectedItem().toString();

            for (Medicine med : medicines) {
                if (med.getName().equals(selectedName)) {
                    flagMedicineLow(med.getId());
                    break;
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Flag medicine as low in "medicines" database
    private void flagMedicineLow(String medicineId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("flaggedLowByChild", true);
        updates.put("flaggedAt", new Date());
        updates.put("lastUpdatedBy", "child");

        db.collection("medicines").document(medicineId)
                .update(updates)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Medicine flagged as low", Toast.LENGTH_SHORT).show();

                    //find medicine flagged low to get name and send alert
                    for (Medicine med : medicines) {
                        if (med.getId().equals(medicineId)) {
                            medicineFlaggedLowAlert(med.getName());
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error flagging medicine", Toast.LENGTH_SHORT).show();
                });
    }

    //dialog where parent can configure rapid rescue alert settings
    private void showRapidRescueSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rapid Rescue Alert Settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        //get the number of uses within how long input
        TextView thresholdLabel = new TextView(this);
        thresholdLabel.setText("How many rescue uses to Alert?");
        layout.addView(thresholdLabel);

        EditText thresholdInput = new EditText(this);
        thresholdInput.setHint("3");
        thresholdInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        thresholdInput.setText(String.valueOf(maxRapidRescues));
        layout.addView(thresholdInput);

        TextView hoursLabel = new TextView(this);
        hoursLabel.setText("Within how many hours of each other?");
        hoursLabel.setPadding(0, 20, 0, 0);
        layout.addView(hoursLabel);

        EditText hoursInput = new EditText(this);
        hoursInput.setHint("3");
        hoursInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        hoursInput.setText(String.valueOf(rapidRescuesTimeframe));
        layout.addView(hoursInput);



        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String thresholdText = thresholdInput.getText().toString().trim();
            String hoursText = hoursInput.getText().toString().trim();

            // Check for empty inputs
            if (thresholdText.isEmpty() || hoursText.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int thresholdInputValue = Integer.parseInt(thresholdInput.getText().toString().trim());
                int hoursInputValue = Integer.parseInt(hoursInput.getText().toString().trim());

                if (thresholdInputValue <= 0 || hoursInputValue <= 0 ) {
                    Toast.makeText(this, "Number of rescues and hours must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                //store shared preferences if no error
                maxRapidRescues = thresholdInputValue;
                rapidRescuesTimeframe = hoursInputValue;

                SharedPreferences prefs = getSharedPreferences("medicine_settings", MODE_PRIVATE);
                prefs.edit()
                        .putInt("rapid_rescue_threshold", maxRapidRescues)
                        .putInt("rapid_rescue_hours", rapidRescuesTimeframe)
                        .apply();

                Toast.makeText(this, "Rapid rescue configuration saved!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    //load stored shared prefences for rapidRescue alerts default to 3 if no settings saved
    private void loadRapidRescueSettings() {
        SharedPreferences prefs = getSharedPreferences("medicine_settings", MODE_PRIVATE);
        maxRapidRescues = prefs.getInt("rapid_rescue_threshold", 3);
        rapidRescuesTimeframe = prefs.getInt("rapid_rescue_hours", 3);
    }



    //finds all logs within the configured number of hours and checks
    // if the amount is equal or surpasses the max amount of rescue uses configured
    private void checkForRapidRescuesAlert(String childId) {

        // set the date back by the configured number of hours to check for rescue uses
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -rapidRescuesTimeframe);
        Date configuredHoursBefore = calendar.getTime();

        //get all rescue logs past that number of hours before and get and check if number of returned instances is >=
        //than configured allowed rescue uses and if so send alert
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .whereGreaterThanOrEqualTo("timestamp", configuredHoursBefore)
                .get()
                .addOnSuccessListener(result -> {
                    int rescueCount = result.size();
                    if (rescueCount >= maxRapidRescues) {
                        sendRapidRescueAlert(rescueCount);
                    }
                });
    }


    private void sendRapidRescueAlert(int rescueCount) {


        //find the parent of the child and add a alert instance to the database with all details
        db.collection("children").document(currentChildId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String parentId = documentSnapshot.getString("parentId");
                        Log.d("RapidRescue", "Parent ID: " + parentId);

                        Map<String, Object> alertInfo = new HashMap<>();
                        alertInfo.put("parentId", parentId);
                        alertInfo.put("childId", currentChildId);
                        alertInfo.put("type", "rapid_rescue");
                        alertInfo.put("details", currentChildName + " has logged " + rescueCount + " rescue uses in " + rapidRescuesTimeframe + " hours!");
                        alertInfo.put("timestamp", new Date());

                        db.collection("alerts").add(alertInfo);
                    }
                });
    }


    private void worseAfterDoseAlert(MedicineLog log) {
        //find the parent of the child and add a alert instance to the database with all details
        db.collection("children").document(currentChildId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String parentId = documentSnapshot.getString("parentId");

                        Map<String, Object> alertInfo = new HashMap<>();
                        alertInfo.put("parentId", parentId);
                        alertInfo.put("childId", currentChildId);
                        alertInfo.put("type", "worse_after_dose");
                        alertInfo.put("details", currentChildName + " felt worse after their dose of " + log.getMedicineName());
                        alertInfo.put("timestamp", new Date());

                        db.collection("alerts").add(alertInfo);
            }
        });
    }

    private void medicineFlaggedLowAlert(String medicineName) {

        //find the parent of the child and add a alert instance to the database with all details
        db.collection("children").document(currentChildId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String parentId = documentSnapshot.getString("parentId");

                        Map<String, Object> alertInfo = new HashMap<>();
                        alertInfo.put("parentId", parentId);
                        alertInfo.put("childId", currentChildId);
                        alertInfo.put("type", "medicine_flagged_low");
                        alertInfo.put("details", currentChildName + " flagged " + medicineName + " as running low");
                        alertInfo.put("timestamp", new Date());

                        db.collection("alerts").add(alertInfo);
                    }
                });
    }


    // updates the display of logs
    private void updateLogsDisplay() {

        if (logs.isEmpty()) {
            noLogsText.setVisibility(View.VISIBLE);
            logsRecyclerView.setVisibility(View.GONE);
        } else {
            noLogsText.setVisibility(View.GONE);
            logsRecyclerView.setVisibility(View.VISIBLE);
        }

        adapter.updateLogs(logs);
    }

    //helper to create spinner(selector) given a list of choices we defined in arrays.xml
    private Spinner createSpinner(int arrayResourceId) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayResourceId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

}