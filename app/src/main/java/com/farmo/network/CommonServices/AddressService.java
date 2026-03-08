package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

import kotlin.jvm.internal.SerializedIr;

public class AddressService {
    public static class AddressResponse{
        @SerializedName("province")
        private String province;
        @SerializedName("district")
        private String district;
        @SerializedName("ward")
        private String ward;

        @SerializedName("municipal")
        private String municipal;

        @SerializedName("tole")
        private String tole;

        public String getProvince() {
            return province;
        }

        public String getDistrict() {
            return district;
        }

        public String getWard() {
            return ward;
        }

        public String getTole() {
            return tole;
        }

        public String getMunicipal() {
            return municipal;
        }
    }
}
