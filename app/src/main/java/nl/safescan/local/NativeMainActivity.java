package nl.safescan.local;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.content.Intent;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import android.graphics.drawable.GradientDrawable;

/** Native UI host using the existing SafeScan scan engine; no WebView required. */
public final class NativeMainActivity extends MainActivity {
    private TextView results;
    private TextView score;
    private byte[] pendingFile;
    private String detailedReport;
    private static final int SAVE_PDF = 501, SAVE_BACKUP = 502, OPEN_BACKUP = 503;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        i18n = new I18n(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 32, 28, 24);
        root.setBackgroundResource(R.drawable.native_background);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(tr("app.title"), 28, Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(28);
        close.setTextColor(Color.WHITE);
        close.setAllCaps(false);
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setContentDescription(tr("button.close"));
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(64, 64));
        root.addView(header);
        TextView subtitle = text(tr("app.subtitle"), 16, Color.LTGRAY);
        root.addView(subtitle);

        Spinner languages = new Spinner(this);
        String[] languageLabels = {"Nederlands", "English", "Deutsch", "Français", "Español", "Italiano", "Português", "Polski"};
        String[] languageCodes = {"nl", "en", "de", "fr", "es", "it", "pt", "pl"};
        languages.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languageLabels));
        String saved = getSharedPreferences("safescan", MODE_PRIVATE).getString("locale", "nl");
        for (int i = 0; i < languageCodes.length; i++) if (languageCodes[i].equals(saved)) languages.setSelection(i);
        final boolean[] ready = {false};
        languages.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (ready[0] && !languageCodes[pos].equals(saved)) { getSharedPreferences("safescan", MODE_PRIVATE).edit().putString("locale", languageCodes[pos]).apply(); recreate(); }
                ready[0] = true;
            }
        });
        root.addView(languages, new LinearLayout.LayoutParams(-1, -2));

        Button scan = new Button(this);
        scan.setText(tr("button.scan"));
        styleButton(scan, true);
        scan.setOnClickListener(v -> runScan());
        root.addView(scan, new LinearLayout.LayoutParams(-1, -2));

        score = text("", 38, Color.rgb(62, 211, 177));
        score.setGravity(Gravity.CENTER);
        score.setVisibility(View.GONE);
        score.setContentDescription(tr("pdf.score_prefix"));
        root.addView(score, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(10, 10, 10, 10);
        panel.setBackgroundResource(R.drawable.native_background);
        root.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        Button pdf = new Button(this); pdf.setText(tr("button.export_pdf")); styleButton(pdf, false); pdf.setOnClickListener(v -> exportPdf()); panel.addView(pdf);
        Button email = new Button(this); email.setText(tr("button.email_pdf")); styleButton(email, false); email.setOnClickListener(v -> emailPdf()); panel.addView(email);
        Button backup = new Button(this); backup.setText(tr("button.backup")); styleButton(backup, false); backup.setOnClickListener(v -> createBackup()); panel.addView(backup);
        Button restore = new Button(this); restore.setText(tr("button.restore_backup")); styleButton(restore, false); restore.setOnClickListener(v -> openRestore()); panel.addView(restore);
        Button advice = new Button(this); advice.setText(tr("button.advice")); styleButton(advice, false); advice.setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setTitle(tr("threat.title")).setMessage(tr("threat.intro")).setPositiveButton(tr("button.close"), null).show()); panel.addView(advice);
        Button forensics = new Button(this); forensics.setText(tr("button.forensics")); styleButton(forensics, false); forensics.setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setTitle(tr("button.forensics")).setMessage(boldCommands(tr("forensics.guide"))).setNeutralButton(tr("button.share"), (d, w) -> shareForensics()).setPositiveButton(tr("button.close"), null).show()); panel.addView(forensics);
        Button repair = new Button(this); repair.setText(tr("button.repair")); styleButton(repair, false); repair.setOnClickListener(v -> startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))); panel.addView(repair);
        Button share = new Button(this); share.setText(tr("button.share")); styleButton(share, false); share.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, "https://github.com/Deus73/SafeScan-Android"); startActivity(Intent.createChooser(i, tr("share.chooser"))); }); panel.addView(share);
        Button update = new Button(this); update.setText(tr("button.update")); styleButton(update, false); update.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Deus73/SafeScan-Android/releases/latest")))); panel.addView(update);
        Button about = new Button(this); about.setText(tr("button.about")); styleButton(about, false); about.setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setTitle(tr("button.about")).setMessage(tr("about.info")).setPositiveButton(tr("button.close"), null).show()); panel.addView(about);
        Button apkDownloads = new Button(this); apkDownloads.setText(tr("button.apk_downloads")); styleButton(apkDownloads, false); apkDownloads.setOnClickListener(v -> showApkDownloads()); panel.addView(apkDownloads);

        results = text(tr("scan.not_started"), 15, Color.WHITE);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView page = new ScrollView(this); page.addView(root); setContentView(page);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.START);
        v.setPadding(0, 12, 0, 12); return v;
    }

    private void styleButton(Button button, boolean primary) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? Color.argb(225, 62, 211, 177) : Color.argb(185, 24, 55, 70));
        bg.setCornerRadius(18f);
        button.setBackground(bg);
        button.setTextColor(primary ? Color.rgb(5, 25, 31) : Color.rgb(226, 255, 248));
        button.setAllCaps(false);
        button.setPadding(18, 12, 18, 12);
    }

    private CharSequence boldCommands(String value) {
        android.text.SpannableString styled = new android.text.SpannableString(value);
        String[] commands = {"./forensics/install-and-scan.sh", "./forensics/install-and-scan.ps1", "./forensics/mvt-all.sh", "git clone -q https://github.com/Deus73/SafeScan-Android /tmp/s&&/tmp/s/forensics/mvt-all.sh"};
        for (String command : commands) {
            int start = 0;
            while ((start = value.indexOf(command, start)) >= 0) {
                styled.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start + command.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start += command.length();
            }
        }
        return styled;
    }

    private void shareForensics() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, tr("forensics.guide"));
        startActivity(Intent.createChooser(share, tr("share.chooser")));
    }

    private void addToolButton(LinearLayout panel, String label, String file) {
        Button tool = new Button(this); tool.setText(label); styleButton(tool, false);
        tool.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Deus73/SafeScan-Android/raw/main/tools/" + file))));
        panel.addView(tool);
    }

    private void showApkDownloads() {
        LinearLayout choices = new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL); choices.setPadding(12, 4, 12, 4);
        addToolButton(choices, tr("button.oxygen"), "oxygen.apk");
        addToolButton(choices, tr("button.copier"), "copier.apk");
        addToolButton(choices, tr("button.clone"), "clone.apk");
        addToolButton(choices, tr("button.payload_dumper"), "payloaddumper.apk");
        new android.app.AlertDialog.Builder(this).setTitle(tr("button.apk_downloads")).setView(choices).setNegativeButton(tr("button.close"), null).show();
    }

    private void runScan() {
        score.setVisibility(View.GONE);
        results.setText(tr("scan.running"));
        new Thread(() -> {
            try {
                JSONObject scan = performScan();
                int finalScore = scan.optInt("score", 0);
                JSONArray findings = scan.optJSONArray("findings");
                StringBuilder out = new StringBuilder();
                if (findings != null) for (int i = 0; i < findings.length(); i++) {
                    JSONObject f = findings.optJSONObject(i);
                    if (f != null) out.append(f.optString("status")).append(" — ")
                            .append(f.optString("title")).append('\n')
                            .append(f.optString("detail")).append("\n\n");
                }
                detailedReport = buildDetailedReport(scan);
                runOnUiThread(() -> {
                    score.setText(tr("pdf.score_prefix") + " " + finalScore + "/100");
                    score.setVisibility(View.VISIBLE);
                    results.setText(out.length() == 0 ? tr("scan.no_findings") : out.toString());
                });
            } catch (Exception e) {
                runOnUiThread(() -> score.setVisibility(View.GONE));
                runOnUiThread(() -> results.setText(tr("scan.failed")));
            }
        }, "SafeScanNativeScan").start();
    }

    private void exportPdf() {
        try {
            if (detailedReport == null) { results.setText(tr("scan.first")); return; }
            pendingFile = createDetailedPdf(detailedReport);
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_TITLE, "SafeScan-report.pdf"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, SAVE_PDF);
        } catch (Exception e) { results.setText(tr("pdf.failed")); }
    }

    private void emailPdf() {
        try {
            if (detailedReport == null) { results.setText(tr("scan.first")); return; }
            byte[] pdf = createDetailedPdf(detailedReport);
            java.io.File f = new java.io.File(getCacheDir(), "SafeScan-rapport.pdf");
            try (OutputStream out = new java.io.FileOutputStream(f)) { out.write(pdf); }
            android.net.Uri uri = android.net.Uri.parse("content://nl.safescan.local.reports/SafeScan-rapport.pdf");
            Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_SUBJECT, tr("email.subject", "device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL));
            i.putExtra(Intent.EXTRA_TEXT, tr("email.body", "device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL));
            i.putExtra(Intent.EXTRA_STREAM, uri); i.setClipData(android.content.ClipData.newRawUri("SafeScan-report", uri)); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, tr("email.chooser")));
        } catch (Exception e) { results.setText(tr("email.failed")); }
    }

    private String buildDetailedReport(JSONObject scan) {
        StringBuilder report = new StringBuilder();
        report.append("SECTION|").append(tr("pdf.identification")).append('\n');
        report.append("KV|Rapporttijd|").append(cleanReport(scan.optString("time"))).append('\n');
        report.append("KV|Toestel|").append(cleanReport(scan.optString("device"))).append('\n');
        report.append("KV|Android|").append(cleanReport(scan.optString("android"))).append('\n');
        report.append("KV|SafeScan-versie|1.6.5\n");
        report.append("SECTION|").append(tr("pdf.summary")).append('\n');
        report.append("KV|").append(tr("pdf.score_prefix")).append('|').append(scan.optInt("score")).append("/100\n");
        report.append("KV|").append(tr("status.safe")).append('|').append(scan.optInt("safe")).append('\n');
        report.append("KV|").append(tr("status.warning")).append('|').append(scan.optInt("warn")).append('\n');
        report.append("KV|").append(tr("status.high_risk")).append('|').append(scan.optInt("high")).append('\n');
        String priority = scan.optInt("high") > 0 ? tr("report.priority.high")
                : scan.optInt("warn") > 0 ? tr("report.priority.warning") : tr("report.priority.safe");
        report.append("TEXT|").append(cleanReport(priority)).append('\n');
        report.append("SECTION|").append(tr("pdf.findings")).append('\n');
        JSONArray findings = scan.optJSONArray("findings");
        if (findings != null) for (int i = 0; i < findings.length(); i++) {
            JSONObject finding = findings.optJSONObject(i); if (finding == null) continue;
            report.append("FINDING|").append(cleanReport(finding.optString("level"))).append('|')
                    .append(cleanReport(finding.optString("status"))).append('|')
                    .append(cleanReport(finding.optString("title"))).append('\n');
            report.append("DETAIL|").append(cleanReport(finding.optString("detail"))).append('\n');
            report.append("ADVICE|").append(cleanReport(tr("advice.default_report"))).append('\n');
        }
        return report.toString();
    }

    private String cleanReport(String value) { return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim(); }

    private void createBackup() {
        try {
            JSONObject b = new JSONObject(); b.put("format", "safescan-backup"); b.put("version", 1);
            b.put("created", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
            b.put("device", deviceInfo()); b.put("scan", results.getText().toString());
            pendingFile = b.toString(2).getBytes("UTF-8");
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE, "SafeScan-backup.safescan"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, SAVE_BACKUP);
        } catch (Exception e) { results.setText(tr("backup.failed")); }
    }

    private void openRestore() { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/json"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, OPEN_BACKUP); }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        if ((request == SAVE_PDF || request == SAVE_BACKUP) && result == RESULT_OK && data != null && pendingFile != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) { out.write(pendingFile); results.setText(tr("backup.saved")); }
            catch (Exception e) { results.setText(tr("backup.failed")); }
        } else if (request == OPEN_BACKUP && result == RESULT_OK && data != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData()); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                BackupValidator.parse(out.toString("UTF-8")); results.setText(tr("restore.loaded"));
            } catch (Exception e) { results.setText(tr("restore.invalid")); }
        }
    }
}
