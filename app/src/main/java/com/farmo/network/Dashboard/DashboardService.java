package com.farmo.network.Dashboard;

import com.google.gson.annotations.SerializedName;

public class DashboardService {
    public static class DashboardRequest{

    }

    public static class DashboardResponse {
        @SerializedName("username")
        private String Username;

        @SerializedName("wallet_balance")
        private String wallet_amt;

        @SerializedName("today_expense")
        private String today_expense;

        @SerializedName("rate")
        private String rating;

        @SerializedName("error")
        private String error;


        // --- Getters ---
        public String getUsername() { return Username; }
        public String getWallet_amt() { return wallet_amt; }
        public String getTodayExpense() { return today_expense; }

        public String getRating() { return rating; }

        public String getError() { return error; }

    }
}