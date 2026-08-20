package nl.safescan.local;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Single native translation entry point. Locale files are shared with the WebView. */
final class I18n {
    private static final String FALLBACK = "en";
    private final Context context;
    private final Map<String, Map<String, String>> cache = new HashMap<>();

    I18n(Context context) { this.context = context.getApplicationContext(); }

    String t(String locale, String key, Object... args) {
        String value = load(normalize(locale), key);
        if (value == null) value = load(FALLBACK, key);
        if (value == null) value = load("nl", key);
        if (value == null) return key;
        for (int i = 0; i + 1 < args.length; i += 2) {
            value = value.replace("{" + String.valueOf(args[i]) + "}", String.valueOf(args[i + 1]));
        }
        return value;
    }

    static String normalize(String locale) {
        if (locale == null || locale.trim().isEmpty()) return "nl";
        String value = locale.toLowerCase(Locale.ROOT).replace('_', '-');
        value = value.substring(0, Math.min(2, value.length()));
        return "nl,en,de,fr,es,it,pt,pl".contains(value) ? value : "nl";
    }

    private String load(String locale, String key) {
        try {
            Map<String, String> values = cache.get(locale);
            if (values == null) {
                values = new HashMap<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        context.getAssets().open("site/i18n/" + locale + ".json"), "UTF-8"));
                JSONObject json = new JSONObject(readAll(reader));
                java.util.Iterator<String> names = json.keys();
                while (names.hasNext()) { String name = names.next(); values.put(name, json.optString(name, "")); }
                cache.put(locale, values);
            }
            String value = values.get(key);
            return value == null || value.trim().isEmpty() ? null : value;
        } catch (Exception ignored) { return null; }
    }

    private static String readAll(BufferedReader reader) throws Exception {
        StringBuilder out = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close(); return out.toString();
    }
}
