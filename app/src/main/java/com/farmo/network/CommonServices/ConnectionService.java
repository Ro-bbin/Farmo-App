package com.farmo.network.CommonServices;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConnectionService {

    // Response class
    public static class ConnectionsResponse {

        @SerializedName("total_count")
        private int totalCount;

        @SerializedName("page")
        private int page;

        @SerializedName("has_next")
        private boolean hasNext;

        @SerializedName("results")
        private List<UserConnection> results;

        // Getters
        public int getTotalCount() { return totalCount; }
        public int getPage() { return page; }
        public boolean isHasNext() { return hasNext; }
        public List<UserConnection> getResults() { return results; }

    }

    // Inner class for each user connection
    public static class UserConnection {
        @SerializedName("user_id")
        private String userId;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("profile_pic")
        private String profilePic;

        @SerializedName("status")
        private String status;

        // Getters
        public String getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getProfilePic() { return profilePic; }
        public String getStatus() { return status; }
    }

    // Request class
    public static class ConnectionsRequest {

        @SerializedName("type")
        private String type;

        @SerializedName("page")
        private int page;

        @SerializedName("page_size")
        private int pageSize;

        public ConnectionsRequest(String type, int page, int pageSize) {
            this.type = type;
            this.page = page;
            this.pageSize = pageSize;
        }

        // Getters
        public String getType() { return type; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
    }

}

