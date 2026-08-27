package ke.co.dasify.dasfund;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;

/**
 * DasFund mobile shell with an offline-first cached WebView experience.
 * A successful page is saved as a WebView archive. If the device has no
 * validated Internet connection at startup, the most recently saved page is
 * opened and an offline/sync banner is shown.
 */
public class MainActivity extends AppCompatActivity {
    private static final int FILE_CHOOSER = 1001;
    private static final String HOME = "https://dasify.co.ke/dasfund/";
    private static final String CACHE_DIR = "dasfund_webcache";
    private static final String LAST_ARCHIVE = "last_page.mht";

    private WebView webView;
    private SwipeRefreshLayout swipe;
    private TextView offlineBanner;
    private ValueCallback<Uri[]> uploadCallback;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean offlineMode;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);
        offlineBanner = findViewById(R.id.offlineBanner);
        configureWebView();

        swipe.setOnRefreshListener(() -> {
            if (hasValidatedInternet()) {
                webView.reload();
            } else {
                swipe.setRefreshing(false);
                showOfflineBanner(true);
            }
        });

        if (hasValidatedInternet()) {
            offlineMode = false;
            showOfflineBanner(false);
            webView.loadUrl(HOME);
        } else if (loadLastCachedPage()) {
            offlineMode = true;
            showOfflineBanner(true);
        } else {
            offlineMode = true;
            showOfflineBanner(true);
            webView.loadDataWithBaseURL(HOME,
                    "<html><body style='font-family:sans-serif;padding:24px'><h2>DasFund</h2><p>No cached information is available yet. Connect to the internet once to load your account.</p></body></html>",
                    "text/html", "UTF-8", HOME);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack(); else finish();
            }
        });

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }

        registerNetworkCallback();
    }

    private void configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " DasFundAndroid/1.1");

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                if (offlineMode && !hasValidatedInternet()) return true;
                return handleUrl(req.getUrl().toString());
            }

            @Override public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
                if (hasValidatedInternet()) {
                    offlineMode = false;
                    showOfflineBanner(false);
                    saveCurrentPageArchive();
                }
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    swipe.setRefreshing(false);
                    if (loadLastCachedPage()) {
                        offlineMode = true;
                        showOfflineBanner(true);
                    } else {
                        showOfflineBanner(true);
                        Toast.makeText(MainActivity.this, "No internet connection. No cached page is available yet.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                uploadCallback = cb;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (ActivityNotFoundException e) { uploadCallback = null; return false; }
                return true;
            }
        });
    }

    private void saveCurrentPageArchive() {
        if (isFinishing() || !hasValidatedInternet()) return;
        File dir = new File(getCacheDir(), CACHE_DIR);
        if (!dir.exists() && !dir.mkdirs()) return;
        File archive = new File(dir, LAST_ARCHIVE);
        webView.saveWebArchive(archive.getAbsolutePath(), false, filename -> {
            if (filename != null) {
                getSharedPreferences("offline", MODE_PRIVATE).edit()
                        .putString("last_archive", filename)
                        .putString("last_url", webView.getUrl())
                        .apply();
            }
        });
    }

    private boolean loadLastCachedPage() {
        String path = getSharedPreferences("offline", MODE_PRIVATE).getString("last_archive", null);
        if (path == null) {
            File fallback = new File(new File(getCacheDir(), CACHE_DIR), LAST_ARCHIVE);
            path = fallback.getAbsolutePath();
        }
        File archive = new File(path);
        if (!archive.exists() || archive.length() == 0) return false;
        try {
            webView.loadUrl(Uri.fromFile(archive).toString());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showOfflineBanner(boolean show) {
        offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean hasValidatedInternet() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || Build.VERSION.SDK_INT < 24) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                boolean online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                runOnUiThread(() -> {
                    if (online) {
                        offlineMode = false;
                        showOfflineBanner(false);
                        if (webView.getUrl() != null && webView.getUrl().startsWith("file:")) webView.loadUrl(HOME);
                    } else {
                        offlineMode = true;
                        showOfflineBanner(true);
                    }
                });
            }
            @Override public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (!hasValidatedInternet()) {
                        offlineMode = true;
                        showOfflineBanner(true);
                    }
                });
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();

        if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) &&
            (host.equalsIgnoreCase("dasify.co.ke") || host.endsWith(".dasify.co.ke"))) {
            return false;
        }

        if ("tel".equalsIgnoreCase(scheme) || "sms".equalsIgnoreCase(scheme) ||
            "mailto".equalsIgnoreCase(scheme) || "geo".equalsIgnoreCase(scheme)) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
            return true;
        }

        if ("intent".equalsIgnoreCase(scheme)) {
            try {
                Intent i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                startActivity(i);
            } catch (Exception ignored) {}
            return true;
        }

        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
        return true;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && uploadCallback != null) {
            Uri[] results = (resultCode == Activity.RESULT_OK && data != null)
                    ? WebChromeClient.FileChooserParams.parseResult(resultCode, data) : null;
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override protected void onDestroy() {
        if (connectivityManager != null && networkCallback != null && Build.VERSION.SDK_INT >= 24) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); } catch (Exception ignored) {}
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
