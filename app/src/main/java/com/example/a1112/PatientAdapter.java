package com.example.a1112;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<Child> patientList;
    private OnPatientClickListener listener;


    public interface OnPatientClickListener {
        void onPatientClick(Child child);
    }

    public PatientAdapter(List<Child> patientList, OnPatientClickListener listener) {
        this.patientList = patientList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PatientViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Child child = patientList.get(position);

        holder.patientNameText.setText(child.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPatientClick(child);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView patientNameText;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            patientNameText = itemView.findViewById(android.R.id.text1);
        }
    }
}