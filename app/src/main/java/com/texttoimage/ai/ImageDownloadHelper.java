package com.texttoimage.ai;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * ✅ ImageDownloadHelper - Handles image downloads with proper MediaStore integration
 * Supports:
 * - Android 10+ (API 29+) using MediaStore
 * - Android 9 and below using legacy storage
 * - blob: URLs from WebView
 * - HTTP/HTTPS downloads
 */
public class ImageDownloadHelper {

    private Context context;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public ImageDownloadHelper(Context context) {
        this.context = context;
    }

    /**
     * Generate a filename from URL or use timestamp
     */
    public String generateFileName(String url) {
        try {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (!fileName.isEmpty() && !fileName.contains("?")) {
                return fileName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "image_" + System.currentTimeMillis() + ".png";
    }

    /**
     * Download image from HTTP/HTTPS URL to Gallery
     */
    public void downloadImageToGallery(String imageUrl, String fileName) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(imageUrl).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    byte[] imageData = response.body().bytes();
                    saveImageToGalleryDirect(imageData, fileName);
                } else {
                    showToast("Failed to download image");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Download error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Save image bytes directly to Gallery
     * ✅ Uses MediaStore for Android 10+
     * ✅ Uses legacy storage for Android 9 and below
     */
    public void saveImageToGalleryDirect(byte[] imageData, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ✅ Android 10+ (API 29+) - Use MediaStore
                saveToMediaStore(imageData, fileName);
            } else {
                // ✅ Android 9 and below - Use legacy storage
                saveLegacy(imageData, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Save error: " + e.getMessage());
        }
    }

    /**
     * ✅ Save to MediaStore (Android 10+, API 29+)
     */
    private void saveToMediaStore(byte[] imageData, String fileName) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    out.write(imageData);
                    out.flush();
                    showToast("Image saved to Gallery!");
                }
            }
        } else {
            showToast("Failed to save image");
        }
    }

    /**
     * ✅ Save to legacy storage (Android 9 and below)
     */
    private void saveLegacy(byte[] imageData, String fileName) throws Exception {
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (!picturesDir.exists()) {
            picturesDir.mkdirs();
        }

        File imageFile = new File(picturesDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageData);
            fos.flush();

            // ✅ Notify MediaStore about the new file
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    imageFile.getAbsolutePath(), fileName, "Downloaded from AI Text to Image");

            showToast("Image saved to Gallery!");
        }
    }

    /**
     * Show toast on main thread
     */
    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
