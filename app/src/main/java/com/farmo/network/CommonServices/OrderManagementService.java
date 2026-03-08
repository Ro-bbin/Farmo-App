package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderManagementService {

    public static class Request {
        @SerializedName("type")
        public String type;

        @SerializedName("status")
        public String status;

        @SerializedName("date_from")
        public String dateFrom;

        @SerializedName("date_to")
        public String dateTo;

        @SerializedName("page")
        public int page;

        public Request(int page, String dateTo, String dateFrom, String status, String type) {
            this.page = page;
            this.dateTo = dateTo;
            this.dateFrom = dateFrom;
            this.status = status;
            this.type = type;
        }
    }

    public static class Response {
        @SerializedName("orders")
        public List<Order> orders;

        @SerializedName("total")
        public int total;

        @SerializedName("total_pages")
        public int totalPages;

        @SerializedName("current_page")
        public int currentPage;

        @SerializedName("has_next")
        public boolean hasNext;

        @SerializedName("has_previous")
        public boolean hasPrevious;
    }

    public static class Order {
        @SerializedName("order_id")
        public String orderId;

        @SerializedName("product_id")
        public String productId;

        @SerializedName("product_name")
        public String productName;

        @SerializedName("cost")
        public String cost;

        @SerializedName("order_date")
        public String orderDate;

        @SerializedName("latest_update")
        public String latestUpdate;

        @SerializedName("foreign_user_id")
        public String foreignUserId;

        @SerializedName("foreign_user_name")
        public String foreignUserName;

        @SerializedName("type")
        public String type;

        @SerializedName("status")
        public String status;
    }

    // New POJOs for status updates
    public static class UpdateStatusRequest {
        @SerializedName("order_id")
        private String orderid;

        @SerializedName("otp")
        private String otp;

        @SerializedName("status")
        private String status;

        @SerializedName("message")
        private String message;

        public UpdateStatusRequest(String orderid, String otp, String status, String message) {
            this.orderid = orderid;
            this.otp = otp;
            this.status = status;
            this.message = message;
        }
    }

    public static class ConfirmOrderRequest {
        @SerializedName("order_id")
        private String orderid;

        @SerializedName("otp")
        private String otp;

        public ConfirmOrderRequest(String orderid, String otp) {
            this.orderid = orderid;
            this.otp = otp;
        }
    }

}
