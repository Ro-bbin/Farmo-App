package com.farmo.network.farmer;

import com.farmo.network.CommonServices.MyProductService;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MyProductsResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("products")
    private List<MyProductService> products;

    @SerializedName("error")
    private String error;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String       getStatus()   { return status; }
    public List<MyProductService> getProducts() { return products; }
    public String       getError()    { return error; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setStatus(String status)             { this.status = status; }
    public void setProducts(List<MyProductService> products)  { this.products = products; }
    public void setError(String error)               { this.error = error; }
}
