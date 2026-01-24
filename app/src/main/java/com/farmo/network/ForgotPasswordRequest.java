package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordRequest {
    @SerializedName("identifier")
    private String identifier;

    public ForgotPasswordRequest(String identifier) {
        this.identifier = identifier;
    }
}
