package com.example.a1112;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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

                int totalAmount = Integer.parseInt(totalAmountString);

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
        final EditText totalAmountEdit = new EditText(this);
        totalAmountEdit.setHint("New total amount");
        totalAmountEdit.setText(String.valueOf(medicine.getTotalAmount()));
        totalAmountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(totalAmountEdit);

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
            String newAmountString = currentAmountEdit.getText().toString();
            if (!newAmountString.isEmpty()) {
                int newAmount = Integer.parseInt(newAmountString);
                medicine.setCurrentAmount(newAmount);
                medicine.setLastUpdatedBy("parent");
                medicine.setLastUpdatedAt(new Date());

                updateAmount(medicine);
            }
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


}