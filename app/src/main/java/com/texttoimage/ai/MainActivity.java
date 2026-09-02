package com.texttoimage.ai;

import android.os.Bundle;
import android.webkit.WebView;          // ✅ Yeh sahi hai
import android.webkit.WebViewClient;    // ✅ Yeh sahi hai
import androidx.appcompat.app.AppCompatActivity;  // ✅ Yeh sahi hai

public class MainActivity extends AppCompatActivity {

    private WebView webView;  // ✅ Variable declare karna zaroori hai!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // 🔥 APNI WEBSITE KA URL
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
