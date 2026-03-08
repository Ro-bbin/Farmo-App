package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

public class WalletService {

    public static class walletPageResponse{
        @SerializedName("balance")
        private double balance;

       @SerializedName("today_income")
        private double todayIncome;

        @SerializedName("today_expense")
        private double todayExpense;

        public double getBalance() {
            return balance;
        }

        public double getTodayIncome() {
            return todayIncome;
        }

        public double getTodayExpense() {
            return todayExpense;
        }
    }

    public static class ChangePinRequest{
        @SerializedName("old_pin")
        private String oldPin;

        @SerializedName("new_pin")
        private String newPin;

        @SerializedName("password")
        private String password;


        public void setOldPin(String oldPin) {
            this.oldPin = oldPin;
        }

        public void setNewPin(String newPin) {
            this.newPin = newPin;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class setup_pinReq{
        @SerializedName("pin")
        private String pin;

        public setup_pinReq(String pin){
            this.pin = pin;
        }
    }

    /// ///////////////////////////////////////////////////////////////////////
    public static class add_withdrawRequest {
        @SerializedName("amount")
        private double amount;

        @SerializedName("action")
        private String action;

        @SerializedName("pin")
        private String pin;

        public add_withdrawRequest(double amount, String action, String pin) {
            this.amount = amount;
            this.action = action;
            this.pin = pin;
        }
    }
}
