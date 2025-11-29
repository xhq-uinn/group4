package com.example.a1112;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicineLogAdapter extends RecyclerView.Adapter<MedicineLogAdapter.LogViewHolder> {

    private List<MedicineLog> logs;

    public MedicineLogAdapter(List<MedicineLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        MedicineLog log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public void updateLogs(List<MedicineLog> newLogs) {
        this.logs = new ArrayList<>(newLogs);
        notifyDataSetChanged();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView logMedicineNameText, logTimeText, logDetailsText, logRatingText;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logMedicineNameText = itemView.findViewById(R.id.logMedicineNameText);
            logTimeText = itemView.findViewById(R.id.logTimeText);
            logDetailsText = itemView.findViewById(R.id.logDetailsText);
            logRatingText = itemView.findViewById(R.id.logRatingText);
        }

        public void bind(MedicineLog log) {
            //setup display of medicine log items (name, timestamp, dose count, logged by and color coded post feeling(optional))

            logMedicineNameText.setText(log.getMedicineName());

            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            logTimeText.setText(timeFormat.format(log.getTimestamp()));

            //initialize unit type to doses in case we cant get it from Medicinelog object
            String unitType = "doses";
            if (log.getUnitType() != null) {
                unitType = log.getUnitType();
            }

            int doseCount = log.getDoseCount();
            if (doseCount == 1) {
                if (unitType == "puffs") {
                    logDetailsText.setText( "1 puff | Logged by " + log.getLoggedBy());
                }
                else if (unitType == "measures") {
                    logDetailsText.setText("1 measure | Logged by " + log.getLoggedBy());
                }
                else
                {
                    logDetailsText.setText(" 1 dose | Logged by " + log.getLoggedBy());
                }
            }
            else {
                logDetailsText.setText(doseCount + " " + unitType + " | Logged by " + log.getLoggedBy());
            }


            if (log.getPostFeeling() != null && !log.getPostFeeling().isEmpty()) {
                String feeling = log.getPostFeeling();
                logRatingText.setVisibility(View.VISIBLE);
                logRatingText.setText("Post check: " + feeling + " after");


                // different colors for different feelings
                if ("Better".equals(feeling)) {
                    logRatingText.setTextColor(0xFF4CAF50); // Green
                }
                else if ("Worse".equals(feeling)) {
                    logRatingText.setTextColor(0xFFF44336); // Red
                }
                else {
                    logRatingText.setTextColor(0xFFFF9800); // Orange
                }
            }
            else //no text if no rating
            {
                logRatingText.setVisibility(View.GONE);
            }
        }

    }
}