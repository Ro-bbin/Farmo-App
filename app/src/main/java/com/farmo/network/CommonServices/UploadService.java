package com.farmo.network.CommonServices;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.farmo.network.ApiService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * UploadService
 * ─────────────────────────────────────────────────────────────────────
 * Handles chunked file uploads for profile pictures, product media, and verification docs.
 *
 * Public uploader classes
 * ────────────────────────
 *   ProfilePictureUploader      — single image,  async or sync
 *   ProductMediaUploader        — single file (image or video), async or sync
 *   ProductMediaBatchUploader   — list of files for one product, sequential background
 *   VerificationUploader        — single doc/image, chunked, async or sync
 *   ProductImageBatchUploader   — image-only batch (validates ext + 20 MB cap)
 *   VerificationImageUploader   — image-only single upload with explicit sequence number
 *
 * Optimizations applied vs previous version
 * ───────────────────────────────────────────
 *  ① MediaType.parse() was called on EVERY chunk/request  → pre-parsed static constants
 *  ② IMAGE_EXTENSIONS / VIDEO_EXTENSIONS were List (O(n) contains) → HashSet (O(1))
 *  ③ Each uploader created its own new Handler(mainLooper)  → single static MAIN_HANDLER
 *  ④ FileUploader + SingleChunkUploader duplicated the entire init→chunk→finish flow
 *     → unified private uploadCore() used by ALL uploaders; ~80 lines of duplicate code removed
 *  ⑤ ProductMediaBatchUploader used boolean[] arrays + busy-wait polling loop to wait for
 *     startSync() callbacks that were posted to the main thread — race condition (no volatile)
 *     and CPU waste → calls uploadCore() directly; result available immediately, no polling
 *  ⑥ ApiService / token / userId were re-fetched inside every iteration of batch loops
 *     → fetched once before the loop
 *  ⑦ readFully() declared "throws Exception" (too broad) → "throws IOException"
 *  ⑧ validateImage() reused CHUNK_SIZE as the image size cap (misleading)
 *     → explicit MAX_IMAGE_SIZE constant
 *  ⑨ resolveFilePurpose() created a new Arrays.asList() on every call → static VIDEO_EXTENSIONS set
 *  ⑩ Per-uploader newSingleThreadExecutor() instances never shared → single ASYNC_EXECUTOR
 *     (CachedThreadPool is fine: uploads are network-bound, not CPU-bound)
 */
public final class UploadService {

    private UploadService() {}

    // ═════════════════════════════════════════════════════════════
    //  § 1 — CONSTANTS  (all static, all pre-computed once)
    // ═════════════════════════════════════════════════════════════

    private static final long CHUNK_SIZE     = 20 * 1024 * 1024L; // 20 MB per chunk
    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024L; // ⑧ separate semantic constant

    private static final String SUBJECT_PROFILE      = "PROFILE_PICTURE";
    private static final String SUBJECT_PRODUCT      = "PRODUCT_MEDIA";
    private static final String SUBJECT_VERIFICATION = "USER_ID_VERIFICATION_MEDIA";

    // ① Pre-parsed once — MediaType.parse() is not free; called thousands of times across chunks
    private static final MediaType MEDIA_OCTET = MediaType.parse("application/octet-stream");
    private static final MediaType MEDIA_TEXT  = MediaType.parse("text/plain");

    // ② HashSet → O(1) contains() vs List's O(n)
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp", "bmp", "tiff", "gif"));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "avi", "mkv", "webm"));

    // ③ One handler shared by the entire class — Handler construction is cheap but pointless to repeat
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    // ⑩ Shared background executor — network-bound work, cached pool is appropriate
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool();


    // ═════════════════════════════════════════════════════════════
    //  § 2 — POJOs
    // ═════════════════════════════════════════════════════════════

    public static final class Init {
        public static final class Req {
            @SerializedName("action")       public final String  action      = "init";
            @SerializedName("file_name")    public final String  fileName;
            @SerializedName("file_size")    public final long    fileSize;
            @SerializedName("subject")      public final String  subject;
            @SerializedName("total_chunks") public final int     totalChunks;
            @SerializedName("product_id")   public final String  productId;
            @SerializedName("file_purpose") public final String  filePurpose;
            @SerializedName("sequence")     public final Integer sequence;

