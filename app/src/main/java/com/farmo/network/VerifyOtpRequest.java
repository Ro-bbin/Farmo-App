package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("otp")
    private String otp;

    public VerifyOtpRequest(String userId, String otp) {
        this.userId = userId;
        this.otp = otp;
    }
}
