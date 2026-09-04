package com.texttoimage.ai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// ✅ Unity Ads v4+ Imports
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageDownloadHelper downloadHelper;
    private boolean adLoaded = false;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";
    private final String REWARDED_PLACEMENT = "Rewarded_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadHelper = new ImageDownloadHelper(this);

        // ✅ Storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }

        // ✅ Unity Ads v4+ Initialization (Real Ads - testMode = false)
        UnityAds.initialize(this, GAME_ID, false, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Toast.makeText(MainActivity.this, "Unity Ads Initialized", Toast.LENGTH_SHORT).show();
                loadInterstitialAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                Toast.makeText(MainActivity.this, "Init Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });

        setupWebView();
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);

        // ✅ WebView Settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // ✅ JavaScript Interface for Blob URL Download
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!adLoaded) {
                    loadInterstitialAd();
                }
            }

            // ✅ Intercept blob: URLs
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("blob:")) {
                    downloadBlobImage(url);
                    return true;
                }
                return false;
            }
        });

        // ✅ DownloadListener for HTTP downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url != null && url.startsWith("http")) {
                String fileName = downloadHelper.generateFileName(url);
                downloadHelper.downloadImageToGallery(url, fileName);
            } else {
                Toast.makeText(MainActivity.this, "Download not supported", Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Handle blob: URLs
    private void downloadBlobImage(String blobUrl) {
        Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show();
        // Inject JavaScript to fetch blob data
        String js = "javascript:(function() {" +
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '" + blobUrl + "', true);" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function(e) {" +
                "    if (this.status == 200) {" +
                "        var reader = new FileReader();" +
                "        reader.onloadend = function() {" +
                "            var base64 = reader.result.split(',')[1];" +
                "            Android.downloadImage(base64, 'image_' + Date.now() + '.png');" +
                "        };" +
                "        reader.readAsDataURL(this.response);" +
                "    }" +
                "};" +
                "xhr.send();" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ Unity Ads: Load Interstitial
    private void loadInterstitialAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(INTERSTITIAL_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                adLoaded = true;
                showInterstitialAd();
            }

            // ✅ No @Override annotation (fix)
            public void onUnityAdsAdLoadFailed(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                adLoaded = false;
                Toast.makeText(MainActivity.this, "Ad Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                adLoaded = false;
                Toast.makeText(MainActivity.this, "Ad Failed to Load: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Unity Ads: Show Interstitial
    private void showInterstitialAd() {
        UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
        UnityAds.show(MainActivity.this, INTERSTITIAL_PLACEMENT, showOptions, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                adLoaded = false;
                Toast.makeText(MainActivity.this, "Ad Show Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAdsShowCompletionState completionState) {
                adLoaded = false;
                loadInterstitialAd();
            }
        });
    }

    // ✅ Unity Ads: Load Rewarded (Optional)
    private void loadRewardedAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(REWARDED_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                showRewardedAd();
            }

            // ✅ No @Override annotation (fix)
            public void onUnityAdsAdLoadFailed(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Failed to Load: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Unity Ads: Show Rewarded (Optional)
    private void showRewardedAd() {
        UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
        UnityAds.show(MainActivity.this, REWARDED_PLACEMENT, showOptions, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Show Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAdsShowCompletionState completionState) {
                Toast.makeText(MainActivity.this, "🎉 Reward Earned!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ JavaScript Interface for Blob URL Download
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void downloadImage(String imageData, String fileName) {
            if (imageData != null && !imageData.isEmpty()) {
                try {
                    byte[] imageBytes = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT);
                    downloadHelper.saveImageToGalleryDirect(imageBytes, fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
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
