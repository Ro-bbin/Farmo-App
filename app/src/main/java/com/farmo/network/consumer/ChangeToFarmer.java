package com.farmo.network.consumer;

import com.google.gson.annotations.SerializedName;

public class ChangeToFarmer {
    public static class Request{
        @SerializedName("password")
        private String password;

        public Request(String password){
            this.password = password;
        }
    }
}
