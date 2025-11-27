package com.example.a1112;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<Medicine> medicineList;
    private OnMedicineClickListener listener;

    public interface OnMedicineClickListener {
        void onMedicineClick(Medicine medicine);
        void onMedicineEditClick(Medicine medicine);
    }

    public MedicineAdapter(List<Medicine> medicineList, OnMedicineClickListener listener) {
        this.medicineList = medicineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);
        holder.bind(medicine, listener);
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    //method to update the display of medicine list
    public void updateMedicines(List<Medicine> newMedicines) {
        medicineList.clear();
        medicineList.addAll(newMedicines);
        notifyDataSetChanged();
    }

    public static class MedicineViewHolder extends RecyclerView.ViewHolder {
         TextView medicineNameText, amountText, percentageText, expiryDateText, statusText, updatedByText, lastUpdatedAtText ;
         ImageButton buttonEdit;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            medicineNameText = itemView.findViewById(R.id.medicineNameText);
            amountText = itemView.findViewById(R.id.amountText);
            percentageText = itemView.findViewById(R.id.percentageText);
            expiryDateText = itemView.findViewById(R.id.expiryDateText);
            statusText = itemView.findViewById(R.id.statusText);
            updatedByText = itemView.findViewById(R.id.updatedByText);
            lastUpdatedAtText = itemView.findViewById(R.id.lastUpdatedAtText);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
        }

        public void bind(Medicine medicine, OnMedicineClickListener listener) {
            //setup the display and click listeners of medicine items

            medicineNameText.setText(medicine.getName() + " (" + medicine.getType() + ")");
            amountText.setText(medicine.getCurrentAmount() + "/" + medicine.getTotalAmount() + " " + medicine.getUnitType() + " left");
            percentageText.setText(medicine.getPercentageLeft() + "%");

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String expiryText = "Expires: " + dateFormat.format(medicine.getExpiryDate());
            expiryDateText.setText(expiryText);

            // displays different status message and color when expired or low amount left
            if (medicine.isExpired()) { // status shows expired with red colors
                statusText.setText("EXPIRED");
                statusText.setBackgroundColor(0xFFFCE4E4);
                statusText.setTextColor(0xFFD32F2F);
            } else if (medicine.isLow()) { // status shows low with yellow colors
                statusText.setText("LOW");
                statusText.setBackgroundColor(0xFFFFF8E1);
                statusText.setTextColor(0xFFF57C00);
            } else {
                statusText.setText("OK"); // status shows ok with green colors
                statusText.setBackgroundColor(0xFFE8F5E8);
                statusText.setTextColor(0xFF388E3C);
            }

            updatedByText.setText("Updated by: " + medicine.getLastUpdatedBy());
            lastUpdatedAtText.setText(getTimeAgo(medicine.getLastUpdatedAt()));

            // set up click listeners (click medicine item or edit button)
            itemView.setOnClickListener(v -> listener.onMedicineClick(medicine));
            buttonEdit.setOnClickListener(v -> listener.onMedicineEditClick(medicine));
        }

        //helper method which returns time between 2 dates
        private String getTimeAgo(Date date) {
            long difference = new Date().getTime() - date.getTime();
            //divide by 60000 because its measured in milliseconds
            long minutes = difference / 60000;

            if (minutes < 60) {
                return minutes + " min ago";
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + " hrs ago";
            }
            return (hours / 24) + " days ago";
        }
    }
}