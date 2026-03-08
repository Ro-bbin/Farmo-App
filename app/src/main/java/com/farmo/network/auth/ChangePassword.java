package com.farmo.network.auth;

import com.google.gson.annotations.SerializedName;

public class ChangePassword {
    public static class Request{
        @SerializedName("current_password")
        private String current_password;

        @SerializedName("new_password")
        private String new_password;

        public Request(String current_password, String new_password) {
            this.current_password = current_password;
            this.new_password = new_password;
        }
    }
}
