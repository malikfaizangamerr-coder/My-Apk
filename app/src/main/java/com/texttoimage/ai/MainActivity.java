package com.texttoimage.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.startio.sdk.StartIO;
import com.startio.sdk.ads.banner.BannerView;
import com.startio.sdk.ads.interstitial.InterstitialAd;
import com.startio.sdk.ads.interstitial.InterstitialAdListener;
import com.startio.sdk.ads.rewarded.RewardedAd;
import com.startio.sdk.ads.rewarded.RewardedAdListener;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageDownloadHelper downloadHelper;
    private boolean interstitialAdLoaded = false;
    private boolean rewardedAdLoaded = false;
    private String currentImageUrl = "";

    // ✅ Start.io Ad IDs
    private final String INTERSTITIAL_AD_ID = "210998353"; // Interstitial ad unit
    private final String REWARDED_AD_ID = "210998354";    // Rewarded ad unit
    private final String BANNER_AD_ID = "210998352";      // Banner ad unit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadHelper = new ImageDownloadHelper(this);

        // Storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }

        // ✅ Start.io SDK Initialize
        StartIO.init(this);

        // ✅ Load Interstitial Ad
        loadInterstitialAd();

        // ✅ Show Interstitial after a short delay (app open)
        new android.os.Handler().postDelayed(this::showInterstitialAd, 2000);

        // ✅ Setup WebView
        setupWebView();

        // ✅ Setup Banner Ad (Top)
        setupBannerAd();
    }

    // ✅ Setup Banner Ad
    private void setupBannerAd() {
        BannerView bannerView = new BannerView(this);
        bannerView.setAdUnitId(BANNER_AD_ID);
        bannerView.setBannerListener(new com.startio.sdk.ads.banner.BannerListener() {
            @Override
            public void onAdLoaded() {
                // Banner loaded
            }

            @Override
            public void onAdFailedToLoad(String error) {
                Toast.makeText(MainActivity.this, "Banner failed: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdClicked() {}

            @Override
            public void onAdImpression() {}
        });

        LinearLayout bannerContainer = findViewById(R.id.bannerContainer);
        if (bannerContainer != null) {
            bannerContainer.addView(bannerView);
            bannerView.loadAd();
        }
    }

    // ✅ Setup WebView
    private void setupWebView() {
        webView = findViewById(R.id.webView);

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
                // Load interstitial after page load (if not shown already)
                if (!interstitialAdLoaded) {
                    loadInterstitialAd();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url);
                // Hide webview while loading (optional)
            }
        });

        // ✅ Share instead of download (blob URLs)
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url != null && url.startsWith("blob:")) {
                // ✅ Share blob image via JavaScript
                shareBlobImage(url);
            } else {
                Toast.makeText(MainActivity.this, "Share not supported", Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Share blob image via JavaScript
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

    // ✅ JavaScript for image share
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

    // ✅ Show Interstitial Ad
    private void showInterstitialAd() {
        if (interstitialAdLoaded) {
            InterstitialAd.show(this);
        }
    }

    // ✅ Show Rewarded Ad (called when user wants to share image)
    private void showRewardedAdForShare(final String imageData) {
        if (rewardedAdLoaded) {
            // Show Rewarded Ad
            RewardedAd.show(this, new RewardedAdListener() {
                @Override
                public void onAdRewarded() {
                    // ✅ User watched full ad, now share image
                    shareImageToWhatsApp(imageData);
                }

                @Override
                public void onAdFailedToShow(String error) {
                    Toast.makeText(MainActivity.this, "Ad failed, sharing anyway...", Toast.LENGTH_SHORT).show();
                    shareImageToWhatsApp(imageData);
                }

                @Override
                public void onAdClosed() {
                    // Ad closed without reward
                }
            });
        } else {
            // If ad not loaded, share directly (or reload and try again)
            Toast.makeText(this, "Ad not ready, sharing directly...", Toast.LENGTH_SHORT).show();
            shareImageToWhatsApp(imageData);
            loadRewardedAd();
        }
    }

    // ✅ Share Image to WhatsApp via Intent
    private void shareImageToWhatsApp(String base64Data) {
        try {
            byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to cache file
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "shared_image.png");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            // Share intent
            Uri imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Optional: Force WhatsApp
            shareIntent.setPackage("com.whatsapp");

            // If WhatsApp not installed, show chooser
            if (shareIntent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                // Fallback to generic share
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

    // ✅ Start.io: Load Interstitial
    private void loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_ID, new InterstitialAdListener() {
            @Override
            public void onAdLoaded() {
                interstitialAdLoaded = true;
            }

            @Override
            public void onAdFailedToLoad(String error) {
                interstitialAdLoaded = false;
                Toast.makeText(MainActivity.this, "Interstitial failed: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdShown() {}

            @Override
            public void onAdClicked() {}

            @Override
            public void onAdClosed() {
                interstitialAdLoaded = false;
                // Load next interstitial
                loadInterstitialAd();
            }
        });
    }

    // ✅ Start.io: Load Rewarded Ad
    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_AD_ID, new RewardedAdListener() {
            @Override
            public void onAdLoaded() {
                rewardedAdLoaded = true;
            }

            @Override
            public void onAdFailedToLoad(String error) {
                rewardedAdLoaded = false;
                Toast.makeText(MainActivity.this, "Rewarded failed: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdShown() {}

            @Override
            public void onAdClicked() {}

            @Override
            public void onAdClosed() {
                rewardedAdLoaded = false;
                loadRewardedAd();
            }

            @Override
            public void onAdRewarded() {}
        });
    }

    // ✅ WebAppInterface
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void shareBlob(String blobUrl) {
            runOnUiThread(() -> shareBlobImage(blobUrl));
        }

        @android.webkit.JavascriptInterface
        public void shareBase64Image(String base64Data) {
            runOnUiThread(() -> {
                currentImageUrl = base64Data;
                // ✅ Show Rewarded Ad before sharing
                if (rewardedAdLoaded) {
                    showRewardedAdForShare(base64Data);
                } else {
                    // Load rewarded and show
                    loadRewardedAd();
                    Toast.makeText(MainActivity.this, "Loading ad, please wait...", Toast.LENGTH_SHORT).show();
                    // Try again after 3 seconds
                    new android.os.Handler().postDelayed(() -> {
                        showRewardedAdForShare(base64Data);
                    }, 3000);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
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
        super.onDestroy();
    }
}
