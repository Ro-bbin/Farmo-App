package com.farmo.network.Dashboard;

import com.google.gson.annotations.SerializedName;
public class RefreshWallet {

    public static class refreshWalletResponse{
        @SerializedName("balance")
        private String balance;

        @SerializedName("today_income")
        private String todaysIncome;

        @SerializedName("error")
        private String error;

        @SerializedName("today_expense")
        private String todayExpense;;

        // Getters
        public String getBalance() {
            return balance;
        }

        public String getTodaysIncome() {
            return todaysIncome;
        }

        public String getTodayExpense() {
            return todayExpense;
        }

            public String getError () {
                return error;
            }

    }
}
