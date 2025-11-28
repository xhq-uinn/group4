package com.example.a1112;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color; // Import Color class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProviderInviteAdapter extends RecyclerView.Adapter<ProviderInviteAdapter.ViewHolder> {

    private final List<ProviderInvite> providerList;
    private final Context context;
    private final String childId;

    // Constructor
    public ProviderInviteAdapter(List<ProviderInvite> providerList, Context context, String childId) {
        this.providerList = providerList;
        this.context = context;
        this.childId = childId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for the list item
        View view = LayoutInflater.from(context).inflate(R.layout.item_invite_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProviderInvite item = providerList.get(position);

        // Display the invite code
        holder.tvInviteCode.setText(item.getInviteCode() != null ? item.getInviteCode() : "-");

        // Display providerId or 'Not accepted' status
        holder.tvProviderId.setText(item.getProviderId() != null ? item.getProviderId() : "Not accepted");

        // --- NEW LOGIC: Display and handle overall permission status ---
        if (item.isPermissionEnabled()) {
            holder.tvPermissionStatus.setText("Sharing: ON");
            holder.tvPermissionStatus.setTextColor(Color.parseColor("#4CAF50")); // Green color for ON
            holder.itemView.setAlpha(1.0f); // Normal opacity
        } else {
            holder.tvPermissionStatus.setText("Sharing: DISABLED");
            holder.tvPermissionStatus.setTextColor(Color.RED); // Red color for DISABLED
            holder.itemView.setAlpha(0.6f); // Reduced opacity to indicate disabled
        }

        // Click edit to navigate to ChildShareSettingsActivity
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChildShareSettingsActivity.class);
            intent.putExtra("childId", childId);
            intent.putExtra("inviteCode", item.getInviteCode());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return providerList.size();
    }

    // ViewHolder class to hold the item views
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInviteCode, tvProviderId, tvPermissionStatus; // Added tvPermissionStatus
        Button btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInviteCode = itemView.findViewById(R.id.tvInviteCode);
            tvProviderId = itemView.findViewById(R.id.tvProviderEmail); // Reusing an existing ID or assuming a new one
            btnEdit = itemView.findViewById(R.id.btnEdit);

            tvPermissionStatus = itemView.findViewById(R.id.tvPermissionStatus);
        }
    }
}