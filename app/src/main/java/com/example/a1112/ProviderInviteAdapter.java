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
    private final OnInviteActionListener listener; // Listener for handling revoke actions


    public interface OnInviteActionListener {

        void onDeleteInvite(String inviteCode, String providerId, int position);
    }

    public ProviderInviteAdapter(List<ProviderInvite> providerList, Context context, String childId, OnInviteActionListener listener) {
        this.providerList = providerList;
        this.context = context;
        this.childId = childId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_invite_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProviderInvite item = providerList.get(position);

        boolean isAccepted = item.getProviderId() != null && !item.getProviderId().isEmpty();

        // display the invite code
        holder.tvInviteCode.setText(item.getInviteCode() != null ? item.getInviteCode() : "-");

        // display providerId or 'Not accepted' status
        holder.tvProviderId.setText(isAccepted ? item.getProviderId() : "Not accepted");

        // Revoke Button
        if (isAccepted) {
            // If accepted (providerId exists), disable the Revoke button
            holder.btnRevoke.setText("Revoke Blocked");
            holder.btnRevoke.setBackgroundColor(Color.parseColor("#CCCCCC")); // Greyed out color
            holder.btnRevoke.setEnabled(false); // Disable interaction
        } else {
            holder.btnRevoke.setText("Revoke");
            // Assuming your original layout has a default red color for delete/revoke
            holder.btnRevoke.setBackgroundColor(Color.parseColor("#F44336"));
            holder.btnRevoke.setEnabled(true);
        }

        // listener for the Edit button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChildShareSettingsActivity.class);
            intent.putExtra("childId", childId);
            intent.putExtra("inviteCode", item.getInviteCode());
            context.startActivity(intent);
        });

        // listener for Revoke button
        holder.btnRevoke.setOnClickListener(v -> {
            if (listener != null) {
                // Delegate the deletion request back to the Activity/Listener.
                listener.onDeleteInvite(item.getInviteCode(), item.getProviderId(), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return providerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInviteCode, tvProviderId;
        Button btnEdit, btnRevoke;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInviteCode = itemView.findViewById(R.id.tvInviteCode);
            tvProviderId = itemView.findViewById(R.id.tvProviderEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);

            btnRevoke = itemView.findViewById(R.id.btnRevoke);
        }
    }
}