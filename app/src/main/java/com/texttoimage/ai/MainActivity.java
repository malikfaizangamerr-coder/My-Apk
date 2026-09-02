package com.texttoimage.ai;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ✅ Unity Ads v4+ Imports
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsInitializationListener;
import com.unity3d.ads.mediation.IUnityAdsLoadListener;
import com.unity3d.ads.mediation.IUnityAdsShowListener;
import com.unity3d.ads.mediation.UnityAdsLoadOptions;
import com.unity3d.ads.mediation.UnityAdsShowOptions;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Unity Ads v4+ Initialization with Listener
        UnityAds.initialize(this, GAME_ID, false, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                // SDK initialized successfully
                Toast.makeText(MainActivity.this, "Unity Ads Initialized", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                Toast.makeText(MainActivity.this, "Init Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        // WebView Setup
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // ✅ Page load hone ke baad Ad Load karein
                loadInterstitialAd();
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Ad Load Method (v4+)
    private void loadInterstitialAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(INTERSTITIAL_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                // Ad load ho gayi, ab show karein
                showInterstitialAd();
            }

            @Override
            public void onUnityAdsAdLoadFailed(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Ad Show Method (v4+)
    private void showInterstitialAd() {
        UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
        UnityAds.show(this, INTERSTITIAL_PLACEMENT, showOptions, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Toast.makeText(MainActivity.this, "Show Failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {
                // Ad start ho gayi
            }

            @Override
            public void onUnityAdsShowClick(String placementId) {
                // User ne ad par click kiya
            }

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsFinishState state) {
                // Ad complete ho gayi
                if (state == UnityAds.UnityAdsFinishState.COMPLETED) {
                    Toast.makeText(MainActivity.this, "Ad Completed!", Toast.LENGTH_SHORT).show();
                }
                // Agli ad load karein
                loadInterstitialAd();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
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
