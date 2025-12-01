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
        /**
         * Handles the delete/revoke action.
         * @param inviteCode The invite code to be deleted.
         * @param providerId The ID of the provider who accepted the invite, or null if unaccepted.
         * @param position The position of the item in the list.
         */
        void onDeleteInvite(String inviteCode, String providerId, int position);
    }

    // Constructor: Now requires the OnInviteActionListener
    public ProviderInviteAdapter(List<ProviderInvite> providerList, Context context, String childId, OnInviteActionListener listener) {
        this.providerList = providerList;
        this.context = context;
        this.childId = childId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the list item layout (item_invite_provider.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.item_invite_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProviderInvite item = providerList.get(position);

        boolean isAccepted = item.getProviderId() != null && !item.getProviderId().isEmpty();

        // Display the invite code
        holder.tvInviteCode.setText(item.getInviteCode() != null ? item.getInviteCode() : "-");

        // Display providerId or 'Not accepted' status
        holder.tvProviderId.setText(isAccepted ? item.getProviderId() : "Not accepted");

        // --- Revoke Button Logic ---
        if (isAccepted) {
            // If accepted (providerId exists), disable the Revoke button
            holder.btnRevoke.setText("Revoke Blocked");
            holder.btnRevoke.setBackgroundColor(Color.parseColor("#CCCCCC")); // Greyed out color
            holder.btnRevoke.setEnabled(false); // Disable interaction
        } else {
            // If not accepted, keep the original text and enable the button
            holder.btnRevoke.setText("Revoke");
            // Assuming your original layout has a default red color for delete/revoke
            holder.btnRevoke.setBackgroundColor(Color.parseColor("#F44336"));
            holder.btnRevoke.setEnabled(true);
        }
        // ---------------------------------

        // Set listener for the Edit button (navigates to settings activity)
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChildShareSettingsActivity.class);
            intent.putExtra("childId", childId);
            intent.putExtra("inviteCode", item.getInviteCode());
            context.startActivity(intent);
        });

        // Set listener for the Revoke (Delete) button
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

    /**
     * ViewHolder class to hold the item views.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInviteCode, tvProviderId; // tvPermissionStatus REMOVED
        Button btnEdit, btnRevoke;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInviteCode = itemView.findViewById(R.id.tvInviteCode);
            tvProviderId = itemView.findViewById(R.id.tvProviderEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);

            // tvPermissionStatus initialization REMOVED
            btnRevoke = itemView.findViewById(R.id.btnRevoke); // Initialize the new Revoke button
        }
    }
}