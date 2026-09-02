package com.texttoimage.ai;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ✅ Unity Ads v4+ Correct Imports
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private boolean isAdShowing = false;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize Unity Ads
        initializeUnityAds();

        // WebView Setup
        setupWebView();
    }

    /**
     * Initialize Unity Ads v4+
     */
    private void initializeUnityAds() {
        UnityAds.initialize(this, GAME_ID, false, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Toast.makeText(MainActivity.this, "Unity Ads Initialized", Toast.LENGTH_SHORT).show();
                // Load ad after initialization
                loadInterstitialAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                Toast.makeText(MainActivity.this, "Init Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Setup WebView
     */
    private void setupWebView() {
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Load ad after page finish (but only if not already showing)
                if (!isAdShowing) {
                    loadInterstitialAd();
                }
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    /**
     * Load Interstitial Ad - Unity Ads v4+ Compatible
     */
    private void loadInterstitialAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();

        UnityAds.load(INTERSTITIAL_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                Toast.makeText(MainActivity.this, "Ad Loaded Successfully", Toast.LENGTH_SHORT).show();
                showInterstitialAd();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Ad Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show Interstitial Ad - Unity Ads v4+ Compatible
     * Note: v4+ removed UnityAdsFinishState, now only onUnityAdsShowComplete
     */
    private void showInterstitialAd() {
        isAdShowing = true;
        UnityAdsShowOptions showOptions = new UnityAdsShowOptions();

        UnityAds.show(MainActivity.this, INTERSTITIAL_PLACEMENT, showOptions, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                isAdShowing = false;
                Toast.makeText(MainActivity.this, "Ad Show Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {
                Toast.makeText(MainActivity.this, "Ad Started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowClick(String placementId) {
                Toast.makeText(MainActivity.this, "Ad Clicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowComplete(String placementId) {
                // v4+ API - No state parameter, just onUnityAdsShowComplete
                isAdShowing = false;
                Toast.makeText(MainActivity.this, "Ad Completed!", Toast.LENGTH_SHORT).show();
                // Load next ad after a delay to avoid rapid loading
                loadInterstitialAd();
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
