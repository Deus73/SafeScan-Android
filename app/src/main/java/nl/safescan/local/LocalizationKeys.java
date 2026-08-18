package nl.safescan.local;

import java.util.Locale;

/** Stable keys shared by scan payloads and the WebView translation layer. */
final class LocalizationKeys {
 static final String FINDING_SECURITY_PATCH="finding.security_patch";
 static final String STATUS_SAFE="status.safe";
 static final String STATUS_WARNING="status.warning";
 static final String STATUS_HIGH_RISK="status.high_risk";
 private LocalizationKeys() {}
 static String finding(String title){String k=title.toLowerCase(Locale.ROOT).replace("&","and").replaceAll("[^a-z0-9]+","_");return "finding."+k.replaceAll("^_|_$","");}
 static String status(String value){if("Veilig".equals(value))return "status.safe";if("Let op".equals(value))return "status.warning";if("Hoog risico".equals(value))return "status.high_risk";if("Niet toegankelijk".equals(value))return "status.not_accessible";return "status.neutral";}
}
