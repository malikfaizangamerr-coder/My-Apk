package com.texttoimage.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private LinearLayout adContainer;
    private ProgressBar progressBar;
    private TextView timerText;
    private Button closeAdButton;
    private String currentImageData = "";
    private CountDownTimer countDownTimer;
    private boolean isAdCompleted = false;

    // ✅ Adsterra Direct Link (Your Link)
    private final String ADSTERRA_LINK = "https://www.profitableratecpmnetwork.com/qipfe6eqa?key=3742cf2fbc4cc31e29fe7df287ed757a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        webView = findViewById(R.id.webView);
        adContainer = findViewById(R.id.adContainer);
        progressBar = findViewById(R.id.progressBar);
        timerText = findViewById(R.id.timerText);
        closeAdButton = findViewById(R.id.closeAdButton);

        // Storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }

        setupWebView();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectImageShareScript();
            }
        });

        // Handle long-press / download
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url != null && url.startsWith("blob:")) {
                shareBlobImage(url);
            } else {
                Toast.makeText(MainActivity.this, "Share not supported", Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Show Adsterra Ad Overlay with Timer
    private void showAdsterraAd() {
        runOnUiThread(() -> {
            adContainer.setVisibility(android.view.View.VISIBLE);
            webView.setVisibility(android.view.View.GONE);

            timerText.setText("Wait 10 seconds");
            progressBar.setProgress(0);
            progressBar.setVisibility(android.view.View.VISIBLE);
            closeAdButton.setVisibility(android.view.View.GONE);
            isAdCompleted = false;

            startTimer(10);
        });
    }

    // ✅ Timer Logic
    private void startTimer(int seconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int remaining = (int) (millisUntilFinished / 1000);
                timerText.setText("Wait " + remaining + "s");
                progressBar.setProgress((10 - remaining) * 10);
            }

            @Override
            public void onFinish() {
                timerText.setText("✅ Tap to view ad");
                progressBar.setProgress(100);
                isAdCompleted = true;
                closeAdButton.setVisibility(android.view.View.VISIBLE);
                closeAdButton.setText("View Ad & Share Image");
            }
        }.start();
    }

    // ✅ Open Adsterra Link in Browser & Share Image
    public void openAdAndShare(android.view.View view) {
        if (!isAdCompleted) {
            Toast.makeText(this, "Please wait for timer", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // ✅ Open Adsterra link in external browser (Policy Compliant)
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ADSTERRA_LINK));
        startActivity(browserIntent);

        // Close ad overlay and share image after returning
        adContainer.setVisibility(android.view.View.GONE);
        webView.setVisibility(android.view.View.VISIBLE);

        if (!currentImageData.isEmpty()) {
            shareImageToWhatsApp(currentImageData);
        } else {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Share blob image from WebView
    private void shareBlobImage(String blobUrl) {
        Toast.makeText(this, "Preparing image...", Toast.LENGTH_SHORT).show();
        String js = "javascript:(function() {" +
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '" + blobUrl + "', true);" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function() {" +
                "    if (this.status === 200) {" +
                "        var reader = new FileReader();" +
                "        reader.onloadend = function() {" +
                "            var base64 = reader.result.split(',')[1];" +
                "            Android.shareBase64Image(base64);" +
                "        };" +
                "        reader.readAsDataURL(this.response);" +
                "    }" +
                "};" +
                "xhr.send();" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ JavaScript for long-press
    private void injectImageShareScript() {
        String js = "javascript:(function() {" +
                "document.addEventListener('contextmenu', function(e) {" +
                "    var target = e.target;" +
                "    while (target && target.tagName !== 'IMG') {" +
                "        target = target.parentNode;" +
                "    }" +
                "    if (target && target.tagName === 'IMG') {" +
                "        e.preventDefault();" +
                "        var src = target.src;" +
                "        if (src.startsWith('blob:')) {" +
                "            Android.shareBlob(src);" +
                "        }" +
                "        return false;" +
                "    }" +
                "}, true);" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ Share Image to WhatsApp
    private void shareImageToWhatsApp(String base64Data) {
        try {
            byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "shared_image.png");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            shareIntent.setPackage("com.whatsapp");

            if (shareIntent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                Intent chooser = Intent.createChooser(shareIntent, "Share Image");
                startActivity(chooser);
            } else {
                startActivity(shareIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ WebAppInterface for JavaScript
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void shareBlob(String blobUrl) {
            runOnUiThread(() -> shareBlobImage(blobUrl));
        }

        @android.webkit.JavascriptInterface
        public void shareBase64Image(String base64Data) {
            runOnUiThread(() -> {
                currentImageData = base64Data;
                // ✅ Show Adsterra Ad before sharing
                showAdsterraAd();
            });
        }

        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (adContainer.getVisibility() == android.view.View.VISIBLE) {
            Toast.makeText(this, "Please complete the ad", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
