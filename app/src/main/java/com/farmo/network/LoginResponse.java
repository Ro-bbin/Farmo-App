package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_type")
    private String userType;

    @SerializedName("error")
    private String error;

    @SerializedName("error_code")
    private String errorCode;

    // Getters
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getUserId() { return userId; }
    public String getUserType() { return userType; }
    public String getError() { return error; }
    public String getErrorCode() { return errorCode; }
}
