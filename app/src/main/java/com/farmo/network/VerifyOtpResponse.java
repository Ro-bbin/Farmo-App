package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpResponse {
    @SerializedName("verified")
    private boolean verified;

    @SerializedName("error")
    private String error;

    public boolean isVerified() { return verified; }
    public String getError() { return error; }
}