            /** Full constructor — all other constructors delegate here. */
            public Req(String fileName, long fileSize, String subject, int totalChunks,
                       String productId, String filePurpose, Integer sequence) {
                this.fileName    = fileName;
                this.fileSize    = fileSize;
                this.subject     = subject;
                this.totalChunks = totalChunks;
                this.productId   = productId;
                this.filePurpose = filePurpose;
                this.sequence    = sequence;
            }

            /** Profile picture — single chunk, image. */
            public Req(String fileName, long fileSize) {
                this(fileName, fileSize, SUBJECT_PROFILE, 1, null, "img", null);
            }

            /** Verification document — image, explicit chunk count. */
            public Req(String fileName, long fileSize, int totalChunks) {
                this(fileName, fileSize, SUBJECT_VERIFICATION, totalChunks, null, "img", null);
            }

            /** Product media — image or video, explicit chunk count + sequence. */
            public Req(String fileName, long fileSize, String productId,
                       int totalChunks, String filePurpose, Integer sequence) {
                this(fileName, fileSize, SUBJECT_PRODUCT, totalChunks, productId, filePurpose, sequence);
            }

            /** Image-only convenience — always single chunk, purpose = "img". */
            public Req(String fileName, long fileSize, String subject, String productId, Integer sequence) {
                this(fileName, fileSize, subject, 1, productId, "img", sequence);
            }
        }

        public static final class Res {
            @SerializedName("upload_id")    public String uploadId;
            @SerializedName("total_chunks") public int    totalChunks;
            @SerializedName("mode")         public String mode;
            @SerializedName("error")        public String error;

            public boolean isSuccess() { return error == null && uploadId != null; }
        }
    }

    public static final class Chunk {
        public static final class Res {
            @SerializedName("success") public boolean success;
            @SerializedName("chunk")   public int     chunk;
            @SerializedName("error")   public String  error;

            public boolean isSuccess() { return success && error == null; }
        }
    }

    public static final class Finish {
        public static final class Req {
            @SerializedName("action")    public final String action    = "finish";
            @SerializedName("upload_id") public final String uploadId;
            public Req(String uploadId) { this.uploadId = uploadId; }
        }

        public static final class Res {
            @SerializedName("success")    public boolean       success;
            @SerializedName("file_url")   public String        fileUrl;
            @SerializedName("serial_no")  public Integer       serialNo;
            @SerializedName("media_type") public String        mediaType;
            @SerializedName("error")      public String        error;
            @SerializedName("missing")    public List<Integer> missingChunks;

            public boolean isSuccess() { return success && error == null; }
        }
    }

    public static final class Abort {
        public static final class Req {
            @SerializedName("action")    public final String action    = "abort";
            @SerializedName("upload_id") public final String uploadId;
            public Req(String uploadId) { this.uploadId = uploadId; }
        }
        public static final class Res {
            @SerializedName("message") public String message;
            @SerializedName("error")   public String error;
            public boolean isSuccess() { return error == null; }
        }
    }


    // ═════════════════════════════════════════════════════════════
    //  § 3 — CALLBACKS
    // ═════════════════════════════════════════════════════════════

    /** Single-file upload result. All methods posted to the main thread. */
    public interface UploadCallback {
        void onSuccess(Finish.Res result);
        void onFailure(String errorMessage);
        void onProgress(int percent);
    }

    /** Batch result for ProductMediaBatchUploader. All methods posted to the main thread. */
    public interface BatchUploadCallback {
        void onBatchSuccess();
        void onBatchProgress(int completed, int total, int currentFilePercent);
        void onBatchFailure(String error);
    }

    /**
     * Image-only batch result for ProductImageBatchUploader.
     * No per-file progress (single-chunk images jump 0→100%).
     * All methods posted to the main thread.
     */
    public interface ImageBatchUploadCallback {
        void onBatchSuccess();
        /** @param completed 1-based count of files finished */
        void onBatchProgress(int completed, int total);
        /** @param failedIndex 0-based index of the file that failed */
        void onBatchFailure(int failedIndex, String error);
    }


