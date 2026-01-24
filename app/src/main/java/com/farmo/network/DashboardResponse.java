package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class DashboardResponse {
    @SerializedName("name")
    private String name;
    
    @SerializedName("wallet_amt")
    private String walletAmt;
    
    @SerializedName("income")
    private String income;
    
    @SerializedName("rating")
    private float rating;

    public String getName() { return name; }
    public String getWalletAmt() { return walletAmt; }
    public String getIncome() { return income; }
    public float getRating() { return rating; }
}
