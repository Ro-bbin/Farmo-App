package com.farmo.network.CommonServices;

import android.content.Context;
import android.util.Base64;

import com.farmo.network.ApiService;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;

import retrofit2.Call;
import retrofit2.Response;

/**
 * SINGLE PARENT CLASS
 * - Request model
 * - Response model
 * - Download executor
 */
public class DownloadService {

    /*───────────────────────────────
     * REQUEST MODEL
     *───────────────────────────────*/
    public static class Req {

        @SerializedName("subject")
        private String subject;

        @SerializedName("product_id")
        private String product_id;

        @SerializedName("seq")
        private int seq;

        // Profile picture
        public static Req profile() {
            return new Req("PROFILE_PICTURE", null, 0);
        }

        // Product media
        public static Req product(String productId, int seq) {
            return new Req("PRODUCT_MEDIA", productId, seq);
        }

        // Verification media
        public static Req verification(int seq) {
            return new Req("USER_ID_VERIFICATION_MEDIA", null, seq);
        }

        private Req(String subject, String productId, int seq) {
            this.subject = subject;
            this.product_id = productId;
            this.seq = seq;
        }
    }

    /*───────────────────────────────
     * RESPONSE MODEL
     *───────────────────────────────*/
    public static class Res {

        public String file;        // Base64
        public String mime_type;   // image/jpeg, video/mp4
        public String media_type;  // img | vid
        public int total;
        public int seq;

        public boolean isEmpty() {
            return file == null || file.isEmpty();
        }
    }

    /*───────────────────────────────
     * EXECUTOR
     *───────────────────────────────*/
    public static class Executor {

        public interface Callback {
            void onSuccess(File file, Res raw);
            void onError(String error);
        }

        private final ApiService api;
        private final Context context;

        public Executor(Context context, ApiService api) {
            this.context = context.getApplicationContext();
            this.api = api;
        }

        public void download(
                String token,
                String userId,
                Req req,
                Callback cb
        ) {

            api.getDownloadedFile(token, userId, req)
                    .enqueue(new retrofit2.Callback<Res>() {

                        @Override
                        public void onResponse(
                                Call<Res> call,
                                Response<Res> response
                        ) {

                            if (!response.isSuccessful() || response.body() == null) {
                                cb.onError("Download failed");
                                return;
                            }

                            Res res = response.body();

                            if (res.isEmpty()) {
                                cb.onError("Empty file");
                                return;
                            }

                            try {
                                File file = writeToFile(res);
                                cb.onSuccess(file, res);
                            } catch (Exception e) {
                                cb.onError(e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(Call<Res> call, Throwable t) {
                            cb.onError(t.getMessage());
                        }
                    });
        }

        /*───────────────────────────────
         * BASE64 → FILE
         *───────────────────────────────*/
        private File writeToFile(Res res) throws Exception {

            byte[] bytes = Base64.decode(res.file, Base64.DEFAULT);

            String ext =
                    res.mime_type.contains("png") ? ".png" :
                            res.mime_type.contains("jpeg") ? ".jpg" :
                                    res.mime_type.contains("mp4") ? ".mp4" :
                                            ".bin";

            File file = new File(
                    context.getCacheDir(),
                    "download_" + res.seq + ext
            );

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }

            return file;
        }
    }
}