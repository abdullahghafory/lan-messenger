package com.lanmessenger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "lan_messenger";
    private static final String KEY_URL = "server_url";
    private static final String DEFAULT_URL = "http://192.168.1.9:8080/";
    private WebView web;
    private SharedPreferences prefs;
    private boolean connectionDialogShown = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildWebView();
        loadServer();
    }

    private void buildWebView() {
        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        Button settings = new Button(this);
        settings.setText("⚙");
        settings.setTextSize(18);
        settings.setOnClickListener(v -> changeServer());
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(56, 56, android.view.Gravity.TOP | android.view.Gravity.END);
        sp.setMargins(0, 32, 12, 0);
        root.addView(settings, sp);
        setContentView(root);
        web.setBackgroundColor(0xFFFFFFFF);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setBuiltInZoomControls(false);
        web.getSettings().setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    showServerDialog(true);
                }
            }
        });
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception e) { Toast.makeText(this, "باز کردن فایل ممکن نیست", Toast.LENGTH_SHORT).show(); }
        });
        web.setOnLongClickListener(v -> false);
    }

    private void loadServer() {
        String url = prefs.getString(KEY_URL, DEFAULT_URL);
        if (url == null || url.trim().isEmpty()) url = DEFAULT_URL;
        web.loadUrl(normalize(url));
    }

    private String normalize(String url) {
        url = url.trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) url = "http://" + url;
        if (!url.endsWith("/")) url += "/";
        return url;
    }

    private void showServerDialog(boolean unavailable) {
        if (isFinishing() || connectionDialogShown) return;
        connectionDialogShown = true;

        final EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("مثلاً 192.168.1.9");

        final EditText port = new EditText(this);
        port.setSingleLine(true);
        port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        port.setHint("مثلاً 8080");
        port.setText(extractPort(prefs.getString(KEY_URL, DEFAULT_URL)));

        String saved = prefs.getString(KEY_URL, DEFAULT_URL);
        address.setText(extractHost(saved));
        address.setSelectAllOnFocus(true);

        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, 4, pad, 0);
        box.addView(address, new LinearLayout.LayoutParams(-1, -2));
        box.addView(port, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(unavailable ? "سرور پیدا نشد" : "آدرس سرور LAN")
            .setMessage(unavailable
                ? "به سرور قبلی دسترسی نیست. آدرس و پورت سرور داخل شبکه را وارد کنید."
                : "آدرس و پورت سرور داخل شبکه را وارد کنید.")
            .setView(box);

        if (unavailable) {
            builder.setNegativeButton("تلاش مجدد", (d, w) -> {
                connectionDialogShown = false;
                loadServer();
            });
        } else {
            builder.setNegativeButton("انصراف", null);
        }

        AlertDialog dialog = builder
            .setPositiveButton("اتصال", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String host = address.getText().toString().trim();
            String p = port.getText().toString().trim();
            if (host.isEmpty()) {
                address.setError("آدرس سرور را وارد کنید");
                return;
            }
            if (p.isEmpty()) p = "80";
            try {
                int portNumber = Integer.parseInt(p);
                if (portNumber < 1 || portNumber > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                port.setError("پورت باید بین 1 تا 65535 باشد");
                return;
            }
            String u = normalize(host + ":" + p);
            prefs.edit().putString(KEY_URL, u).apply();
            connectionDialogShown = false;
            dialog.dismiss();
            web.loadUrl(u);
        }));
        dialog.setOnDismissListener(d -> connectionDialogShown = false);
        dialog.show();
    }

    private void changeServer() {
        showServerDialog(false);
    }

    private String extractHost(String url) {
        try {
            Uri u = Uri.parse(normalize(url));
            String host = u.getHost();
            return host == null ? "" : host;
        } catch (Exception e) {
            return "";
        }
    }

    private String extractPort(String url) {
        try {
            Uri u = Uri.parse(normalize(url));
            int port = u.getPort();
            if (port > 0) return String.valueOf(port);
            return "80";
        } catch (Exception e) {
            return "8080";
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

}
