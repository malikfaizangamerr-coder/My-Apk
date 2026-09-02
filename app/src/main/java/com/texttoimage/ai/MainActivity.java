package com.texttoimage.ai;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ✅ Unity Ads Imports (Latest SDK)
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.load.IUnityAdsLoadListener;
import com.unity3d.ads.load.UnityAdsLoadOptions;
import com.unity3d.ads.show.IUnityAdsShowListener;
import com.unity3d.ads.show.UnityAdsShowOptions;
import com.unity3d.ads.UnityAdsShowError;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Unity Ads Initialize
        UnityAds.initialize(this, GAME_ID, true);

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
        
        // ✅ Aapki Website URL
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    // ✅ Ad Load Method
    private void loadInterstitialAd() {
        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
        UnityAds.load(INTERSTITIAL_PLACEMENT, loadOptions, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                // Ad load ho gayi, ab show karein
                showInterstitialAd();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Toast.makeText(MainActivity.this, "Ad Load Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Ad Show Method
    private void showInterstitialAd() {
        UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
        UnityAds.show(this, INTERSTITIAL_PLACEMENT, showOptions, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
                Toast.makeText(MainActivity.this, "Ad Show Failed: " + message, Toast.LENGTH_SHORT).show();
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
