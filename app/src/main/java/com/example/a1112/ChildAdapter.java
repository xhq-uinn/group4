package com.example.a1112;

import android.content.Context;
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
        holder.dobText.setText(child.getDob());

        holder.editBtn.setOnClickListener(v -> listener.onEditChild(child));
        holder.deleteBtn.setOnClickListener(v -> listener.onDeleteChild(child));
        holder.shareBtn.setOnClickListener(v -> listener.onShareSettings(child));
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, dobText;
        Button editBtn, deleteBtn, shareBtn;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textChildName);
            dobText = itemView.findViewById(R.id.textChildDOB);
            editBtn = itemView.findViewById(R.id.btn_edit_child);
            deleteBtn = itemView.findViewById(R.id.btn_delete_child);
            shareBtn = itemView.findViewById(R.id.btn_share_settings);
        }
    }
}
