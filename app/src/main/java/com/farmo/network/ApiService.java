package com.farmo.network;

import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.auth.ForgotPasswordChangePasswordRequest;
import com.farmo.network.auth.ForgotPasswordRequest;
import com.farmo.network.auth.ForgotPasswordResponse;
import com.farmo.network.auth.LoginRequest;
import com.farmo.network.auth.LoginResponse;
import com.farmo.network.auth.RegisterRequest;
import com.farmo.network.auth.TokenLoginRequest;
import com.farmo.network.auth.VerifyEmailRequest;
import com.farmo.network.auth.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/login/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/login-with-token/")
    Call<LoginResponse> loginWithToken(@Body TokenLoginRequest tokenLoginRequest);

    @POST("api/auth/register/")
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
    Call<MessageResponse> logout();

    @POST("api/user/profile/")
    Call<UserProfileResponse> getUserProfile(@Query("user_id") String userId);

    @POST("api/home/dashboard/")
    Call<DashboardService.DashboardResponse> getDashboard();

    @POST("api/home/refresh-wallet/")
    Call<RefreshWallet.refreshWalletResponse> getRefreshWallet();
}
