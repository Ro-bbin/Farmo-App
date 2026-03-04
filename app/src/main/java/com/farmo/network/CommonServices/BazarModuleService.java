package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class BazarModuleService {

    public static class BazarRequest {
        @SerializedName("page")
        private int page;
        @SerializedName("filter")
        private String filter;
        @SerializedName("search_term")
        private String searchTerm;

        public BazarRequest(int page, String filter, String searchTerm) {
            this.page = page;
            this.filter = filter;
            this.searchTerm = searchTerm;
        }
    }

    public static class BazarResponse {
        @SerializedName("page")
        private int page;
        @SerializedName("total_pages")
        private int totalPages;
        @SerializedName("next_page")
        private boolean nextPage;
        @SerializedName("filter")
        private String filter;
        @SerializedName("products")
        private List<BazarProduct> products;

        public List<BazarProduct> getProducts() { return products; }
        public boolean hasNextPage() { return nextPage; }
        public int getPage() { return page; }
    }

    public static class BazarProduct {
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private String name;
        @SerializedName("product_type")
        private String productType;
        @SerializedName("status")
        private String status;
        @SerializedName("price")
        private String price;
        @SerializedName("priceUnit")
        private String priceUnit;
        @SerializedName("stock")
        private String stock;
        @SerializedName("stockUnit")
        private String stockUnit;
        @SerializedName("image")
        private Object image;

        @SerializedName("rating")
        private float rating;

        @SerializedName("original_price")
        private String originalPrice;

        @SerializedName("discount_amount")
        private String discountAmount;

        @SerializedName("discount_type")
        private String discountType;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getPriceUnit() { return priceUnit; }
        public String getStock() { return stock; }
        public String getStockUnit() { return stockUnit; }
        public float getRating() { return rating; }
        public String getOriginalPrice() { return originalPrice; }
        public String getDiscountAmount() { return discountAmount; }
        public String getDiscountType() { return discountType; }

        public String getImageUrl() {
            if (image instanceof String) {
                return (String) image;
            }
            if (image instanceof Map) {
                Map<?, ?> imgMap = (Map<?, ?>) image;
                if (imgMap.containsKey("media_url")) {
                    Object url = imgMap.get("media_url");
                    return url != null ? url.toString() : "";
                }
            }
            return "";
        }
    }
}
