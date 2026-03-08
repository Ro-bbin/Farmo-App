package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TransactionService {
    public static class RecentTransResponse {

        @SerializedName("transaction")
        private List<RecentTransaction> transactions;

        public List<RecentTransaction> getTransactions() {
            return transactions;
        }

    }
    public static class RecentTransaction {
        @SerializedName("transaction_id")
        private String transactionId;

        @SerializedName("amount")
        private String amount;

        @SerializedName("type")
        private String type; // "CR" or "DR"

        @SerializedName("other_party_id")
        private String otherPartyId;

        @SerializedName("other_party_name")
        private String otherPartyName;

        @SerializedName("date")
        private String date;

        @SerializedName("status")
        private String status;

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getAmount() { return amount; }
        public String getType() { return type; }
        public String getOtherPartyId() { return otherPartyId; }
        public String getOtherPartyName() { return otherPartyName; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }

    public static class TransactionHistoryRequest {

        @SerializedName("type")
        private String type; // "CR" or "DR" or "All"

        @SerializedName("date_to")
        private String dateTo; // "dd-mm-yyyy"

        @SerializedName("date_from")
        private String dateFrom; // "dd-mm-yyyy"

        @SerializedName("page")
        private int page;

        public TransactionHistoryRequest(String type, String dateTo, String dateFrom, int page) {
            this.type = type;
            this.dateTo = dateTo;
            this.dateFrom = dateFrom;
            this.page = page;
        }
    }

    public static class TransactionHistoryResponse{
        @SerializedName("page")
        private int page;

        @SerializedName("total_items")
        private int totalItems;

        @SerializedName("total_pages")
        private int totalPages;

        @SerializedName("data")
        private List<data> data;

        public int getPage() {
            return page;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public List<data> getData() {
            return data;
        }
    }

    public static class data{
        @SerializedName("date")
        private String date;

        @SerializedName("monthYear")
        private String monthYear;

        @SerializedName("closingBalance")
        private String closingBalance;

        @SerializedName("transactions")
        private List<Transaction> transactions;

        // Getters
        public String getDate() { return date; }
        public String getMonthYear() { return monthYear; }
        public String getClosingBalance() { return closingBalance; }
        public List<Transaction> getTransactions() { return transactions; }

    }

    public static class Transaction {

        @SerializedName("transaction_id")
        private String transactionId;

        @SerializedName("amount")
        private String amount;

        @SerializedName("type")
        private String type; // debit/credit

        @SerializedName("for_product")
        private String forProduct;

        @SerializedName("user_id")
        private String userId;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("timestamp")
        private String timestamp;

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getAmount() { return amount; }
        public String getType() { return type; }
        public String getForProduct() { return forProduct; }
        public String getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getTimestamp() { return timestamp; }
    }


}
