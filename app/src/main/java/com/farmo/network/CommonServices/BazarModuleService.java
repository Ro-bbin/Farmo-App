package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
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
        @SerializedName("serial_no")
        private int serial_no;

        public BazarRequest(int page, String filter, String searchTerm, int serial_no) {
            this.page = page;
            this.filter = filter;
            this.searchTerm = searchTerm;
            this.serial_no = serial_no;
        }
    }

    public static class BazarResponse {
        @SerializedName("page")
        private int page;
        @SerializedName("total_pages")
        private int totalPages;
        @SerializedName("has_more")
        private boolean hasMore;
        @SerializedName("filter")
        private String filter;
        
        @SerializedName("products")
        private List<BazarProduct> products;

        @SerializedName("product")
        private BazarProduct product;

        public List<BazarProduct> getProducts() {
            if (products != null && !products.isEmpty()) return products;
            if (product != null) {
                List<BazarProduct> list = new ArrayList<>();
                list.add(product);
                return list;
            }
            return null;
        }

        public boolean hasNextPage() { return hasMore; }
        public int getPage() { return page; }
    }

    public static class BazarProduct {
        @SerializedName("id")
        private String id;

        @SerializedName("p_id")
        private String pId;

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

        @SerializedName("discount")
        private String discount;

        @SerializedName("discount_type")
        private String discountType;

        @SerializedName("is_organic")
        private boolean isOrganic;

        public String getId() { 
            if (id != null && !id.isEmpty()) return id;
            return pId;
        }

        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getPriceUnit() { return priceUnit; }
        public String getStock() { return stock; }
        public String getStockUnit() { return stockUnit; }
        public float getRating() { return rating; }
        public String getOriginalPrice() { return originalPrice; }
        public String getDiscountAmount() { return discount; }
        public String getDiscountType() { return discountType; }
        public boolean isOrganic() { return isOrganic; }

        public String getImageUrl() {
            if (image instanceof String) return (String) image;
            if (image instanceof Map) {
                Map<?, ?> imgMap = (Map<?, ?>) image;
                if (imgMap.containsKey("media_url")) {
                    Object url = imgMap.get("media_url");
                    return url != null ? url.toString() : "";
                }
            }
            return "";
        }

        public int getImageSerialNo() {
            if (image instanceof Map) {
                Map<?, ?> imgMap = (Map<?, ?>) image;
                Object seq = imgMap.get("serial_no");
                if (seq instanceof Number) return ((Number) seq).intValue();
                if (seq instanceof String) {
                    try { return Integer.parseInt((String) seq); } catch (Exception ignored) {}
                }
            }
            return 1;
        }
    }
}
