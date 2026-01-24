package com.farmo.network;

import com.google.gson.annotations.SerializedName;

public class ActivateAccountRequest {
    @SerializedName("identifier")
    private String identifier;
    @SerializedName("old_password")
    private String oldPassword;
    @SerializedName("new_password")
    private String newPassword;

    public ActivateAccountRequest(String identifier, String oldPassword, String newPassword) {
        this.identifier = identifier;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
