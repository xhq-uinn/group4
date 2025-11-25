package com.example.a1112;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<Child> childList;
    private OnChildActionListener listener;

    public interface OnChildActionListener {
        void onEditChild(Child child);
        void onDeleteChild(Child child);
        void onShareSettings(Child child);
        void onOpenChildHome(Child child);
    }

    public ChildAdapter(List<Child> childList, OnChildActionListener listener) {
        this.childList = childList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Child child = childList.get(position);
        holder.nameText.setText(child.getName());
        holder.ageText.setText(String.valueOf(child.getAge())); // convert int to string

        holder.editBtn.setOnClickListener(v -> listener.onEditChild(child));
        holder.deleteBtn.setOnClickListener(v -> listener.onDeleteChild(child));
        holder.shareBtn.setOnClickListener(v -> listener.onShareSettings(child));

        holder.dailyCheckInBtn.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DailyCheckInActivity.class);
            intent.putExtra("childId", child.getId());
            intent.putExtra("authorRole", "parent");
            v.getContext().startActivity(intent);
        });

        holder.childHomeBtn.setOnClickListener(v -> listener.onOpenChildHome(child));
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, ageText;
        Button editBtn, deleteBtn, shareBtn, dailyCheckInBtn;
        Button childHomeBtn;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textChildName);
            ageText = itemView.findViewById(R.id.textChildAge);
            editBtn = itemView.findViewById(R.id.btn_edit_child);
            deleteBtn = itemView.findViewById(R.id.btn_delete_child);
            shareBtn = itemView.findViewById(R.id.btn_share_settings);
            dailyCheckInBtn = itemView.findViewById(R.id.btn_daily_check_in);
            childHomeBtn = itemView.findViewById(R.id.btn_child_home);
        }
    }
}
