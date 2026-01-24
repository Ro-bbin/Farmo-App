package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("f_name")
    private String fName;
    
    @SerializedName("m_name")
    private String mName;
    
    @SerializedName("l_name")
    private String lName;
    
    @SerializedName("sex")
    private String sex;
    
    @SerializedName("dob")
    private String dob;
    
    @SerializedName("user_type")
    private String userType;
    
    @SerializedName("province")
    private String province;
    
    @SerializedName("district")
    private String district;
    
    @SerializedName("municipal")
    private String municipal;
    
    @SerializedName("ward")
    private String ward;
    
    @SerializedName("tole")
    private String tole;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("phone2")
    private String phone2;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("facebook")
    private String facebook;
    
    @SerializedName("whatsapp")
    private String whatsapp;
    
    @SerializedName("about")
    private String about;
    
    @SerializedName("created_by")
    private String createdBy;

    public RegisterRequest(String userId, String password, String fName, String mName, String lName, 
                           String sex, String dob, String userType, String province, String district, 
                           String municipal, String ward, String tole, String phone, String phone2, 
                           String email, String facebook, String whatsapp, String about, String createdBy) {
        this.userId = userId;
        this.password = password;
        this.fName = fName;
        this.mName = mName;
        this.lName = lName;
        this.sex = sex;
        this.dob = dob;
        this.userType = userType;
        this.province = province;
        this.district = district;
        this.municipal = municipal;
        this.ward = ward;
        this.tole = tole;
        this.phone = phone;
        this.phone2 = phone2;
        this.email = email;
        this.facebook = facebook;
        this.whatsapp = whatsapp;
        this.about = about;
        this.createdBy = createdBy;
    }
}
