package nl.safescan.local;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.gecko.util.GeckoBundle;

/** Temporary engine validation host; SafeScan's production bridge remains unchanged until validated. */
public final class GeckoTestActivity extends MainActivity {
    private static final String TAG = "SafeScanGecko";
    private static final int SAVE_GECKO_PDF = 401;
    private static final int SAVE_GECKO_BACKUP = 402;
    private static final int OPEN_GECKO_BACKUP = 403;
    private byte[] geckoReport;
    private ServerSocket assetServer;
    private static GeckoRuntime sharedRuntime;
    private GeckoRuntime runtime;
    private GeckoSession session;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        i18n = new I18n(this);
        GeckoView view = new GeckoView(this);
        setContentView(view);
        if (sharedRuntime == null) sharedRuntime = GeckoRuntime.create(this.getApplicationContext());
        runtime = sharedRuntime;
        installBridge();
        session = new GeckoSession();
        view.setSession(session);
        session.open(runtime);
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onLocationChange(GeckoSession s, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> permissions, Boolean hasUserGesture) {
                if (url != null && url.startsWith("safescan://")) Log.i(TAG, "Bridge location: " + url);
            }
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession s, GeckoSession.NavigationDelegate.LoadRequest request) {
                if (request.uri != null && request.uri.startsWith("safescan://")) {
                    Log.i(TAG, "Bridge URL: " + request.uri);
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }
        });
        startAssetServer();
        session.loadUri("http://127.0.0.1:8765/site/index.html");
    }

    private void startAssetServer() {
        try {
            assetServer = new ServerSocket(8765, 8, java.net.InetAddress.getByName("127.0.0.1"));
            new Thread(() -> { while (!assetServer.isClosed()) try { serve(assetServer.accept()); } catch (Exception ignored) {} }, "SafeScanAssets").start();
        } catch (Exception e) { Log.e(TAG, "Asset server failed", e); }
    }

    private void serve(Socket socket) {
        try (Socket s = socket; InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream()) {
            byte[] request = new byte[2048]; int n = in.read(request); String line = new String(request, 0, Math.max(0, n), "US-ASCII");
            String path = "/site/index.html"; if (line.startsWith("GET ")) { int a = line.indexOf(' ') + 1, b = line.indexOf(' ', a); path = line.substring(a, b); }
            if (path.contains("..")) path = "/site/index.html";
            InputStream asset = getAssets().open(path.startsWith("/") ? path.substring(1) : path);
            String mime = path.endsWith(".html") ? "text/html" : path.endsWith(".js") ? "application/javascript" : path.endsWith(".css") ? "text/css" : "application/octet-stream";
            java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[8192]; int k; while ((k = asset.read(buf)) != -1) body.write(buf, 0, k); asset.close();
            byte[] bytes = body.toByteArray(); out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + mime + "\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII")); out.write(bytes); out.flush();
        } catch (Exception ignored) {}
    }

    private void installBridge() {
        runtime.getWebExtensionController().enableExtensionProcessSpawning();
        runtime.getWebExtensionController()
                .installBuiltIn("resource://android/assets/gecko_bridge/manifest.json")
                .accept(extension -> {
                    if (extension == null) {
                        Log.e(TAG, "Built-in bridge installation returned null");
                        return;
                    }
                    Log.i(TAG, "Bridge installed: " + extension.id + " at " + extension.location);
                    extension.setMessageDelegate(new WebExtension.MessageDelegate() {
                        @Override public GeckoResult<Object> onMessage(String message, Object payload,
                                WebExtension.MessageSender sender) {
                            Log.i(TAG, "Native message: " + message + " payload=" + payload);
                            String method = "";
                            if (payload instanceof org.json.JSONObject) {
                                method = ((org.json.JSONObject) payload).optString("method", "");
                            } else if (payload instanceof GeckoBundle) {
                                method = ((GeckoBundle) payload).getString("method", "");
                            }
                            if (!method.isEmpty()) {
                                Log.i(TAG, "Bridge method: " + method);
                                if ("startScan".equals(method)) {
                                    try { String result = performScan().toString(); Log.i(TAG, "Gecko scan findings: " + result.length()); return GeckoResult.fromValue(result); }
                                    catch (Exception e) { Log.e(TAG, "Gecko scan failed", e); return GeckoResult.fromValue("{\"error\":\"scan_failed\"}"); }
                                }
                                if ("exportPdf".equals(method)) {
                                    String source = payloadValue(payload);
                                    try {
                                        geckoReport = createDetailedPdf(source);
                                        runOnUiThread(() -> {
                                            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                            i.setType("application/pdf");
                                            i.putExtra(Intent.EXTRA_TITLE, "SafeScan-report.pdf");
                                            i.addCategory(Intent.CATEGORY_OPENABLE);
                                            startActivityForResult(i, SAVE_GECKO_PDF);
                                        });
                                    } catch (Exception e) { Log.e(TAG, "Gecko PDF failed", e); }
                                }
                                if ("backup".equals(method)) {
                                    try {
                                        org.json.JSONObject backup = new org.json.JSONObject();
                                        backup.put("format", "safescan-backup"); backup.put("version", 1);
                                        backup.put("created", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
                                        backup.put("device", deviceInfo()); backup.put("scan", payloadValue(payload));
                                        geckoReport = backup.toString(2).getBytes("UTF-8");
                                        runOnUiThread(() -> { Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/json"); i.putExtra(Intent.EXTRA_TITLE, "SafeScan-backup.safescan"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, SAVE_GECKO_BACKUP); });
                                    } catch (Exception e) { Log.e(TAG, "Gecko backup failed", e); }
                                }
                                if ("restoreBackup".equals(method)) {
                                    runOnUiThread(() -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/json"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, OPEN_GECKO_BACKUP); });
                                }
                                if ("openSettings".equals(method)) {
                                    try { startActivity(new Intent(payloadValue(payload))); } catch (Exception e) { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
                                }
                                if ("repairPlan".equals(method)) {
                                    startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                                }
                                if ("updateApp".equals(method)) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Deus73/SafeScan-Android/releases/latest")));
                                }
                                if ("shareApp".equals(method)) {
                                    Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain");
                                    i.putExtra(Intent.EXTRA_TEXT, "https://github.com/Deus73/SafeScan-Android");
                                    startActivity(Intent.createChooser(i, tr("share.chooser")));
                                }
                                if ("emailPdf".equals(method)) {
                                    try {
                                        byte[] pdf = createDetailedPdf(payloadValue(payload));
                                        java.io.File f = new java.io.File(getCacheDir(), "SafeScan-report.pdf");
                                        try (OutputStream out = new java.io.FileOutputStream(f)) { out.write(pdf); }
                                        Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
                                        i.putExtra(Intent.EXTRA_SUBJECT, tr("email.subject", "device", Build.MANUFACTURER + " " + Build.MODEL));
                                        i.putExtra(Intent.EXTRA_TEXT, tr("email.body", "device", Build.MANUFACTURER + " " + Build.MODEL));
                                        startActivity(Intent.createChooser(i, tr("email.chooser")));
                                    } catch (Exception e) { Log.e(TAG, "Gecko email PDF failed", e); }
                                }
                            }
                            return GeckoResult.fromValue(null);
                        }
                    }, "safescan");
                });
    }

    private String payloadValue(Object payload) {
        if (payload instanceof org.json.JSONObject) return ((org.json.JSONObject) payload).optString("value", "");
        if (payload instanceof GeckoBundle) return ((GeckoBundle) payload).getString("value", "");
        return "";
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == SAVE_GECKO_PDF || requestCode == SAVE_GECKO_BACKUP) && resultCode == RESULT_OK && data != null && geckoReport != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) { out.write(geckoReport); }
            catch (Exception e) { Log.e(TAG, "Gecko PDF save failed", e); }
            return;
        }
        if (requestCode == OPEN_GECKO_BACKUP && resultCode == RESULT_OK && data != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData()); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                BackupValidator.parse(out.toString("UTF-8")); Log.i(TAG, "Gecko backup validated");
            } catch (Exception e) { Log.w(TAG, "Gecko backup rejected", e); }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String read(String name) throws Exception {
        InputStream in = getAssets().open("site/" + name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close(); return out.toString("UTF-8");
    }

    private String inlineSite() {
        try {
            String html = read("index.html");
            html = html.replace("<link rel=\"stylesheet\" href=\"style.css\">", "<style>" + read("style.css") + "</style>");
            html = html.replace("<script src=\"app.js\"></script>", "<script>" + read("app.js") + "</script>");
            html = html.replace("<script src=\"i18n.js\"></script>", "<script>" + read("i18n.js") + "</script>");
            String bridge = "<script>(function(){if(window.SafeScanAndroid)return;function c(m,v){var u='safescan://'+encodeURIComponent(m);if(v!==undefined)u+='?value='+encodeURIComponent(v);location.href=u;}window.SafeScanAndroid={setLocale:function(v){c('setLocale',v)},startScan:function(v){c('startScan',v)},exportPdf:function(v){c('exportPdf',v)},emailPdf:function(v){c('emailPdf',v)},backup:function(v){c('backup',v)},restoreBackup:function(){c('restoreBackup')},repairPlan:function(){c('repairPlan')},shareApp:function(){c('shareApp')},updateApp:function(){c('updateApp')},openSettings:function(v){c('openSettings',v)}}})()</script>";
            html = html.replace("</head>", bridge + "</head>");
            return html;
        } catch (Exception e) {
            return "<!doctype html><meta charset='utf-8'><body><h1>SafeScan</h1><p>GeckoView asset loading failed.</p></body>";
        }
    }

    @Override protected void onDestroy() {
        if (assetServer != null) try { assetServer.close(); } catch (Exception ignored) {}
        if (session != null) session.close();
        super.onDestroy();
    }
}
