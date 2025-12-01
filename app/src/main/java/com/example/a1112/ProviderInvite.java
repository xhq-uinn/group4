package com.example.a1112;

import java.util.Map;

public class ProviderInvite {

    // Unique code for the invitation
    public String inviteCode;

    // ID of the provider who accepted the invite (null if unaccepted)
    public String providerId;

    public Map<String, Boolean> sharingFields;


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

}