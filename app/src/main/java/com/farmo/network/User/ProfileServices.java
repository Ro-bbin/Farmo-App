package com.farmo.network.User;

import com.google.gson.annotations.SerializedName;

public class ProfileServices {

    public static class ProfileResponse {
        @SerializedName("user_id")
        private String userId;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("address")
        private String address;

        @SerializedName("phone")
        private String phone;

        @SerializedName("phone2")
        private String phone2;

        @SerializedName("user_type")
        private String userType;

        @SerializedName("email")
        private String email;

        @SerializedName("facebook")
        private String facebook;

        @SerializedName("whatsapp")
        private String whatsapp;

        @SerializedName("join_date")
        private String joinDate;

        @SerializedName("about")
        private String about;

        @SerializedName("dob")
        private String dob;

        @SerializedName("sex")
        private String sex;

        @SerializedName("profile_picture_otp")
        private String profilePictureOtp;

        @SerializedName("profile_picture")
        private String profilePicture;

        // Getters
        public String getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }

        public String getPhone2() {
            return phone2;
        }

        public String getUserType() {
            return userType;
        }

        public String getEmail() {
            return email;
        }

        public String getFacebook() {
            return facebook;
        }

        public String getWhatsapp() {
            return whatsapp;
        }

        public String getJoinDate() {
            return joinDate;
        }

        public String getAbout() {
            return about;
        }

        public String getDob() {
            return dob;
        }

        public String getSex() {
            return sex;
        }

        public String getProfilePictureOtp() {
            return profilePictureOtp;
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        // Setters (optional, if needed)
        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public void setPhone2(String phone2) {
            this.phone2 = phone2;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setFacebook(String facebook) {
            this.facebook = facebook;
        }

        public void setWhatsapp(String whatsapp) {
            this.whatsapp = whatsapp;
        }

        public void setJoinDate(String joinDate) {
            this.joinDate = joinDate;
        }

        public void setAbout(String about) {
            this.about = about;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public void setProfilePictureOtp(String profilePictureOtp) {
            this.profilePictureOtp = profilePictureOtp;
        }

        public void setProfilePicture(String profilePicture) {
            this.profilePicture = profilePicture;
        }
    }

    public static class FileDownloadResponse {
        @SerializedName("file")
        private String file;

        @SerializedName("mime_type")
        private String mimeType;

        @SerializedName("media_type")
        private String mediaType;

        @SerializedName("total")
        private int total;

        @SerializedName("seq")
        private int seq;

        // Getters
        public String getFile() {
            return file;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getMediaType() {
            return mediaType;
        }

        public int getTotal() {
            return total;
        }

        public int getSeq() {
            return seq;
        }
    }
}
