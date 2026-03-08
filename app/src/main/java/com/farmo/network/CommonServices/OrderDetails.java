package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrderDetails {

    public static class Request{
        @SerializedName("order_id")
        private String orderId;

        public Request(String orderId) {
            this.orderId = orderId;
        }
    }
    public static class Response {

        @SerializedName("order_id")
        private String orderId;

        @SerializedName("ordered_date")
        private String orderedDate;

        @SerializedName("total_cost")
        private String totalCost;

        @SerializedName("order_status")
        private String orderStatus;

        @SerializedName("shipping_address")
        private String shippingAddress;

        @SerializedName("expected_delivery_date")
        private String expectedDeliveryDate;

        @SerializedName("message")
        private List<MessageItem> message;

        @SerializedName("ORDER_OTP")
        private String orderOtp;

        @SerializedName("ordered_quantity")
        private String orderedQuantity;

        @SerializedName("latest_update")
        private String latestUpdate;

        @SerializedName("payment_method")
        private String paymentMethod;

        @SerializedName("product")
        private String product;

        @SerializedName("consumer_id")
        private String consumerId;

        @SerializedName("quantity_unit")
        private String quantityUnit;

        @SerializedName("cost_per_unit")
        private double costPerUnit;

        @SerializedName("transaction_status")
        private String transactionStatus;



        // Getters
        public String getOrderId() { return orderId; }
        public String getOrderedDate() { return orderedDate; }
        public String getTotalCost() { return totalCost; }
        public String getOrderStatus() { return orderStatus; }
        public String getShippingAddress() { return shippingAddress; }
        public String getExpectedDeliveryDate() { return expectedDeliveryDate; }
        public List<MessageItem> getMessage() { return message; }
        public String getOrderOtp() { return orderOtp; }
        public String getOrderedQuantity() { return orderedQuantity; }
        public String getLatestUpdate() { return latestUpdate; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getProduct() { return product; }
        public String getConsumerId() { return consumerId; }
        public String getQuantityUnit() { return quantityUnit; }
        public double getCostPerUnit() { return costPerUnit; }

        public String getTransactionStatus() {
            return transactionStatus;
        }

    }
    // Nested MessageItem class
    public static class MessageItem {

        @SerializedName("by")
        private String by;

        @SerializedName("message")
        private String message;

        @SerializedName("date-time")
        private String dateTime;

        // Getters
        public String getBy() { return by; }
        public String getMessage() { return message; }
        public String getDateTime() { return dateTime; }
    }
}
