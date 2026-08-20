package nl.safescan.local;

import org.json.JSONObject;

/** Strict validation for the stable SafeScan backup envelope. */
final class BackupValidator {
    private BackupValidator() {}

    static JSONObject parse(String raw) throws Exception {
        if (raw == null || raw.trim().isEmpty()) throw new IllegalArgumentException("empty backup");
        JSONObject value = new JSONObject(raw);
        if (!"safescan-backup".equals(value.optString("format", null))) throw new IllegalArgumentException("wrong format");
        int version = value.optInt("version", -1);
        if (version != 1) throw new IllegalArgumentException("unsupported version");
        requireText(value, "created");
        requireText(value, "device");
        requireText(value, "scan");
        return value;
    }

    private static void requireText(JSONObject value, String key) {
        Object item = value.opt(key);
        if (!(item instanceof String) || ((String) item).trim().isEmpty()) {
            throw new IllegalArgumentException("missing " + key);
        }
    }
}
