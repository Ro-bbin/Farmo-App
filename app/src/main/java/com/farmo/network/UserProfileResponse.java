package com.example.farmo_app.network;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("f_name")
    private String firstName;
    @SerializedName("m_name")
    private String middleName;
    @SerializedName("l_name")
    private String lastName;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("phone")
    private String phone;
    @SerializedName("email")
    private String email;
    @SerializedName("user_type")
    private String userType;
    @SerializedName("sex")
    private String sex;
    @SerializedName("dob")
    private String dob;
    @SerializedName("province")
    private String province;
    @SerializedName("district")
    private String district;
    @SerializedName("municipal")
    private String municipality;
    @SerializedName("ward")
    private String ward;
    @SerializedName("tole")
    private String tole;

    // Getters
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getUserId() { return userId; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public String getSex() { return sex; }
    public String getDob() { return dob; }
    public String getProvince() { return province; }
    public String getDistrict() { return district; }
    public String getMunicipality() { return municipality; }
    public String getWard() { return ward; }
    public String getTole() { return tole; }
    
    public String getFullName() {
        return firstName + (middleName != null && !middleName.isEmpty() ? " " + middleName : "") + " " + lastName;
    }
}
