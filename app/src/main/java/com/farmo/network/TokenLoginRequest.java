package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class TokenLoginRequest {
    @SerializedName("token")
    private String token;
    
    @SerializedName("refresh_token")
    private String refreshToken;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("is_admin")
    private boolean isAdmin;
    
    @SerializedName("device_info")
    private String deviceInfo;

    public TokenLoginRequest(String token, String refreshToken, String userId, boolean isAdmin, String deviceInfo) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.deviceInfo = deviceInfo;
    }
}
