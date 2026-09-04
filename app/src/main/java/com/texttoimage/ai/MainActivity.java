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

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ImageDownloadHelper downloadHelper;
    private boolean adLoaded = false;

    // ✅ Unity Ads IDs (Aap ki IDs)
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

        // ✅ Unity Ads v4+ Initialization
        UnityAds.initialize(this, GAME_ID, true, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Toast.makeText(MainActivity.this, "✅ Unity Ads Initialized", Toast.LENGTH_SHORT).show();
                loadInterstitialAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                Toast.makeText(MainActivity.this, "❌ Init Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });

        setupWebView();
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!adLoaded) {
                    loadInterstitialAd();
                }
            }
        });

        // ✅ WebView Settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

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
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsFinishState state) {
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

            @Override
            public void onUnityAdsAdLoadFailed(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Rewarded Load Failed: " + message, Toast.LENGTH_SHORT).show();
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
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsFinishState state) {
                if (state == UnityAds.UnityAdsFinishState.COMPLETED) {
                    Toast.makeText(MainActivity.this, "🎉 Reward Earned!", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
