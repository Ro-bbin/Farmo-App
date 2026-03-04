package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

public class MyProductService {
    @SerializedName("id")
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("category")
    public String category;
    
    @SerializedName("status")
    public String status;
    
    @SerializedName("image")
    public String image;
    
    @SerializedName("price")
    public String price;
    
    @SerializedName("priceUnit")
    public String priceUnit;
    
    @SerializedName("stock")
    public String stock;
    
    @SerializedName("stockUnit")
    public String stockUnit;
    
    public boolean isActive;

    public MyProductService() {}
}
