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

    // ✅ Unity Ads IDs (Aap ki IDs)
    private final String GAME_ID = "6184303";
    private final String INTERSTITIAL_PLACEMENT = "Interstitial_Android";
    private final String REWARDED_PLACEMENT = "Rewarded_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Unity Ads Initialize
        UnityAds.initialize(this, GAME_ID, this, true);

        // WebView Setup
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");

        // ✅ WebView load hone ke baad Interstitial Ad Show Karein
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

    // ✅ Show Rewarded Ad (Optional - agar use karna ho toh)
    private void showRewardedAd() {
        if (UnityAds.isReady(REWARDED_PLACEMENT)) {
            UnityAds.show(this, REWARDED_PLACEMENT);
        } else {
            Toast.makeText(this, "Rewarded Ad not ready", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Unity Ads Listeners
    @Override
    public void onUnityAdsReady(String placementId) {
        // Ad ready hai
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        // Ad start hui
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (placementId.equals(INTERSTITIAL_PLACEMENT)) {
            if (finishState == UnityAds.FinishState.COMPLETED) {
                Toast.makeText(this, "Interstitial Ad Completed!", Toast.LENGTH_SHORT).show();
            } else if (finishState == UnityAds.FinishState.SKIPPED) {
                Toast.makeText(this, "Interstitial Ad Skipped", Toast.LENGTH_SHORT).show();
            }
        } else if (placementId.equals(REWARDED_PLACEMENT)) {
            if (finishState == UnityAds.FinishState.COMPLETED) {
                Toast.makeText(this, "Rewarded Ad Completed! 🎉", Toast.LENGTH_SHORT).show();
                // ✅ Yahan user ko reward dein
            } else {
                Toast.makeText(this, "Rewarded Ad not completed", Toast.LENGTH_SHORT).show();
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
