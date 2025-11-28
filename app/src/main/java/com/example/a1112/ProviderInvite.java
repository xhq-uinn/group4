package com.example.a1112;

import java.util.Map;

public class ProviderInvite {
    // Unique code for the invitation
    public String inviteCode;
    // ID of the provider who accepted the invite (null if unaccepted)
    public String providerId;
    // Map containing all specific sharing permissions (e.g., rescueLogs, symptoms, permissionEnabled)
    public Map<String, Boolean> sharingFields;

    // Dedicated field for the overall sharing status, extracted from sharingFields
    private boolean permissionEnabled;

    // Default constructor for Firebase deserialization
    public ProviderInvite() {}

    // Constructor used when only inviteCode and sharingFields are available (less common now)
    public ProviderInvite(String inviteCode, Map<String, Boolean> sharingFields) {
        this(inviteCode, null, sharingFields);
    }

    // Main constructor used by the Adapter
    public ProviderInvite(String inviteCode, String providerId, Map<String, Boolean> sharingFields) {
        this.inviteCode = inviteCode;
        this.providerId = providerId;
        this.sharingFields = sharingFields;

        // Extract the overall permission status. Defaults to true if missing, though it should exist.
        this.permissionEnabled = sharingFields.getOrDefault("permissionEnabled", true);
    }

    // --- Getters ---

    public String getInviteCode() {
        return inviteCode;
    }

    public String getProviderId() {
        return providerId;
    }

    public Map<String, Boolean> getSharingFields() {
        return sharingFields;
    }

    // Getter for the overall permission status
    public boolean isPermissionEnabled() {
        return permissionEnabled;
    }
}