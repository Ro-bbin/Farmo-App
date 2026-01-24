package com.farmo.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("login/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("login-with-token/")
    Call<LoginResponse> loginWithToken(@Body TokenLoginRequest tokenLoginRequest);

    @POST("register/")
    Call<MessageResponse> register(@Body RegisterRequest registerRequest);

    @POST("forgot-password/")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest forgotPasswordRequest);

    @POST("forgot-password-verify-email/")
    Call<MessageResponse> verifyEmail(@Body VerifyEmailRequest verifyEmailRequest);

    @POST("forgot-password-verify-otp/")
    Call<MessageResponse> verifyOtp(@Body VerifyOtpRequest verifyOtpRequest);

    @POST("forgot-password-change-password/")
    Call<MessageResponse> changePassword(@Body ChangePasswordRequest changePasswordRequest);

    @POST("login-change-password/")
    Call<MessageResponse> activateAccount(@Body ActivateAccountRequest activateAccountRequest);

    @GET("get-profile/")
    Call<UserProfileResponse> getUserProfile(@Query("user_id") String userId);

    @GET("get-dashboard/")
    Call<DashboardResponse> getDashboard(@Query("user_id") String userId);
}