    // ═════════════════════════════════════════════════════════════
    //  § 4 — PUBLIC UPLOADER CLASSES
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // 4-A  Profile Picture
    // ─────────────────────────────────────────────────────────────

    public static final class ProfilePictureUploader {
        private final Context        context;
        private final File           imageFile;
        private final UploadCallback callback;

        public ProfilePictureUploader(Context context, File imageFile, UploadCallback callback) {
            this.context   = context;
            this.imageFile = imageFile;
            this.callback  = callback;
        }

        /** Runs upload on a background thread; callbacks dispatched to main thread. */
        public void start() {
            Init.Req req = new Init.Req(imageFile.getName(), imageFile.length());
            ASYNC_EXECUTOR.execute(() -> dispatchUpload(context, imageFile, req, callback));
        }

        /** Runs upload on the CALLING thread; callbacks still dispatched to main thread. */
        public void startSync() {
            Init.Req req = new Init.Req(imageFile.getName(), imageFile.length());
            dispatchUpload(context, imageFile, req, callback);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4-B  Product Media  (single file — image or video)
    // ─────────────────────────────────────────────────────────────

    public static final class ProductMediaUploader {
        private final Context        context;
        private final File           file;
        private final String         productId;
        private final Integer        sequence;
        private final UploadCallback callback;

        public ProductMediaUploader(Context context, File file, String productId,
                                    Integer sequence, UploadCallback callback) {
            this.context   = context;
            this.file      = file;
            this.productId = productId;
            this.sequence  = sequence;
            this.callback  = callback;
        }

        public void start() {
            Init.Req req = buildReq();
            ASYNC_EXECUTOR.execute(() -> dispatchUpload(context, file, req, callback));
        }

        public void startSync() {
            dispatchUpload(context, file, buildReq(), callback);
        }

        private Init.Req buildReq() {
            long   size        = file.length();
            int    totalChunks = (int) Math.ceil((double) size / CHUNK_SIZE);
            String purpose     = VIDEO_EXTENSIONS.contains(getExtension(file.getName())) ? "vid" : "img"; // ⑨
            return new Init.Req(file.getName(), size, productId, totalChunks, purpose, sequence);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4-C  Product Media Batch  (image + video, sequential background)
    // ─────────────────────────────────────────────────────────────

    /**
     * ⑤ Previous version had a critical race condition:
     *    startSync() posts callbacks to the main thread, but the batch executor
     *    polled boolean[] done without volatile — update was never guaranteed visible.
     *    Replaced with direct uploadCore() call whose result is available immediately
     *    on the background thread, with no polling or synchronization primitives needed.
     *
     * ⑥ ApiService / token / userId now fetched once before the loop, not per file.
     */
    public static final class ProductMediaBatchUploader {
        private final Context             context;
        private final List<File>          files;
        private final String              productId;
        private final BatchUploadCallback callback;

        public ProductMediaBatchUploader(Context context, List<File> files,
                                         String productId, BatchUploadCallback callback) {
            this.context   = context;
            this.files     = files;
            this.productId = productId;
            this.callback  = callback;
        }

        public void start() {
            ASYNC_EXECUTOR.execute(() -> {
                // ⑥ Resolve credentials once for the entire batch
                ApiSession sess  = ApiSession.from(context);
                int        total = files.size();

                for (int i = 0; i < total; i++) {
                    File      file = files.get(i);
                    final int seq  = i + 1;
                    final int idx  = i;

                    long   size        = file.length();
                    int    totalChunks = (int) Math.ceil((double) size / CHUNK_SIZE);
                    String purpose     = VIDEO_EXTENSIONS.contains(getExtension(file.getName())) ? "vid" : "img";
                    Init.Req req = new Init.Req(file.getName(), size, productId, totalChunks, purpose, seq);

                    // ⑤ uploadCore() is fully synchronous — result is directly available here
                    UploadResult result = uploadCore(sess, file, req,
                            pct -> MAIN_HANDLER.post(() -> callback.onBatchProgress(idx, total, pct)));

                    if (!result.success) {
                        MAIN_HANDLER.post(() -> callback.onBatchFailure(result.error));
                        return;
                    }
                }
                MAIN_HANDLER.post(callback::onBatchSuccess);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4-D  Verification  (single doc/image — chunked)
    // ─────────────────────────────────────────────────────────────

    public static final class VerificationUploader {
        private final Context        context;
        private final File           docFile;
        private final UploadCallback callback;

        public VerificationUploader(Context context, File docFile, UploadCallback callback) {
            this.context  = context;
            this.docFile  = docFile;
            this.callback = callback;
        }

        public void start() {
            Init.Req req = buildReq();
            ASYNC_EXECUTOR.execute(() -> dispatchUpload(context, docFile, req, callback));
        }

        public void startSync() {
            dispatchUpload(context, docFile, buildReq(), callback);
        }

        private Init.Req buildReq() {
            long size        = docFile.length();
            int  totalChunks = (int) Math.ceil((double) size / CHUNK_SIZE);
            return new Init.Req(docFile.getName(), size, totalChunks);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4-E  Product Image Batch  (image-only, validates ext + 20 MB)
    // ─────────────────────────────────────────────────────────────

    /**
     * Uploads a list of product <em>images</em> one-by-one in the background.
     * Validates extension and 20 MB cap before each upload.
     * Sequence numbers are assigned automatically (1-based, matching list order).
     * Stops and reports immediately if any file fails.
     *
     * <pre>
     *   new UploadService.ProductImageBatchUploader(ctx, images, productId, new ImageBatchUploadCallback() {
     *       public void onBatchSuccess()                              { ... }
     *       public void onBatchProgress(int done, int total)         { ... }
     *       public void onBatchFailure(int failedIdx, String error)  { ... }
     *   }).start();
     * </pre>
     */
    public static final class ProductImageBatchUploader {
        private final Context                  context;
        private final List<File>               imageFiles;
        private final String                   productId;
        private final ImageBatchUploadCallback callback;

        public ProductImageBatchUploader(Context context, List<File> imageFiles,
                                         String productId, ImageBatchUploadCallback callback) {
            this.context    = context;
            this.imageFiles = imageFiles;
            this.productId  = productId;
            this.callback   = callback;
        }

        public void start() {
            ASYNC_EXECUTOR.execute(() -> {
                // ⑥ Credentials resolved once for the entire batch
                ApiSession sess  = ApiSession.from(context);
                int        total = imageFiles.size();

                for (int i = 0; i < total; i++) {
                    File      file = imageFiles.get(i);
                    final int idx  = i;
                    final int seq  = i + 1;

                    String precheck = validateImage(file);
                    if (precheck != null) {
                        MAIN_HANDLER.post(() -> callback.onBatchFailure(idx, precheck));
                        return;
                    }

                    Init.Req     req    = new Init.Req(file.getName(), file.length(),
                            SUBJECT_PRODUCT, productId, seq);
                    UploadResult result = uploadCore(sess, file, req, null); // no per-chunk progress for single-chunk images

                    if (!result.success) {
                        MAIN_HANDLER.post(() -> callback.onBatchFailure(idx, result.error));
                        return;
                    }

                    final int done = i + 1;
                    MAIN_HANDLER.post(() -> callback.onBatchProgress(done, total));
                }
                MAIN_HANDLER.post(callback::onBatchSuccess);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4-F  Verification Image Uploader  (image-only, with sequence)
    // ─────────────────────────────────────────────────────────────

    /**
     * Uploads a single verification <em>image</em> with an explicit sequence number.
     * Validates extension and 20 MB cap before uploading.
     *
     * <pre>
     *   new UploadService.VerificationImageUploader(ctx, file, 1, new UploadCallback() {
     *       public void onSuccess(Finish.Res r) { ... }
     *       public void onFailure(String err)   { ... }
     *       public void onProgress(int pct)     { }
     *   }).start();
     * </pre>
     */
    public static final class VerificationImageUploader {
        private final Context        context;
        private final File           imageFile;
        private final int            sequence;
        private final UploadCallback callback;

        public VerificationImageUploader(Context context, File imageFile,
                                         int sequence, UploadCallback callback) {
            this.context   = context;
            this.imageFile = imageFile;
            this.sequence  = sequence;
            this.callback  = callback;
        }

        public void start() {
            ASYNC_EXECUTOR.execute(() -> runUpload());
        }

        private void runUpload() {
            String precheck = validateImage(imageFile);
            if (precheck != null) {
                MAIN_HANDLER.post(() -> callback.onFailure(precheck));
                return;
            }

            ApiSession   sess   = ApiSession.from(context);
            Init.Req     req    = new Init.Req(imageFile.getName(), imageFile.length(),
                    SUBJECT_VERIFICATION, null, sequence);
            UploadResult result = uploadCore(sess, imageFile, req, null);

            MAIN_HANDLER.post(() -> {
                if (result.success) {
                    callback.onProgress(100);
                    callback.onSuccess(result.finishRes);
                } else {
                    callback.onFailure(result.error);
                }
            });
        }
    }


    // ═════════════════════════════════════════════════════════════
    //  § 5 — PRIVATE CORE ENGINE
    // ═════════════════════════════════════════════════════════════

    /**
     * ④ Unified upload engine — replaces the duplicate FileUploader + SingleChunkUploader.
     *
     * Runs fully synchronously on the calling thread.
     * Never touches the main thread — all callback dispatching is the caller's responsibility.
     *
     * @param sess     pre-resolved API credentials (fetch once per batch, not per file)
     * @param file     file to upload
     * @param initReq  init request body
     * @param progress called on the background thread after each chunk; may be null
     */
    private static UploadResult uploadCore(
            ApiSession sess, File file, Init.Req initReq, ProgressListener progress) {

        String uploadId = null;
        try {
            // ── 1. Init ──────────────────────────────────────────────────────────
            Response<Init.Res> initResp = sess.api.uploadInit(sess.token, sess.userId, initReq).execute();
            Init.Res initBody = initResp.body();
            if (!initResp.isSuccessful() || initBody == null || !initBody.isSuccess()) {
                return fail(initBody != null ? initBody.error
                        : "Init failed (HTTP " + initResp.code() + ")");
            }
            uploadId = initBody.uploadId;
            int totalChunks = initBody.totalChunks;

            // ── 2. Stream chunks ─────────────────────────────────────────────────
            byte[] buf = new byte[(int) CHUNK_SIZE];
            try (FileInputStream fis = new FileInputStream(file)) {
                int chunkIdx = 0;
                int bytesRead;
                while ((bytesRead = readFully(fis, buf)) > 0) {
                    // Avoid allocating a new array when the chunk fills the buffer exactly
                    byte[] chunkBytes = (bytesRead == buf.length) ? buf : Arrays.copyOf(buf, bytesRead);

                    MultipartBody.Part part = MultipartBody.Part.createFormData(
                            "file", file.getName(),
                            RequestBody.create(chunkBytes, MEDIA_OCTET)); // ① pre-parsed constant

                    Response<Chunk.Res> chunkResp = sess.api.uploadChunk(
                            sess.token, sess.userId,
                            plain("chunk"), plain(uploadId), plain(String.valueOf(chunkIdx)),
                            part).execute();

                    Chunk.Res chunkBody = chunkResp.body();
                    if (!chunkResp.isSuccessful() || chunkBody == null || !chunkBody.isSuccess()) {
                        silentAbort(sess, uploadId);
                        return fail(chunkBody != null ? chunkBody.error
                                : "Chunk " + chunkIdx + " failed (HTTP " + chunkResp.code() + ")");
                    }
                    chunkIdx++;
                    if (progress != null) progress.onProgress((int) ((chunkIdx / (float) totalChunks) * 100));
                }
            }

            // ── 3. Finish ────────────────────────────────────────────────────────
            Response<Finish.Res> finishResp = sess.api.uploadFinish(sess.token, sess.userId,
                    new Finish.Req(uploadId)).execute();
            Finish.Res finishBody = finishResp.body();
            if (!finishResp.isSuccessful() || finishBody == null || !finishBody.isSuccess()) {
                return fail(finishBody != null ? finishBody.error
                        : "Finish failed (HTTP " + finishResp.code() + ")");
            }
            return new UploadResult(finishBody);

        } catch (Exception e) {
            if (uploadId != null) silentAbort(sess, uploadId);
            return fail(e.getMessage() != null ? e.getMessage() : "Unexpected error");
        }
    }

    /**
     * Convenience wrapper: resolves session, runs uploadCore, dispatches callbacks to main thread.
     * Used by all single-file async/sync uploaders (ProfilePicture, ProductMedia, Verification).
     */
    private static void dispatchUpload(Context context, File file, Init.Req req, UploadCallback cb) {
        ApiSession   sess   = ApiSession.from(context);
        UploadResult result = uploadCore(sess, file, req,
                pct -> MAIN_HANDLER.post(() -> cb.onProgress(pct)));

        MAIN_HANDLER.post(() -> {
            if (result.success) cb.onSuccess(result.finishRes);
            else                cb.onFailure(result.error);
        });
    }


    // ═════════════════════════════════════════════════════════════
    //  § 6 — PRIVATE SUPPORT TYPES
    // ═════════════════════════════════════════════════════════════

    /**
     * ⑥ Bundles ApiService + credentials resolved once per upload operation.
     * Avoids re-fetching them on every file in a batch loop.
     */
    private static final class ApiSession {
        final ApiService api;
        final String     token;
        final String     userId;

        private ApiSession(Context context) {
            SessionManager sm = new SessionManager(context);
            this.api    = RetrofitClient.getApiService(context);
            this.token  = sm.getAuthToken();
            this.userId = sm.getUserId();
        }

        static ApiSession from(Context context) { return new ApiSession(context); }
    }

    /** Internal upload result — success carries FinishRes, failure carries an error string. */
    private static final class UploadResult {
        final boolean    success;
        final Finish.Res finishRes;
        final String     error;

        UploadResult(Finish.Res res) { success = true;  finishRes = res;  error = null; }
        UploadResult(String err)     { success = false; finishRes = null; error = err;  }
    }

    /**
     * Progress callback invoked on the background thread.
     * The caller is responsible for posting to the main thread if needed.
     */
    @FunctionalInterface
    private interface ProgressListener {
        void onProgress(int percent);
    }


    // ═════════════════════════════════════════════════════════════
    //  § 7 — SHARED UTILITIES
    // ═════════════════════════════════════════════════════════════

    /**
     * Reads from {@code fis} until {@code buf} is completely filled or the stream ends.
     * Unlike FileInputStream.read(byte[]), never returns a partial result mid-stream.
     *
     * @return bytes placed into buf; 0 means end-of-stream was already reached before any read
     * @throws IOException on I/O error (⑦ was previously "throws Exception")
     */
    private static int readFully(FileInputStream fis, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int n = fis.read(buf, total, buf.length - total);
            if (n == -1) break;
            total += n;
        }
        return total;
    }

    /**
     * Returns {@code null} if the file passes image validation, or an error message if not.
     * Checks: file exists · extension is a known image type · size ≤ MAX_IMAGE_SIZE (20 MB).
     */
    private static String validateImage(File file) {
        if (file == null || !file.exists())
            return "File does not exist: " + (file != null ? file.getName() : "null");

        if (!IMAGE_EXTENSIONS.contains(getExtension(file.getName()))) // ② O(1) HashSet lookup
            return "Not an image file: " + file.getName()
                    + " (allowed: jpg, jpeg, png, webp, bmp, tiff, gif)";

        if (file.length() > MAX_IMAGE_SIZE) // ⑧ named constant, not CHUNK_SIZE
            return "File too large: " + file.getName()
                    + " (" + (file.length() / (1024 * 1024)) + " MB, max 20 MB)";

        return null;
    }

    private static UploadResult fail(String msg) { return new UploadResult(msg); }

    private static RequestBody plain(String v) {
        return RequestBody.create(v, MEDIA_TEXT); // ① pre-parsed constant
    }

    private static void silentAbort(ApiSession sess, String uploadId) {
        try { sess.api.uploadAbort(sess.token, sess.userId, new Abort.Req(uploadId)).execute(); }
        catch (Exception ignored) {}
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}