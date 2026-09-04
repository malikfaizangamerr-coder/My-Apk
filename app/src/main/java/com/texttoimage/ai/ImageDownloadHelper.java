package com.texttoimage.ai;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageDownloadHelper {

    private Context context;

    public ImageDownloadHelper(Context context) {
        this.context = context;
    }

    // ✅ Download image from URL and save to Gallery
    public void downloadImageToGallery(String imageUrl, String fileName) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            showToast("Invalid image URL");
            return;
        }

        showToast("Downloading image...");

        new Thread(() -> {
            try {
                if (imageUrl.startsWith("data:image")) {
                    saveDataUriImage(imageUrl, fileName);
                } else {
                    saveHttpImageToGallery(imageUrl, fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Download failed: " + e.getMessage());
            }
        }).start();
    }

    // ✅ Save HTTP image
    private void saveHttpImageToGallery(String imageUrl, String fileName) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(imageUrl).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new Exception("Server error: " + response.code());
        }

        byte[] imageData = response.body().bytes();
        saveImageToGallery(imageData, fileName);
    }

    // ✅ Save data:image URI
    private void saveDataUriImage(String dataUri, String fileName) throws Exception {
        String[] parts = dataUri.split(",");
        if (parts.length != 2) {
            throw new Exception("Invalid data URI format");
        }

        String base64Data = parts[1];
        byte[] imageData = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
        saveImageToGallery(imageData, fileName);
    }

    // ✅ Direct save method (for blob/images from JavaScript)
    public void saveImageToGalleryDirect(byte[] imageData, String fileName) {
        if (imageData == null || imageData.length == 0) {
            showToast("No image data to save");
            return;
        }
        saveImageToGallery(imageData, fileName);
    }

    // ✅ Save image bytes to Gallery (Modern Android)
    private void saveImageToGallery(byte[] imageData, String fileName) {
        try {
            String mimeType = getMimeType(fileName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp");

                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (FileOutputStream out = (FileOutputStream) context.getContentResolver().openOutputStream(uri)) {
                        out.write(imageData);
                        showToast("✅ Image saved to Gallery!");
                    }
                } else {
                    showToast("Failed to save image");
                }
            } else {
                // Android 9 and below - Legacy storage
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appDir = new File(picturesDir, "MyApp");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }

                File imageFile = new File(appDir, fileName);
                try (FileOutputStream out = new FileOutputStream(imageFile)) {
                    out.write(imageData);
                    showToast("✅ Image saved to: " + imageFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Save failed: " + e.getMessage());
        }
    }

    // ✅ Get MIME type from filename
    private String getMimeType(String fileName) {
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".webp")) return "image/webp";
        return "image/png";
    }

    // ✅ Generate filename from URL
    public String generateFileName(String url) {
        String fileName = URLUtil.guessFileName(url, null, null);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "image_" + System.currentTimeMillis() + ".png";
        }
        return fileName;
    }

    // ✅ Show toast on UI thread
    private void showToast(String message) {
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
