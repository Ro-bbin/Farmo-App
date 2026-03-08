package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

public class OrderRequestService {
    public static class OrderRequest{
        @SerializedName("expected_delivery_within")
        private int expected_delivery_within;

        @SerializedName("shipping_address")
        private String shipping_address;

        @SerializedName("total_cost")
        private String total_cost;

        @SerializedName("product_id")
        private String product_id;

        @SerializedName("quantity")
        private Double quantity;

        @SerializedName("message")
        private String message;

        @SerializedName("payment")
        private String payment;

        @SerializedName("discount_type")
        private String discount_type;

        @SerializedName("discount")
        private String discount;

        public OrderRequest(int expected_delivery_within, String total_cost, String product_id, Double quantity, String message, String payment, String province, String district, String municipal, String ward, String tole, String discountType, String discount) {
            this.expected_delivery_within = expected_delivery_within;
            this.shipping_address = municipal + "-" + ward + ", "+ tole + ", " + district + ", " + province;
            this.total_cost = total_cost;
            this.product_id = product_id;
            this.quantity = quantity;
            this.message = message;
            this.payment = payment;
            this.discount_type = discountType;
            this.discount = discount;
        }
    }

    public static class OrderRequestResponse{
        @SerializedName("order_id")
        private String order_id;

        @SerializedName("otp")
        private String otp;

        @SerializedName("ordered_date")
        private String ordered_date;

        public String getOrdered_date() {
            return ordered_date;
        }

        public String getOrder_id() {
            return order_id;
        }

        public String getOtp() {
            return otp;
        }
    }
}
