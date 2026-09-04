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

// ✅ Unity Ads v4+ Imports
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

// ✅ Correct import for ByteArrayInputStream from java.io
import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageDownloadHelper downloadHelper;
    private boolean adLoaded = false;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";
    private final String REWARDED_PLACEMENT = "Rewarded_Android";
    private final String BANNER_PLACEMENT = "Banner_Android";

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
                // ✅ Inject JavaScript to intercept image long-press
                injectImageDownloadScript();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("blob:")) {
                    downloadBlobImage(url);
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url != null && url.startsWith("blob:")) {
                    try {
                        byte[] data = fetchBlobData(url);
                        if (data != null) {
                            String mimeType = "image/png";
                            // ✅ FIXED: Using java.io.ByteArrayInputStream (not android.webkit.ByteArrayInputStream)
                            return new WebResourceResponse(mimeType, "UTF-8", new ByteArrayInputStream(data));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        // ✅ DownloadListener for normal HTTP downloads
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

    // ✅ Inject JavaScript to handle image long-press
    private void injectImageDownloadScript() {
        String js = "javascript:(function() {" +
                "var images = document.getElementsByTagName('img');" +
                "for (var i = 0; i < images.length; i++) {" +
                "    images[i].addEventListener('contextmenu', function(e) {" +
                "        e.preventDefault();" +
                "        var src = this.src;" +
                "        if (src.startsWith('blob:')) {" +
                "            Android.downloadBlob(src);" +
                "        } else {" +
                "            Android.downloadImage(src, 'image_' + Date.now() + '.png');" +
                "        }" +
                "        return false;" +
                "    });" +
                "}" +
                "})()";
        webView.loadUrl(js);
    }

    // ✅ Fetch blob data using OkHttp
    private byte[] fetchBlobData(String blobUrl) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(blobUrl).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().bytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Handle blob: URLs
    private void downloadBlobImage(String blobUrl) {
        Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                byte[] data = fetchBlobData(blobUrl);
                if (data != null) {
                    String fileName = "image_" + System.currentTimeMillis() + ".png";
                    downloadHelper.saveImageToGalleryDirect(data, fileName);
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download image", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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

            @Override
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

    // ✅ Unity Ads: Load Rewarded
    private void loadRewardedAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(REWARDED_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                showRewardedAd();
            }

            @Override
            public void onUnityAdsAdLoadFailed(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Failed to Load: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Unity Ads: Show Rewarded
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
