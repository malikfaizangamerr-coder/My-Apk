package com.texttoimage.ai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageDownloadHelper downloadHelper;
    private boolean adLoaded = false;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android"; // Verify this exact string
    private final String REWARDED_PLACEMENT = "Rewarded_Android";
    private final String BANNER_PLACEMENT = "Banner_Android";

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

        // ✅ Unity Ads v4+ Initialization (testMode = true for debugging)
        UnityAds.initialize(this, GAME_ID, true, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Toast.makeText(MainActivity.this, "Unity Ads Initialized (Test Mode)", Toast.LENGTH_SHORT).show();
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

        // WebView Settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        // ✅ JavaScript Interface for Blob URL Download
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!adLoaded) {
                    loadInterstitialAd();
                }
                // ✅ Inject JavaScript to intercept image long-press (robust)
                injectImageDownloadScript();
            }

            // ❌ No need to override shouldOverrideUrlLoading for blob: because we handle in DownloadListener
            // ❌ shouldInterceptRequest does NOT work for blob: URLs, so we remove it.
        });

        // ✅ Updated DownloadListener: handle blob: URLs
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url == null) {
                Toast.makeText(MainActivity.this, "Invalid download URL", Toast.LENGTH_SHORT).show();
                return;
            }

            if (url.startsWith("blob:")) {
                downloadBlobImage(url);
            } else if (url.startsWith("http")) {
                String fileName = downloadHelper.generateFileName(url);
                downloadHelper.downloadImageToGallery(url, fileName);
            } else {
                Toast.makeText(MainActivity.this, "Download not supported for this URL type", Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Improved JavaScript injection with MutationObserver and document-level listener
    private void injectImageDownloadScript() {
        String js = "javascript:(function() {" +
                "if (window._imageDownloadInjected) return; window._imageDownloadInjected = true;" +
                "document.addEventListener('contextmenu', function(e) {" +
                "    var target = e.target;" +
                "    while (target && target.tagName !== 'IMG') {" +
                "        target = target.parentNode;" +
                "    }" +
                "    if (target && target.tagName === 'IMG') {" +
                "        e.preventDefault();" +
                "        var src = target.src;" +
                "        if (src.startsWith('blob:')) {" +
                "            Android.downloadBlob(src);" +
                "        } else {" +
                "            Android.downloadImage(src, 'image_' + Date.now() + '.png');" +
                "        }" +
                "        return false;" +
                "    }" +
                "}, true);" +
                "// Also handle dynamically added images" +
                "var observer = new MutationObserver(function(mutations) {" +
                "    mutations.forEach(function(mutation) {" +
                "        mutation.addedNodes.forEach(function(node) {" +
                "            if (node.tagName === 'IMG') {" +
                "                // already covered by document listener" +
                "            }" +
                "        });" +
                "    });" +
                "});" +
                "observer.observe(document.body, { childList: true, subtree: true });" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ Handle blob: URLs with improved fetch (using JavaScript to get base64)
    private void downloadBlobImage(String blobUrl) {
        Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show();

        // Use JavaScript to fetch blob and pass base64 to Android interface
        String js = "javascript:(function() {" +
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '" + blobUrl + "', true);" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function() {" +
                "    if (this.status === 200) {" +
                "        var reader = new FileReader();" +
                "        reader.onloadend = function() {" +
                "            var base64 = reader.result.split(',')[1];" +
                "            Android.downloadBase64Image(base64, 'image_' + Date.now() + '.png');" +
                "        };" +
                "        reader.readAsDataURL(this.response);" +
                "    } else {" +
                "        Android.showToast('Failed to fetch image');" +
                "    }" +
                "};" +
                "xhr.onerror = function() {" +
                "    Android.showToast('Network error');" +
                "};" +
                "xhr.send();" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ Unity Ads: Load Interstitial (only two methods)
    private void loadInterstitialAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(INTERSTITIAL_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                adLoaded = true;
                showInterstitialAd();
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

    // ✅ Unity Ads: Load Rewarded (optional)
    private void loadRewardedAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(REWARDED_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                showRewardedAd();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Failed to Load: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Unity Ads: Show Rewarded (optional)
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

    // ✅ JavaScript Interface - Added new method for base64 download
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void downloadImage(String imageUrl, String fileName) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                runOnUiThread(() -> {
                    if (imageUrl.startsWith("http")) {
                        downloadHelper.downloadImageToGallery(imageUrl, fileName);
                    } else {
                        Toast.makeText(MainActivity.this, "Unsupported URL type", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @android.webkit.JavascriptInterface
        public void downloadBlob(String blobUrl) {
            runOnUiThread(() -> downloadBlobImage(blobUrl));
        }

        // ✅ New method: receive base64 image data directly from JavaScript
        @android.webkit.JavascriptInterface
        public void downloadBase64Image(String base64Data, String fileName) {
            if (base64Data != null && !base64Data.isEmpty()) {
                try {
                    byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    downloadHelper.saveImageToGalleryDirect(imageBytes, fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Base64 decode error", Toast.LENGTH_SHORT).show());
                }
            }
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
