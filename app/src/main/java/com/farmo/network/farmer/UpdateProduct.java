package com.farmo.network.farmer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UpdateProduct {

    public static class Request {
        @SerializedName("p_id")
        private String p_id;

        @SerializedName("name")
        private String name;

        @SerializedName("product_type")
        private String product_type;

        @SerializedName("is_organic")
        private Boolean isOrganic;

        @SerializedName("quantity_available")
        private Integer quantityAvailable;

        @SerializedName("cost_per_unit")
        private Double costPerUnit;

        @SerializedName("discount_type")
        private String discountType;

        @SerializedName("discount")
        private Double discount;

        @SerializedName("description")
        private String description;

        @SerializedName("delivery_option")
        private String deliveryOption;

        @SerializedName("keywords")
        public List<String> keywords;

        @SerializedName("expiry_Date")
        private String expiryDate;

        public Request() {
        }

        public Request(String p_id, String name, String product_type, Boolean isOrganic, Integer quantityAvailable, Double costPerUnit, String discountType, Double discount, String description, String deliveryOption, List<String> keywords, String expiryDate) {
            this.p_id = p_id;
            this.name = name;
            this.product_type = product_type;
            this.isOrganic = isOrganic;
            this.quantityAvailable = quantityAvailable;
            this.costPerUnit = costPerUnit;
            this.discountType = discountType;
            this.discount = discount;
            this.description = description;
            this.deliveryOption = deliveryOption;
            this.keywords = keywords;
            this.expiryDate = expiryDate;
        }

        public String getP_id() {
            return p_id;
        }

        public void setP_id(String p_id) {
            this.p_id = p_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProduct_type() {
            return product_type;
        }

        public void setProduct_type(String product_type) {
            this.product_type = product_type;
        }

        public Boolean getOrganic() {
            return isOrganic;
        }

        public void setOrganic(Boolean organic) {
            this.isOrganic = organic;
        }

        public Integer getQuantityAvailable() {
            return quantityAvailable;
        }

        public void setQuantityAvailable(Integer quantityAvailable) {
            this.quantityAvailable = quantityAvailable;
        }

        public Double getCostPerUnit() {
            return costPerUnit;
        }

        public void setCostPerUnit(Double costPerUnit) {
            this.costPerUnit = costPerUnit;
        }

        public String getDiscountType() {
            return discountType;
        }

        public void setDiscountType(String discountType) {
            this.discountType = discountType;
        }

        public Double getDiscount() {
            return discount;
        }

        public void setDiscount(Double discount) {
            this.discount = discount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDeliveryOption() {
            return deliveryOption;
        }

        public void setDeliveryOption(String deliveryOption) {
            this.deliveryOption = deliveryOption;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }
    }

    public static class Response {
        @SerializedName("message")
        private String message;

        public String getMessage() {
            return message;
        }

        @SerializedName("product_id")
        private String productId;

        public String getProductId() {
            return productId;
        }
    }

    public static class ToggleAvailabilityRequest {
        @SerializedName("p_id")
        private String p_id;

        @SerializedName("action")
        private String action;

        public ToggleAvailabilityRequest() {
        }

        public ToggleAvailabilityRequest(String p_id, String action) {
            this.p_id = p_id;
            this.action = action;
        }

        public String getP_id() {
            return p_id;
        }

        public void setP_id(String p_id) {
            this.p_id = p_id;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
