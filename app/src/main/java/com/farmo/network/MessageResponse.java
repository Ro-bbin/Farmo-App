package com.farmo.network;

import com.google.gson.annotations.SerializedName;

public class MessageResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private String error;

    @SerializedName("missing")
    private String missing;

    public String getMessage() { return message; }
    public String getError() { return error; }
    public String getMissing() { return missing; }
}
