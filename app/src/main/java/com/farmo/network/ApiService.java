package com.farmo.network;

import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.auth.ForgotPasswordChangePasswordRequest;
import com.farmo.network.auth.ForgotPasswordRequest;
import com.farmo.network.auth.ForgotPasswordResponse;
import com.farmo.network.auth.LoginRequest;
import com.farmo.network.auth.LoginResponse;
import com.farmo.network.auth.RegisterRequest;
import com.farmo.network.auth.TokenLoginRequest;
import com.farmo.network.auth.VerifyEmailRequest;
import com.farmo.network.auth.VerifyOtpRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/auth/login/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/login-with-token/")
    Call<LoginResponse> loginWithToken(@Body TokenLoginRequest tokenLoginRequest);

    @POST("api/auth/register/")
    @Headers("Content-Type: multipart/form-data")
    Call<MessageResponse> register(@Body RegisterRequest registerRequest);

    @POST("api/auth/forgot-password/")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest forgotPasswordRequest);

    @POST("api/auth/forgot-password-verify-email/")
    Call<MessageResponse> verifyEmail(@Body VerifyEmailRequest verifyEmailRequest);

    @POST("api/auth/forgot-password-verify-otp/")
    Call<MessageResponse> verifyOtp(@Body VerifyOtpRequest verifyOtpRequest);

    @POST("api/auth/forgot-password-change-password/")
    Call<MessageResponse> changePassword(@Body ForgotPasswordChangePasswordRequest changePasswordRequest);

    @POST("api/auth/login-change-password/")
    Call<MessageResponse> activateAccount(@Body ForgotPasswordChangePasswordRequest.ActivateAccountRequest activateAccountRequest);

    @POST("api/auth/logout/")
    @Headers("Content-Type: application/json")
    Call<Void> logout(
            @Header("token") String token,
            @Header("user-id") String userId);

    @POST("api/user/view-profile/")
    @Headers("Content-Type: application/json")
    Call<ProfileServices.ProfileResponse> getProfileData(
            @Header("token") String token,
            @Header("user-id") String userId);

    @POST("api/file/download/")
    Call<ProfileServices.FileDownloadResponse> downloadFile(
            @Header("user-id") String userId,
            @Header("token") String token,
            @Body Map<String, Object> requestBody
    );

    @POST("api/home/dashboard/")
    Call<DashboardService.DashboardResponse> getDashboard(
            @Header("token") String token,
            @Header("user-id") String userId);

    @POST("api/home/refresh-wallet/")
    Call<RefreshWallet.refreshWalletResponse> getRefreshWallet(
            @Header("token") String token,
            @Header("user-id") String userId);

}
