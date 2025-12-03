package com.example.a1112;

import java.util.Map;

public class ProviderInvite {

    public String inviteCode;

    // ID of the provider who accepted the invite (null if unaccepted)
    public String providerId;

    public Map<String, Boolean> sharingFields;


    public ProviderInvite() {}

    public ProviderInvite(String inviteCode, Map<String, Boolean> sharingFields) {
        this(inviteCode, null, sharingFields);
    }

    // constructor used by the Adapter
    public ProviderInvite(String inviteCode, String providerId, Map<String, Boolean> sharingFields) {
        this.inviteCode = inviteCode;
        this.providerId = providerId;
        this.sharingFields = sharingFields;
    }

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