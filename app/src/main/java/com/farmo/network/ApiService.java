package com.farmo.network;

import com.farmo.network.CommonServices.AddressService;
import com.farmo.network.CommonServices.BazarModuleService;
import com.farmo.network.CommonServices.ConnectionService;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.network.CommonServices.OrderDetails;
import com.farmo.network.CommonServices.OrderManagementService;
import com.farmo.network.CommonServices.OrderRequestService;
import com.farmo.network.CommonServices.ProductDetailsServices;
import com.farmo.network.CommonServices.TransactionService;
import com.farmo.network.CommonServices.UpdateProfile;
import com.farmo.network.CommonServices.UploadService;
import com.farmo.network.CommonServices.WalletService;
import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.auth.ChangePassword;
import com.farmo.network.auth.CheckUserIDService;
import com.farmo.network.auth.ForgotPasswordChangePasswordRequest;
import com.farmo.network.auth.ForgotPasswordRequest;
import com.farmo.network.auth.ForgotPasswordResponse;
import com.farmo.network.auth.LoginRequest;
import com.farmo.network.auth.LoginResponse;
import com.farmo.network.auth.RegisterRequest;
import com.farmo.network.auth.TokenLoginRequest;
import com.farmo.network.auth.VerifyEmailRequest;
import com.farmo.network.auth.VerifyOtpRequest;
import com.farmo.network.consumer.ChangeToFarmer;
import com.farmo.network.farmer.AddProductService;
import com.farmo.network.farmer.MyProductListService;
import com.farmo.network.farmer.UpdateProduct;

import org.checkerframework.checker.units.qual.C;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/auth/login/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/login-with-token/")
    Call<LoginResponse> loginWithToken(@Body TokenLoginRequest tokenLoginRequest);

    @POST("api/auth/register/")
    Call<MessageResponse> register(@Body RegisterRequest registerRequest);

    @POST("api/auth/check-userid/")
    Call<CheckUserIDService.Response> checkUserID(@Body CheckUserIDService.Request req);


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
    Call<Void> logout(
            @Header("token") String token,
            @Header("user-id") String userId);

    @POST("api/user/change-to-farmer/")
    Call<MessageResponse> changeToFarmer(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body ChangeToFarmer.Request request);

    @POST("api/user/change-password/")
    Call<MessageResponse> changePassword(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body ChangePassword.Request request);

    @POST("api/user/update-profile/")
    Call<MessageResponse> updateProfile(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body UpdateProfile.Request request);


    @POST("api/user/own-address/")
    Call<AddressService.AddressResponse> getOWNAddress(
            @Header("token") String token,
            @Header("user-id") String userId);


    @POST("api/user/view-profile/")
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

    @GET("api/product/get-for-update/{pid}/")
    Call<UpdateProduct.Request> getDetails_before_UpdateProduct(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Path("pid") String pid);


    @PUT("api/product/update/")
    Call<UpdateProduct.Response> updateProduct(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body UpdateProduct.Request request);

    @POST("api/product/toggle-availability/")
    Call<Void> toggleAvailability(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body UpdateProduct.ToggleAvailabilityRequest request);

    @POST("api/file/download/")
    Call<DownloadService.Res> getDownloadedFile(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body DownloadService.Req request);

    @POST("api/file/upload/")
    Call<UploadService.Init.Res> uploadInit(
            @Header("token")   String token,
            @Header("user-id") String userId,
            @Body UploadService.Init.Req request
    );

    @Multipart
    @POST("api/file/upload/")
    Call<UploadService.Chunk.Res> uploadChunk(
            @Header("token")          String token,
            @Header("user-id")        String userId,
            @Part("action")           RequestBody action,
            @Part("upload_id")        RequestBody uploadId,
            @Part("chunk_index")      RequestBody chunkIndex,
            @Part                     MultipartBody.Part file
    );

    @POST("api/file/upload/")
    Call<UploadService.Finish.Res> uploadFinish(
            @Header("token")   String token,
            @Header("user-id") String userId,
            @Body UploadService.Finish.Req request
    );

    @POST("api/file/upload/")
    Call<UploadService.Abort.Res> uploadAbort(
            @Header("token")   String token,
            @Header("user-id") String userId,
            @Body UploadService.Abort.Req request
    );


    @POST("api/product/mylist/")
    Call<MyProductListService.Response> getMyProductList(
      @Header("token") String token,
      @Header("user-id") String userId,
      @Body MyProductListService.Request request
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

    @POST("api/user/order/request/")
    Call<OrderRequestService.OrderRequestResponse> getOrderRequest(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body OrderRequestService.OrderRequest request);


    @POST("api/user/order/list/")
    Call<OrderManagementService.Response> getOrderList(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body OrderManagementService.Request request);

    @POST("api/user/order/detail/")
    Call<OrderDetails.Response> getOrderDetails(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body OrderDetails.Request request);


    @POST("api/user/order/status-update/")
    Call<MessageResponse> updateOrderStatus(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body OrderManagementService.UpdateStatusRequest request);

    @POST("api/user/order/confirm-delivery/")
    Call<MessageResponse> confirmDelivery(
        @Header("token") String token,
        @Header("user-id") String userId,
        @Body OrderManagementService.ConfirmOrderRequest request);

    @POST("api/user/order/cancel/")
    Call<MessageResponse> cancelOrder(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body OrderManagementService.ConfirmOrderRequest request);

    @POST("api/transaction/recent/")
    Call<TransactionService.RecentTransResponse> getRecentTransactions(
            @Header("token") String token,
            @Header("user-id") String userId);


    @POST("api/user/wallet/page/")
    Call<WalletService.walletPageResponse> getWalletPage(
            @Header("token") String token,
            @Header("user-id") String userId);

    @POST("api/user/wallet/set-pin/")
    Call<MessageResponse> activateWallet(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body WalletService.setup_pinReq body);

    @POST("api/user/wallet/forget-pin/")
    Call<MessageResponse> forgetPin(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body WalletService.ChangePinRequest body); // use password and new pin

    @POST("api/user/wallet/change-pin/")
    Call<MessageResponse> changePin(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body WalletService.ChangePinRequest body); // use old and new pin

    @POST("api/user/transaction/")
    Call<TransactionService.TransactionHistoryResponse> getTransactionHistory(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body TransactionService.TransactionHistoryRequest body);

    @POST("api/wallet/add-withdraw/")
    Call<MessageResponse> add_withdraw(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body WalletService.add_withdrawRequest body);



    @POST("api/user/connections/")
    Call<ConnectionService.ConnectionsResponse> getConnections(
            @Header("token") String token,
            @Header("user-id") String userId,
            @Body ConnectionService.ConnectionsRequest body);


}
