package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class DashboardService {
    public static class DashboardRequest{

    }

    public static class DashboardResponse{
        @SerializedName("username")
        private String username;

        @SerializedName("wallet_ammount")
        private String wallet_amt;

        @SerializedName("today_income")
        private String income;

        @SerializedName("my_rating")
        private String rating;

        @SerializedName("error")
        private String error;


        // --- Getters ---
        public String getUsername() { return username; }
        public String getWallet_amt() { return wallet_amt; }
        public String getIncome() { return income; }
        public String getRating() { return rating; }
        public String getError() { return error; }


    }
}
