package com.farmo.network;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("identifier")
    private String identifier;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("is_admin")
    private boolean isAdmin;
    
    @SerializedName("device_info")
    private String deviceInfo;

    public LoginRequest(String identifier, String password, boolean isAdmin , String deviceInfo) {
        this.identifier = identifier;
        this.password = password;
        this.isAdmin = false;
        this.deviceInfo = deviceInfo;
    }
}
