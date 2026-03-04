package com.farmo.network.CommonServices;

import android.util.Log;
import com.farmo.network.ApiService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Response;

public class UploadService {

    // 1. REQUEST MODEL
    public static class UploadRequest {
        public String action;
        public String user_id;
        public String upload_id;
        public String file_name;
        public long file_size;
        public String subject;
        public Integer total_chunks;
        public String product_id;
        public String file_purpose;
        public Integer sequence;

        public UploadRequest(String action, String userId, String fileName, long fileSize, String subject) {
            this.action = action;
            this.user_id = userId;
            this.file_name = fileName;
            this.file_size = fileSize;
            this.subject = subject;
        }
    }

    // 2. RESPONSE MODEL
    public static class UploadResponse {
        public String upload_id;
        public String mode;
        public int total_chunks;
        public boolean success;
        public int chunk;
        public String file_url;
        public String error;
        public List<Integer> missing;
        public Integer serial_no;
        public String media_type;
    }

    public static RequestBody toPart(String value) {
        if (value == null) return null;
        return RequestBody.create(MultipartBody.FORM, value);
    }

    public static class ChunkedFileUploader {
        private static final String TAG = "ChunkedUpload";
        private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB

        private final ApiService api;
        private final String userId;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public ChunkedFileUploader(ApiService api, String userId) {
            this.api = api;
            this.userId = userId;
        }

        public void uploadFile(
                File file,
                String subject,
                String productId,
                Integer sequence,
                UploadCallback callback
        ) {
            executor.execute(() -> {
                String uploadId = null;
                try {
                    // 1. INIT
                    int totalChunks = (int) Math.ceil((double) file.length() / CHUNK_SIZE);
                    UploadRequest initReq = new UploadRequest("init", userId, file.getName(), file.length(), subject);
                    initReq.total_chunks = totalChunks;
                    initReq.product_id = productId;
                    initReq.sequence = sequence;
                    initReq.file_purpose = file.getName().toLowerCase().matches(".*\\.(mp4|mov|avi|mkv|webm)$") ? "vid" : "img";

                    Response<UploadResponse> initRes = api.controlAction(initReq).execute();
                    if (!initRes.isSuccessful() || initRes.body() == null) {
                        callback.onError("INIT failed: " + (initRes.body() != null ? initRes.body().error : "Unknown error"));
                        return;
                    }

                    uploadId = initRes.body().upload_id;
                    String mode = initRes.body().mode;

                    // 2. UPLOAD (Full or Chunked)
                    if ("full".equals(mode)) {
                        uploadFullFile(file, uploadId);
                    } else {
                        uploadInChunks(file, uploadId);
                    }

                    // 3. FINISH
                    finishUpload(uploadId, callback);

                } catch (Exception e) {
                    Log.e(TAG, "Upload error", e);
                    if (uploadId != null) abortUpload(uploadId);
                    callback.onError(e.getMessage());
                }
            });
        }

        private void uploadInChunks(File file, String uploadId) throws Exception {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData = (bytesRead == CHUNK_SIZE) ? buffer : Arrays.copyOf(buffer, bytesRead);
                sendChunk(uploadId, chunkIndex, chunkData, file.getName());
                chunkIndex++;
            }
            fis.close();
        }

        private void uploadFullFile(File file, String uploadId) throws Exception {
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int read = fis.read(data);
                if (read < data.length) {
                     data = Arrays.copyOf(data, read);
                }
            }
            sendChunk(uploadId, 0, data, file.getName());
        }

        private void sendChunk(String uploadId, int index, byte[] data, String fileName) throws IOException {
            RequestBody body = createStreamingBody(data, fileName);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, body);

            Response<UploadResponse> res = api.uploadChunk(
                    UploadService.toPart("chunk"),
                    UploadService.toPart(uploadId),
                    UploadService.toPart(String.valueOf(index)),
                    UploadService.toPart(userId),
                    part
            ).execute();

            if (!res.isSuccessful()) {
                throw new IOException("Chunk upload failed at index " + index);
            }
        }

        private void finishUpload(String uploadId, UploadCallback callback) throws Exception {
            UploadRequest finishReq = new UploadRequest("finish", userId, null, 0, null);
            finishReq.upload_id = uploadId;

            Response<UploadResponse> res = api.controlAction(finishReq).execute();
            if (res.isSuccessful() && res.body() != null) {
                callback.onSuccess(res.body());
            } else {
                callback.onError("Finish failed");
            }
        }

        private void abortUpload(String uploadId) {
            try {
                UploadRequest abortReq = new UploadRequest("abort", userId, null, 0, null);
                abortReq.upload_id = uploadId;
                api.controlAction(abortReq).execute();
            } catch (Exception ignored) {}
        }

        private RequestBody createStreamingBody(byte[] data, String fileName) {
            String mime = URLConnection.guessContentTypeFromName(fileName);
            if (mime == null) mime = "application/octet-stream";
            MediaType type = MediaType.parse(mime);
            return new RequestBody() {
                @Override public MediaType contentType() { return type; }
                @Override public void writeTo(BufferedSink sink) throws IOException { sink.write(data); }
                @Override public long contentLength() { return data.length; }
            };
        }

        public interface UploadCallback {
            void onSuccess(UploadResponse response);
            void onError(String error);
        }
    }
}
