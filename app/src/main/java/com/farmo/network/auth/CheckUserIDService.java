package com.farmo.network.auth;

public class CheckUserIDService {
    public static class Request {
        private String user_id;

        public Request(String user_id){
            this.user_id = user_id;
        }
    }
    public static class Response{
        private int status;

        public int getStatus(){
            return status;
        }
    }
}
