package com.aurora.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    public class AndroidBridge {
        @JavascriptInterface
        public void enterPip() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 26) {
                        try {
                            Class<?> rationalClass = Class.forName("android.util.Rational");
                            Constructor<?> rationalCons = rationalClass.getConstructor(int.class, int.class);
                            Object rationalObj = rationalCons.newInstance(16, 9);

                            Class<?> builderClass = Class.forName("android.app.PictureInPictureParams$Builder");
                            Object builderObj = builderClass.newInstance();
                            Method setAspectMethod = builderClass.getMethod("setAspectRatio", rationalClass);
                            setAspectMethod.invoke(builderObj, rationalObj);
                            Method buildMethod = builderClass.getMethod("build");
                            Object paramsObj = buildMethod.invoke(builderObj);

                            Class<?> paramsClass = Class.forName("android.app.PictureInPictureParams");
                            Method enterPipMethod = Activity.class.getMethod("enterPictureInPictureMode", paramsClass);
                            enterPipMethod.invoke(MainActivity.this, paramsObj);
                            return;
                        } catch (Exception ignored) {}
                    }
                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            Method enterPipMethod = Activity.class.getMethod("enterPictureInPictureMode");
                            enterPipMethod.invoke(MainActivity.this);
                        } catch (Exception ex) {
                            Log.e("AuroraPlayer", "PiP reflection error", ex);
                        }
                    }
                }
            });
        }

        @JavascriptInterface
        public void toggleFullscreen() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during video playback
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        setContentView(webView);

        // Set background to dark immediately to avoid any white flash
        webView.setBackgroundColor(0xFF0D0E12);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);

        // Expose Android bridge to JavaScript
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("AuroraJS", consoleMessage.message() + " ["
                        + consoleMessage.sourceId() + ":"
                        + consoleMessage.lineNumber() + "]");
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);

                ViewGroup decor = (ViewGroup) getWindow().getDecorView();
                decor.addView(customView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                onHideCustomViewInternal();
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Allow HTTPS streams with self-signed or intermediate certificates
                handler.proceed();
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                String path = uri.getPath();

                // 1. Intercept /api/proxy-stream natively on Android (no CORS or server needed)
                if (path != null && path.startsWith("/api/proxy-stream")) {
                    String targetUrl = uri.getQueryParameter("url");
                    if (targetUrl != null && !targetUrl.isEmpty()) {
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
                            conn.setConnectTimeout(10000);
                            conn.setReadTimeout(15000);
                            conn.setInstanceFollowRedirects(true);

                            // Forward Range header if present
                            Map<String, String> reqHeaders = request.getRequestHeaders();
                            if (reqHeaders != null) {
                                for (Map.Entry<String, String> entry : reqHeaders.entrySet()) {
                                    if ("range".equalsIgnoreCase(entry.getKey())) {
                                        conn.setRequestProperty("Range", entry.getValue());
                                    }
                                }
                            }

                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                            conn.setRequestProperty("Accept", "*/*");

                            // Forward custom headers from query
                            String headersJson = uri.getQueryParameter("headers");
                            if (headersJson != null && !headersJson.isEmpty()) {
                                try {
                                    JSONObject jsonObj = new JSONObject(headersJson);
                                    Iterator<String> keys = jsonObj.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        conn.setRequestProperty(key, jsonObj.optString(key));
                                    }
                                } catch (Exception ignored) {}
                            }

                            int responseCode = conn.getResponseCode();
                            String contentType = conn.getContentType();
                            if (contentType == null) contentType = "application/octet-stream";

                            Map<String, String> responseHeaders = new HashMap<>();
                            responseHeaders.put("Access-Control-Allow-Origin", "*");
                            responseHeaders.put("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
                            responseHeaders.put("Access-Control-Allow-Headers", "*");

                            // Forward critical streaming headers
                            String contentRange = conn.getHeaderField("Content-Range");
                            if (contentRange != null) responseHeaders.put("Content-Range", contentRange);
                            String acceptRanges = conn.getHeaderField("Accept-Ranges");
                            if (acceptRanges != null) responseHeaders.put("Accept-Ranges", acceptRanges);

                            // M3U8 Playlist rewriting
                            boolean isM3u8 = contentType.contains("mpegurl") || contentType.contains("x-mpegURL") || targetUrl.toLowerCase().contains(".m3u8");
                            if (isM3u8) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                StringBuilder sb = new StringBuilder();
                                String line;
                                String headersParam = headersJson != null ? "&headers=" + Uri.encode(headersJson) : "";
                                while ((line = reader.readLine()) != null) {
                                    String trimmed = line.trim();
                                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                                        try {
                                            String resolved = new URL(new URL(targetUrl), trimmed).toString();
                                            line = "https://localhost/api/proxy-stream?url=" + Uri.encode(resolved) + headersParam;
                                        } catch (Exception ignored) {}
                                    }
                                    sb.append(line).append("\n");
                                }
                                byte[] bytes = sb.toString().getBytes("UTF-8");
                                return new WebResourceResponse("application/vnd.apple.mpegurl", "UTF-8", responseCode, "OK", responseHeaders, new ByteArrayInputStream(bytes));
                            }

                            return new WebResourceResponse(contentType, "UTF-8", responseCode, "OK", responseHeaders, conn.getInputStream());
                        } catch (Exception e) {
                            Log.e("AuroraProxy", "Proxy error: " + targetUrl, e);
                        }
                    }
                }

                // 2. Intercept /api/fetch-playlist natively
                if (path != null && path.startsWith("/api/fetch-playlist")) {
                    String targetUrl = uri.getQueryParameter("url");
                    if (targetUrl != null && !targetUrl.isEmpty()) {
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
                            conn.setConnectTimeout(8000);
                            conn.setReadTimeout(12000);
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            JSONObject result = new JSONObject();
                            result.put("content", sb.toString());
                            result.put("status", conn.getResponseCode());

                            Map<String, String> responseHeaders = new HashMap<>();
                            responseHeaders.put("Access-Control-Allow-Origin", "*");
                            return new WebResourceResponse("application/json", "UTF-8", 200, "OK", responseHeaders, new ByteArrayInputStream(result.toString().getBytes("UTF-8")));
                        } catch (Exception e) {
                            Log.e("AuroraProxy", "Fetch playlist error: " + targetUrl, e);
                        }
                    }
                }

                // 3. Intercept local assets (HTML, JS, CSS, PNG, SVG, fonts, etc.)
                if ("localhost".equals(host) || "appassets.androidplatform.net".equals(host)) {
                    try {
                        if (path == null || path.isEmpty() || "/".equals(path)) {
                            path = "/index.html";
                        }
                        if (path.startsWith("/")) {
                            path = path.substring(1);
                        }
                        String assetPath = "dist/" + path;
                        InputStream is = getAssets().open(assetPath);
                        String mimeType = "text/plain";
                        if (path.endsWith(".html")) mimeType = "text/html";
                        else if (path.endsWith(".js")) mimeType = "application/javascript";
                        else if (path.endsWith(".css")) mimeType = "text/css";
                        else if (path.endsWith(".png")) mimeType = "image/png";
                        else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) mimeType = "image/jpeg";
                        else if (path.endsWith(".svg")) mimeType = "image/svg+xml";
                        else if (path.endsWith(".json") || path.endsWith(".webmanifest")) mimeType = "application/json";

                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
                        headers.put("Access-Control-Allow-Headers", "*");

                        return new WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, is);
                    } catch (Exception e) {
                        Log.e("AuroraPlayer", "Error loading asset: " + uri, e);
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }
        });

        // Load the local app on https://localhost/
        webView.loadUrl("https://localhost/index.html");
    }

    private void onHideCustomViewInternal() {
        if (customView == null) return;
        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        decor.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            onHideCustomViewInternal();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
