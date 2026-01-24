package com.farmo.network;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("password")
    private String password;

    public ChangePasswordRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }
}
