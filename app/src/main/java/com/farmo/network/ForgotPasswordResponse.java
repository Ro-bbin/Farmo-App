package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordResponse {
    @SerializedName("half_email")
    private String halfEmail;
    
    @SerializedName("user_id")
    private String userId;

    @SerializedName("error")
    private String error;

    public String getHalfEmail() { return halfEmail; }
    public String getUserId() { return userId; }
    public String getError() { return error; }
}
