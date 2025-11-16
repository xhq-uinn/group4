package com.example.a1112;

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

    public ChildAdapter(List<Child> childList) {
        this.childList = childList;
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

        // click on share settings button → share settings
        holder.shareBtn.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChildShareSettingsActivity.class);
            intent.putExtra("childId", child.getId());
            intent.putExtra("childName", child.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, dobText;
        Button shareBtn;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textChildName);
            dobText = itemView.findViewById(R.id.textChildDOB);
            shareBtn = itemView.findViewById(R.id.btn_share_settings);
        }
    }
}
