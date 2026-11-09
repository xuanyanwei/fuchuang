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

    private boolean isImmersiveFullscreen = false;

    private void applyImmersiveMode(final boolean enable) {
        isImmersiveFullscreen = enable;
        if (enable) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    android.view.WindowInsetsController insetsController = getWindow().getInsetsController();
                    if (insetsController != null) {
                        insetsController.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                        insetsController.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } catch (Throwable ignored) {}
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    android.view.WindowInsetsController insetsController = getWindow().getInsetsController();
                    if (insetsController != null) {
                        insetsController.show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

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
        public void setFullscreen(final boolean fullscreen) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fullscreen) {
                        applyImmersiveMode(true);
                    } else {
                        onHideCustomViewInternal();
                        applyImmersiveMode(false);
                    }
                }
            });
        }

        @JavascriptInterface
        public void exitFullscreen() {
            setFullscreen(false);
        }

        @JavascriptInterface
        public void requestFullscreen() {
            setFullscreen(true);
        }

        @JavascriptInterface
        public void toggleFullscreen() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        exitFullscreen();
                    } else {
                        requestFullscreen();
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

        // Allow drawing into display cutout / notch area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 && isImmersiveFullscreen) {
                    getWindow().getDecorView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isImmersiveFullscreen) {
                                applyImmersiveMode(true);
                            }
                        }
                    }, 1200);
                }
            }
        });

        webView = new WebView(this);
        setContentView(webView);

        // Set background to dark immediately to avoid any white flash
        webView.setBackgroundColor(0xFF0D0E12);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

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

                applyImmersiveMode(true);
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
                String path = uri.getPath();

                // 0. Intercept local asset files if requested via root-relative or absolute path
                if (path != null && (path.startsWith("/assets/") || path.contains("player-features.js") || path.contains("registerSW.js") || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".svg") || path.endsWith(".png") || path.endsWith(".webmanifest"))) {
                    try {
                        String cleanPath = path;
                        if (cleanPath.startsWith("/android_asset/dist/")) {
                            cleanPath = cleanPath.substring("/android_asset/dist/".length());
                        } else if (cleanPath.startsWith("/")) {
                            cleanPath = cleanPath.substring(1);
                        }
                        String assetPath = cleanPath.startsWith("dist/") ? cleanPath : ("dist/" + cleanPath);
                        InputStream is = getAssets().open(assetPath);
                        String mime = "application/octet-stream";
                        if (path.endsWith(".js")) mime = "application/javascript";
                        else if (path.endsWith(".css")) mime = "text/css";
                        else if (path.endsWith(".svg")) mime = "image/svg+xml";
                        else if (path.endsWith(".png")) mime = "image/png";
                        else if (path.endsWith(".json") || path.endsWith(".webmanifest")) mime = "application/json";
                        else if (path.endsWith(".html")) mime = "text/html";
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, is);
                    } catch (Exception ignored) {}
                }

                // 1. Intercept stream proxy natively on Android (no CORS or server needed)
                if (path != null && path.contains("/api/proxy-stream")) {
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
                if (path != null && path.contains("/api/fetch-playlist")) {
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

                return super.shouldInterceptRequest(view, request);
            }
        });

        // Load the local app directly from android assets (same native mechanism as MT Manager preview)
        webView.loadUrl("file:///android_asset/dist/index.html");
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
        applyImmersiveMode(false);
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isImmersiveFullscreen) {
            applyImmersiveMode(true);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (webView != null) {
            String script = isInPictureInPictureMode
                    ? "document.body.classList.add('pip-mode');"
                    : "document.body.classList.remove('pip-mode');";
            webView.evaluateJavascript(script, null);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            onHideCustomViewInternal();
            return;
        }
        if (webView != null) {
            webView.evaluateJavascript(
                "(function() { if (window.exitFullscreenIfActive && window.exitFullscreenIfActive()) { return 'handled'; } return 'no'; })()",
                new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"\"handled\"".equals(value) && !"handled".equals(value)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (webView != null && webView.canGoBack()) {
                                        webView.goBack();
                                    } else {
                                        MainActivity.super.onBackPressed();
                                    }
                                }
                            });
                        }
                    }
                }
            );
            return;
        }
        super.onBackPressed();
    }
}

