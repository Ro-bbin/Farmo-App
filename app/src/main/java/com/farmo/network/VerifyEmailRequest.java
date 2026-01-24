package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class VerifyEmailRequest {
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("email")
    private String email;

    public VerifyEmailRequest(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
