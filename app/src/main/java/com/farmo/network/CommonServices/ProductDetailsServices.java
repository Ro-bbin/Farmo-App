package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

public class ProductDetailsServices {

    // ✅ Request class
    public static class Request {
        @SerializedName("p_id")
        private String pId;

        public Request(String pId) {
            this.pId = pId;
        }

        public String getPId() {
            return pId;
        }
    }

    // ✅ Response class
    public static class Response {
        @SerializedName("p_id")
        private String pId;

        @SerializedName("user_id")
        private String userId;

        @SerializedName("name")
        private String name;

        @SerializedName("product_type")
        private String productType;

        @SerializedName("keywords")
        private String[] keywords;

        @SerializedName("is_organic")
        private boolean isOrganic;

        @SerializedName("product_status")
        private String productStatus;

        @SerializedName("Cost_per_unit")
        private String costPerUnit;

        @SerializedName("Unit")
        private String unit;

        @SerializedName("in_Stock")
        private boolean inStock;

        @SerializedName("discount_type")
        private String discountType;

        @SerializedName("discount_value")
        private String discountValue;

        @SerializedName("description")
        private String description;

        @SerializedName("registered_date")
        private String registeredDate;

        @SerializedName("produced_date")
        private String producedDate;

        @SerializedName("expiry_date")
        private String expiryDate;

        @SerializedName("rating")
        private int rating;

        @SerializedName("rating_count")
        private int ratingCount;

        @SerializedName("sold_count")
        private String soldCount;

        @SerializedName("farmer_name")
        private String farmerName;

        @SerializedName("farmer_location")
        private String farmerLocation;

        @SerializedName("no_of_media")
        private int noOfMedia;

        // Getters
        public String getPId() { return pId; }
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getProductType() { return productType; }
        public String[] getKeywords() { return keywords; }
        public boolean isOrganic() { return isOrganic; }
        public String getProductStatus() { return productStatus; }
        public String getCostPerUnit() { return costPerUnit; }
        public String getUnit() { return unit; }
        public boolean isInStock() { return inStock; }
        public String getDiscountType() { return discountType; }
        public String getDiscountValue() { return discountValue; }
        public String getDescription() { return description; }
        public String getRegisteredDate() { return registeredDate; }
        public String getProducedDate() { return producedDate; }
        public String getExpiryDate() { return expiryDate; }
        public int getRating() { return rating; }
        public int getRatingCount() { return ratingCount; }
        public String getSoldCount() { return soldCount; }
        public String getFarmerName() { return farmerName; }
        public String getFarmerLocation() { return farmerLocation; }
        public int getNoOfMedia() { return noOfMedia; }
    }
}