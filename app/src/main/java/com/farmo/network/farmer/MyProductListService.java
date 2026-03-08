package com.farmo.network.farmer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MyProductListService {

    public static class Request {

        @SerializedName("filter")
        private String filter;

        @SerializedName("page")
        private int page;

        @SerializedName("search")
        private String search;

        @SerializedName("date_from")
        private String dateFrom;

        @SerializedName("date_to")
        private String dateTo;

        @SerializedName("sort_by")
        private String sortBy;

        public Request(String filter, int page, String search, String dateFrom, String dateTo, String sortBy) {
            this.filter = filter;
            this.page = page;
            this.search = search;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.sortBy = sortBy;
        }

        // Getters
        public String getFilter() { return filter; }
        public int getPage() { return page; }
        public String getSearch() { return search; }
        public String getDateFrom() { return dateFrom; }
        public String getDateTo() { return dateTo; }
        public String getSortBy() { return sortBy; }
    }


    public static class Response {

        @SerializedName("total_pages")
        private int totalPages;

        @SerializedName("total_products")
        private int totalProducts;

        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("has_next")
        private boolean hasNext;

        @SerializedName("has_previous")
        private boolean hasPrevious;

        @SerializedName("products")
        private List<Product> products;

        @SerializedName("error")
        private String error;

        // Getters
        public int getTotalPages() { return totalPages; }
        public int getTotalProducts() { return totalProducts; }
        public int getCurrentPage() { return currentPage; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public List<Product> getProducts() { return products; }
        public String getError() { return error; }


        public static class Product {

            @SerializedName("p_id")
            private String pId;

            @SerializedName("name")
            private String name;

            @SerializedName("product_type")
            private String productType;

            @SerializedName("cost_per_unit")
            private String costPerUnit;

            @SerializedName("quantity_available")
            private String quantityAvailable;

            @SerializedName("product_status")
            private String productStatus;

            @SerializedName("registered_at")
            private String registeredAt;

            @SerializedName("rating")
            private float rating;

            @SerializedName("sold_count")
            private int soldCount;

            // Getters
            public String getPId() { return pId; }
            public String getName() { return name; }
            public String getProductType() { return productType; }
            public String getCostPerUnit() { return costPerUnit; }
            public String getQuantityAvailable() { return quantityAvailable; }
            public String getProductStatus() { return productStatus; }
            public String getRegisteredAt() { return registeredAt; }
            public float getRating() { return rating; }
            public int getSoldCount() { return soldCount; }
        }
    }
}
