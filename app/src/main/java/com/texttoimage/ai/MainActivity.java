package com.texttoimage.ai;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ✅ Unity Ads Imports
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class MainActivity extends AppCompatActivity implements IUnityAdsListener {

    private WebView webView;

    // ✅ Unity Ads IDs
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Unity Ads Initialize (FIXED)
        UnityAds.initialize(this, GAME_ID, true);

        // WebView Setup
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");

        // ✅ WebView load hone ke baad Interstitial Ad Show
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showInterstitialAd();
            }
        });
    }

    // ✅ Show Interstitial Ad
    private void showInterstitialAd() {
        if (UnityAds.isReady(INTERSTITIAL_PLACEMENT)) {
            UnityAds.show(this, INTERSTITIAL_PLACEMENT);
        } else {
            Toast.makeText(this, "Ad not ready", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Unity Ads Listeners
    @Override
    public void onUnityAdsReady(String placementId) {}

    @Override
    public void onUnityAdsStart(String placementId) {}

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (placementId.equals(INTERSTITIAL_PLACEMENT)) {
            if (finishState == UnityAds.FinishState.COMPLETED) {
                Toast.makeText(this, "Ad Completed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onUnityAdsError(UnityAdsError error, String message) {
        Toast.makeText(this, "Ad Error: " + message, Toast.LENGTH_SHORT).show();
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
        UnityAds.removeListener(this);
        super.onDestroy();
    }
}
