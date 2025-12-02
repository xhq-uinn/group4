package com.example.a1112;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InventoryActivity extends AppCompatActivity {
    // instance variables for UI components
    private TextView childNameText, noMedicinesText;
    private RecyclerView medicinesRecyclerView;
    private Button buttonAddMedicine, buttonHome;


    private MedicineAdapter adapter;
    private List<Medicine> medicines = new ArrayList<>();

    //Data of child passed from previous screen when routed to inventory
    private String currentChildId;
    private String currentChildName;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration medicineListener;

    private Date selectedPurchaseDate;
    private Date selectedExpiryDate;




    // date formatter for display
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // get child info from intent
        Intent intent = getIntent();
        currentChildId = intent.getStringExtra("CHILD_ID");
        currentChildName = intent.getStringExtra("CHILD_NAME");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupMedicineList();

    }

    //remove real time listener when activity is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (medicineListener != null) {
            medicineListener.remove();
        }
    }

    private void initializeViews() {
        childNameText = findViewById(R.id.childNameText);
        noMedicinesText = findViewById(R.id.noMedicinesText);
        medicinesRecyclerView = findViewById(R.id.medicinesRecyclerView);
        buttonAddMedicine = findViewById(R.id.buttonAddMedicine);
        buttonHome = findViewById(R.id.buttonHome);

        if (currentChildName != null) {
            childNameText.setText(currentChildName + "'s Medicines");
        }

    }

    private void setupRecyclerView() {
        adapter = new MedicineAdapter(medicines, new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                changeAmountPopup(medicine);
            }

            @Override
            public void onMedicineEditClick(Medicine medicine) {
                editMedicinePopup(medicine);
            }
        });

        medicinesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicinesRecyclerView.setAdapter(adapter);
    }

    void setupClickListeners() {
        //show dialog to add medicine when add medicine button is clicked
        buttonAddMedicine.setOnClickListener(view -> {

            //reinitialize dates to current time
            selectedPurchaseDate  = new Date();
            selectedExpiryDate = new Date();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add New Medicine");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40);

            // medicine name input
            final EditText nameInput = new EditText(this);
            nameInput.setHint("Medicine Name");
            layout.addView(nameInput);

            // spinners to select medicine type (controller/rescue) and its unit (puffs/measures)
            Spinner medicineTypeSelect = createSpinner(R.array.medicine_types);
            layout.addView(medicineTypeSelect);
            Spinner unitTypeSelect = createSpinner(R.array.medicine_units);
            layout.addView(unitTypeSelect);

            // total amount input
            final EditText totalAmountInput = new EditText(this);
            totalAmountInput.setHint("Total Amount");
            totalAmountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            layout.addView(totalAmountInput);

            // buttons making a calendar dialog pop up to choose a date of purchase and of expiry
            final Button buttonPurchaseDate = new Button(this);
            buttonPurchaseDate.setText("Purchase Date: " + dateFormatter.format(selectedPurchaseDate));
            buttonPurchaseDate.setOnClickListener(v -> showDatePicker(buttonPurchaseDate, "purchase"));
            layout.addView(buttonPurchaseDate);

            final Button buttonExpiryDate = new Button(this);
            buttonExpiryDate.setText("Expiry Date: " + dateFormatter.format(selectedExpiryDate));
            buttonExpiryDate.setOnClickListener(v -> showDatePicker(buttonExpiryDate, "expiry"));
            layout.addView(buttonExpiryDate);

            builder.setView(layout);

            //if no issue with input adds medicine to database when user hits add button in the popup
            builder.setPositiveButton("Add", (dialog, which) -> {
                String name = nameInput.getText().toString();
                String type = medicineTypeSelect.getSelectedItem().toString();
                String unitType = unitTypeSelect.getSelectedItem().toString();
                String totalAmountString = totalAmountInput.getText().toString();

                if (name.isEmpty() || totalAmountString.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                int totalAmount;
                try {
                    totalAmount = Integer.parseInt(totalAmountString);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Total amount must be a number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (totalAmount < 1) {
                    Toast.makeText(this, "Total amount must be at least " + 1, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedPurchaseDate.after(new Date()) && !isSameDay(selectedPurchaseDate, new Date())) {
                    Toast.makeText(this, "Purchase date cannot be in the future", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!selectedExpiryDate.after(new Date())) {
                    Toast.makeText(this, "Expiry date must be in the future", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedPurchaseDate.after(selectedExpiryDate)) {
                    Toast.makeText(this, "Purchase date must be before expiry date", Toast.LENGTH_SHORT).show();
                    return;
                }

                Medicine newMedicine = new Medicine(
                        currentChildId,
                        name,
                        type,
                        unitType,
                        selectedPurchaseDate,
                        selectedExpiryDate,
                        totalAmount
                );

                addMedicine(newMedicine);
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        buttonHome.setOnClickListener(view -> {
            Intent intent = new Intent(InventoryActivity.this, ParentHomeActivity.class);
            startActivity(intent);
        });



    }


    private void setupMedicineList() {

        //real-time listener for all medicine belonging to the child of the parent that needs to be displayed
        medicineListener = db.collection("medicines")
                .whereEqualTo("childId", currentChildId)
                .addSnapshotListener((result, error) -> {

                    if (error != null) {
                        Toast.makeText(this, "Error loading medicines", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //no error then we clear the medicine list and replace with the updated list
                    medicines.clear();
                    if (result != null) {
                        for (QueryDocumentSnapshot doc : result) {
                            Medicine medicine = doc.toObject(Medicine.class);
                            medicine.setId(doc.getId());
                            medicines.add(medicine);
                        }
                    }

                    //could possibly have an empty list so we call updateNoMedicinesDisplay function to have it display a message for that case
                    updateNoMedicinesDisplay();

                    adapter.notifyDataSetChanged();

                    checkMedicinesExpiry();
                    checkMedicinesLow();

                });
    }

    //add medicine to database
    private void addMedicine(Medicine medicine) {
        //have to manually build a map for all fields becuase medicines collection doesnt have an id field
        //as we use document id but medicine class has an id field so we cant just add the the object itself
        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("childId", medicine.getChildId());
        medicineData.put("name", medicine.getName());
        medicineData.put("type", medicine.getType());
        medicineData.put("unitType", medicine.getUnitType());
        medicineData.put("totalAmount", medicine.getTotalAmount());
        medicineData.put("currentAmount", medicine.getCurrentAmount());
        medicineData.put("purchaseDate", medicine.getPurchaseDate());
        medicineData.put("expiryDate", medicine.getExpiryDate());
        medicineData.put("lastUpdatedBy", medicine.getLastUpdatedBy());
        medicineData.put("lastUpdatedAt", medicine.getLastUpdatedAt());
        medicineData.put("createdAt", medicine.getCreatedAt());
        medicineData.put("flaggedLowByChild", medicine.isFlaggedLowByChild());
        medicineData.put("flaggedAt", medicine.getFlaggedAt());

        db.collection("medicines")
                .add(medicineData)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Medicine added successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "error adding medicine!", Toast.LENGTH_SHORT).show();
                });
    }

    //update medicine fields in firestore database when parent modifies info using edit button
    private void updateMedicine(Medicine medicine) {
        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            Toast.makeText(this, "Medicine ID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", medicine.getName());
        updates.put("type", medicine.getType());
        updates.put("unitType", medicine.getUnitType());
        updates.put("totalAmount", medicine.getTotalAmount());
        updates.put("currentAmount", medicine.getCurrentAmount());
        updates.put("purchaseDate", medicine.getPurchaseDate());
        updates.put("expiryDate", medicine.getExpiryDate());
        updates.put("lastUpdatedBy", medicine.getLastUpdatedBy());
        updates.put("lastUpdatedAt", medicine.getLastUpdatedAt());
        updates.put("flaggedLowByChild", medicine.isFlaggedLowByChild());
        updates.put("flaggedAt", medicine.getFlaggedAt());

        db.collection("medicines").document(medicine.getId())
                .update(updates)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Medicine updated", Toast.LENGTH_SHORT).show();
                    //check for low medcicne amount after updating a medicines details
                    checkMedicineLowAlert(medicine);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update medicine", Toast.LENGTH_SHORT).show();
                });
    }

   //updates current amount in database and tracks timestamp and user type
    private void updateAmount(Medicine medicine) {
        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            Toast.makeText(this, "Medicine ID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentAmount", medicine.getCurrentAmount());
        updates.put("lastUpdatedBy", medicine.getLastUpdatedBy());
        updates.put("lastUpdatedAt", medicine.getLastUpdatedAt());

        db.collection("medicines").document(medicine.getId())
                .update(updates)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Amount updated", Toast.LENGTH_SHORT).show();
                    //check for low medcicne amount after updating a medicines current amount
                    checkMedicineLowAlert(medicine);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update amount", Toast.LENGTH_SHORT).show();
                });
    }

    //removes medicine instance from database when done so from edit button
    private void deleteMedicine(Medicine medicine) {
        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            Toast.makeText(this, "Medicine ID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("medicines").document(medicine.getId())
                .delete()
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {;
                    Toast.makeText(this, "Failed to delete medicine", Toast.LENGTH_SHORT).show();
                });
    }


    // popup where user can edit medicine info
    private void editMedicinePopup(Medicine medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + medicine.getName());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final EditText nameEdit = new EditText(this);
        nameEdit.setText(medicine.getName());
        layout.addView(nameEdit);

        // Selectors to change medicine type and unit type which is defaulted to its current value so user wont
        //need to remember
        Spinner medicineTypeSelector = createSpinner(R.array.medicine_types);
        setValueSelector(medicineTypeSelector, medicine.getType());
        layout.addView(medicineTypeSelector);

        Spinner unitSelector = createSpinner(R.array.medicine_units);
        setValueSelector(unitSelector, medicine.getUnitType());
        layout.addView(unitSelector);

        //input fields to edit total or current amount of medicine
        TextView totalAmountTitle = new TextView(this);
        totalAmountTitle.setText("Total Amount");
        layout.addView(totalAmountTitle);

        final EditText totalAmountEdit = new EditText(this);
        totalAmountEdit.setHint("New total amount");
        totalAmountEdit.setText(String.valueOf(medicine.getTotalAmount()));
        totalAmountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(totalAmountEdit);

        TextView currentAmountTitle = new TextView(this);
        currentAmountTitle.setText("Current Amount");
        layout.addView(currentAmountTitle);

        final EditText currentAmountEdit = new EditText(this);
        currentAmountEdit.setHint("New current amount");
        currentAmountEdit.setText(String.valueOf(medicine.getCurrentAmount()));
        currentAmountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(currentAmountEdit);

        builder.setView(layout);

        //updates medicine info in database when save button is hit in edit popup
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = nameEdit.getText().toString();
            String medicineType = medicineTypeSelector.getSelectedItem().toString();
            String unit = unitSelector.getSelectedItem().toString();
            String totalAmountString = totalAmountEdit.getText().toString();
            String currentAmountString = currentAmountEdit.getText().toString();

            if (name.isEmpty() || totalAmountString.isEmpty() || currentAmountString.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalAmount, currentAmount;
            try {
                totalAmount = Integer.parseInt(totalAmountString);
                currentAmount = Integer.parseInt(currentAmountString);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Amounts must be numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            if (totalAmount < 1) {
                Toast.makeText(this, "Total amount must be at least " + 1, Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentAmount > totalAmount) {
                Toast.makeText(this, "Current amount cannot be greater than total amount", Toast.LENGTH_SHORT).show();
                return;
            }

            medicine.setName(name);
            medicine.setType(medicineType);
            medicine.setUnitType(unit);
            medicine.setTotalAmount(Integer.parseInt(totalAmountString));
            medicine.setCurrentAmount(Integer.parseInt(currentAmountString));
            medicine.setLastUpdatedBy("parent");
            medicine.setLastUpdatedAt(new Date());

            updateMedicine(medicine);
        });

        //cancel button to exit popup
        builder.setNegativeButton("Cancel", null);

        //delete button which prompts confirmation of delete and if confirmed deletes the medicine form database
        builder.setNeutralButton("Delete", (dialog, which) -> {
            confirmDelete(medicine);
        });

        builder.show();
    }


    private void changeAmountPopup(Medicine medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Amount Left");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        //show current amount
        TextView displayCurrentAmount = new TextView(this);
        displayCurrentAmount.setText("Current: " + medicine.getCurrentAmount() + " / " + medicine.getTotalAmount());
        layout.addView(displayCurrentAmount);

        //input new current amount
        final EditText currentAmountEdit = new EditText(this);
        currentAmountEdit.setHint("New current amount");
        currentAmountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        currentAmountEdit.setText(String.valueOf(medicine.getCurrentAmount()));
        layout.addView(currentAmountEdit);

        builder.setView(layout);

        //changes current amount, timestamp of change and tracks that its parent marked when update button is hit
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newAmountString = currentAmountEdit.getText().toString().trim();
            if (newAmountString.isEmpty()) {
                Toast.makeText(this, "Please enter a new amount", Toast.LENGTH_SHORT).show();
                return;
            }

            int newAmount;
            try {
                newAmount = Integer.parseInt(newAmountString);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Amount must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newAmount > medicine.getTotalAmount()) {
                Toast.makeText(this, "Current amount cannot be greater than total amount ", Toast.LENGTH_SHORT).show();
                return;
            }

            medicine.setCurrentAmount(newAmount);
            medicine.setLastUpdatedBy("parent");
            medicine.setLastUpdatedAt(new Date());

            updateAmount(medicine);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    //simple dialog popup to confirm user wants to delete the medicine
    private void confirmDelete(Medicine medicine) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setMessage("Are you sure you want to delete " + medicine.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMedicine(medicine);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    //Helper functions

    //defaults a selector to a wanted value
    private void setValueSelector(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }


    // creates spinner(selector) given a list of choices we defined in arrays.xml
    private Spinner createSpinner(int arrayResourceId) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayResourceId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    //displays a date picker when a parent is adding an expiry date or purchase date for medicine
    private void showDatePicker(Button button, String dateType) {
        Date currentDate = selectedPurchaseDate;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, month, dayOfMonth);

                    if (dateType.equals("purchase")) {
                        selectedPurchaseDate = newDate.getTime();
                        button.setText("Purchase Date: " + dateFormatter.format(selectedPurchaseDate));
                    } else {
                        selectedExpiryDate = newDate.getTime();
                        button.setText("Expiry Date: " + dateFormatter.format(selectedExpiryDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();

    }

    //Displays or hides a textview message depending on if the list of medicine is empty or has something in it
    private void updateNoMedicinesDisplay() {
        if (medicines.isEmpty()) {
            noMedicinesText.setVisibility(View.VISIBLE);
            medicinesRecyclerView.setVisibility(View.GONE);
        } else {
            noMedicinesText.setVisibility(View.GONE);
            medicinesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    private void checkMedicineLowAlert(Medicine medicine) {
        if (medicine == null || medicine.getTotalAmount() <= 0) {
            return;
        }

        double percentageRemaining = ((double) medicine.getCurrentAmount() / medicine.getTotalAmount()) * 100;

        if (percentageRemaining <= 20 && percentageRemaining > 0) {
            sendMedicineLowAlert(medicine.getName(), percentageRemaining, medicine.getCurrentAmount());
        }
    }

    private void sendMedicineLowAlert (String medicineName, double percentageRemaining, int currentAmount) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String parentId = currentUser.getUid();

        String percentageString = String.format(Locale.getDefault(), "%.0f%%", percentageRemaining);

        Map<String, Object> alertInfo = new HashMap<>();
        alertInfo.put("parentId", parentId);
        alertInfo.put("childId", currentChildId);
        alertInfo.put("type", "medicine_low");
        alertInfo.put("medicineName", medicineName);
        alertInfo.put("details", currentChildName + "'s " + medicineName +
                " is running low (" + percentageString + " remaining, " +
                currentAmount + " left)");
        alertInfo.put("timestamp", new Date());

        db.collection("alerts").add(alertInfo)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this,
                            medicineName + " is running low! (" + percentageString + " remaining)",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("LowMedicineAlert", "Failed to send low medicine alert", e);
                });
    }

    private void checkMedicinesLow() {
        for (Medicine medicine : medicines) {
            checkMedicineLowAlert(medicine);
        }
    }

    //check if medicines are expired or within 7 days of expiry and send alerts accordingly
    private void checkMedicinesExpiry() {
        if (medicines == null || medicines.isEmpty()) {
            return;
        }

        Date now = new Date();
        Calendar expiryWarningCal = Calendar.getInstance();
        expiryWarningCal.setTime(now);
        expiryWarningCal.add(Calendar.DAY_OF_YEAR, 7);
        Date warningThreshold = expiryWarningCal.getTime();


        List<String> expiredMedicineNames = new ArrayList<>();
        List<String> expiringSoonMedicineNames = new ArrayList<>();

        for (Medicine medicine : medicines) {
            Date expiryDate = medicine.getExpiryDate();

            if (expiryDate == null) {
                continue;
            }

            if (expiryDate.before(now)) {
                expiredMedicineNames.add(medicine.getName());
                Toast.makeText(this,medicine.getName() + " has EXPIRED!", Toast.LENGTH_LONG).show();
            }

            else if (expiryDate.before(warningThreshold) || isSameDay(expiryDate, warningThreshold)) {
                expiringSoonMedicineNames.add(medicine.getName());
                Toast.makeText(this, medicine.getName() + " expires SOON!", Toast.LENGTH_LONG).show();

            }
        }

        //send alerts if lists of expired/soon expired medicines are nonempty with correct alert type
        if (!expiredMedicineNames.isEmpty()) {
            ExpiryAlert("expired", expiredMedicineNames);
        }
        if (!expiringSoonMedicineNames.isEmpty()) {
            ExpiryAlert("expiring_soon", expiringSoonMedicineNames);
        }
    }


    private void ExpiryAlert(String alertType, List<String> medicineNames) {
        if (medicineNames == null || medicineNames.isEmpty()) {
            return;
        }
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String parentId = currentUser.getUid();

        StringBuilder medicineList = new StringBuilder();
        for (int i = 0; i < medicineNames.size(); i++) {
            medicineList.append(medicineNames.get(i));
            if (i < medicineNames.size() - 1) {
                medicineList.append(", ");
            }
        }

        Map<String, Object> alertInfo = new HashMap<>();
        alertInfo.put("parentId", parentId);
        alertInfo.put("childId", currentChildId);

        if ("expired".equals(alertType)) {
            alertInfo.put("type", "medicine_expired");
            alertInfo.put("details", currentChildName + "'s medicines have EXPIRED: " + medicineList.toString());
        }
        else
        {
            alertInfo.put("type", "medicine_expiring_soon");
            alertInfo.put("details", currentChildName + "'s medicines expire in 7 days or less: " + medicineList.toString());
        }

        alertInfo.put("timestamp", new Date());

        db.collection("alerts").add(alertInfo)
                .addOnFailureListener(e -> {
                    Log.e("InventoryActivity", "Failed to send expiry alert", e);
                });
    }

    //helper which checks if 2 dates are the same day
    private boolean isSameDay(Date date1, Date date2) {
        Calendar day1 = Calendar.getInstance();
        Calendar day2 = Calendar.getInstance();
        day1.setTime(date1);
        day2.setTime(date2);

        boolean sameDay = day1.get(Calendar.DAY_OF_MONTH) == day2.get(Calendar.DAY_OF_MONTH);
        boolean sameMonth = day1.get(Calendar.MONTH) == day2.get(Calendar.MONTH);
        boolean sameYear = day1.get(Calendar.YEAR) == day2.get(Calendar.YEAR);

        return  sameDay && sameMonth && sameYear;

    }


}
