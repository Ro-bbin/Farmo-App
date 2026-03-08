package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

public class UpdateProfile {
    public static class Request{
        @SerializedName("f_name")
        private String fName;

        @SerializedName("m_name")
        private String mName;

        @SerializedName("l_name")
        private String lName;

        @SerializedName("phone")
        private String phone;

        @SerializedName("phone2")
        private String phone2;

        @SerializedName("facebook")
        private String facebook;

        @SerializedName("whatsapp")
        private String whatsapp;

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

        @SerializedName("about")
        private String about;

        @SerializedName("dob")
        private String dob;

        @SerializedName("sex")
        private String sex;

        // Constructor
        public Request(String fName, String mName, String lName, String phone, String phone2,
                           String facebook, String whatsapp, String province, String district,
                           String municipal, String ward, String tole, String about,
                           String dob, String sex) {
            this.fName = fName;
            this.mName = mName;
            this.lName = lName;
            this.phone = phone;
            this.phone2 = phone2;
            this.facebook = facebook;
            this.whatsapp = whatsapp;
            this.province = province;
            this.district = district;
            this.municipal = municipal;
            this.ward = ward;
            this.tole = tole;
            this.about = about;
            this.dob = dob;
            this.sex = sex;
        }

    }
}
