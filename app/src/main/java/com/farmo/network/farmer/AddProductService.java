package com.farmo.network.farmer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AddProductService {
    public static class AddProductRequest {
        @SerializedName("product_name")
        public String product_name;
        @SerializedName("product_type")
        public String product_type;
        @SerializedName("unit")
        public String unit;
        @SerializedName("quantity")
        public double quantity;
        @SerializedName("is_organic")
        public boolean is_organic;
        @SerializedName("cost_per_unit")
        public double cost_per_unit;
        @SerializedName("produced_date")
        public String produced_date;
        @SerializedName("expiry_date")
        public String expiry_date;
        @SerializedName("description")
        public String description;
        @SerializedName("discount_type")
        public String discount_type;
        @SerializedName("discount")
        public int discount;
        @SerializedName("delivery_options")
        public String delivery_options;
        @SerializedName("keywords")
        public List<String> keywords;

        public AddProductRequest() {}
    }

    public static class AddProductResponse {
        public String message;
        public String product_id;
        public String note;
        public UploadInstructions upload_instructions;

        public static class UploadInstructions {
            public String subject;
            public String product_id;
            public String file_purpose;
        }
    }

    public static class ProductType {
        @SerializedName(value = "key", alternate = {"id", "value"})
        private String key;

        @SerializedName(value = "label", alternate = {"name", "title"})
        private String label;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class ProductTypeRequest{
        @SerializedName("search")
        public String search;
    }

    public static class ProductTypeResponse {
        @SerializedName(value = "categories", alternate = {"product_types", "results", "data"})
        private List<ProductType> productTypes;

        public List<ProductType> getCategories() { return productTypes; }
        public void setCategories(List<ProductType> productTypes) { this.productTypes = productTypes; }
    }

    public static class FarmProduct {
        @SerializedName(value = "id", alternate = {"pk"})
        private int id;
        @SerializedName(value = "english_name", alternate = {"name", "label"})
        private String english_name;
        @SerializedName("nepali_name")
        private String nepali_name;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getEnglish_name() { return english_name; }
        public void setEnglish_name(String english_name) { this.english_name = english_name; }
        public String getNepali_name() { return nepali_name; }
        public void setNepali_name(String nepali_name) { this.nepali_name = nepali_name; }
    }

    public static class KeywordResponse {
        private String category;
        @SerializedName(value = "farm_products", alternate = {"products", "results", "keywords"})
        private List<FarmProduct> farm_products;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<FarmProduct> getFarm_products() { return farm_products; }
        public void setFarm_products(List<FarmProduct> farm_products) { this.farm_products = farm_products; }
    }

    public static class keywordRequest{
        @SerializedName("category")
        public String category;
        @SerializedName("keyword")
        public String keyword;
    }
}
