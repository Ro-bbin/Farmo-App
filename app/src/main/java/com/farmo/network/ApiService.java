package com.farmo.network;

import com.farmo.network.CommonServices.BazarModuleService;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.network.CommonServices.ProductDetailsServices;
import com.farmo.network.CommonServices.UploadService;
import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
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
import com.farmo.network.farmer.AddProductService;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

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
            @Header("token") String token,
            @Header("user-id") String userId,
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

    @POST("api/product/category/")
    Call<AddProductService.ProductTypeResponse> getProductTypes(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body AddProductService.ProductTypeRequest productTypeRequest);

    @POST("api/product/category/products/")
    Call<AddProductService.KeywordResponse> getKeyword(
        @Header("token") String token,
        @Header("user-id") String userId,
        @Body AddProductService.keywordRequest keywordRequest);

    @POST("api/product/add/")
    Call<AddProductService.AddProductResponse> addProduct(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body AddProductService.AddProductRequest addProductRequest);

    @POST("api/file/download/")
    Call<DownloadService.Res> getDownloadedFile(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body DownloadService.Req request);

    @POST("api/file/upload/")
    Call<UploadService.UploadResponse> controlAction(@Body UploadService.UploadRequest request);

    // For Chunk Upload
    @Multipart
    @POST("api/file/upload/")
    Call<UploadService.UploadResponse> uploadChunk(
            @Part("action") RequestBody action,
            @Part("upload_id") RequestBody uploadId,
            @Part("chunk_index") RequestBody chunkIndex,
            @Part("user_id") RequestBody userId,
            @Part MultipartBody.Part file
    );

    @POST("api/product/feed/")
    Call<BazarModuleService.BazarResponse> getBazarProducts(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body BazarModuleService.BazarRequest request);

    @POST("api/product/users/details/")
    Call<ProductDetailsServices.Response> getProductDetails(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body ProductDetailsServices.Request request);
}
