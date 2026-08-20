#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
python3 tools/validate-i18n.py
python3 tools/test-backup-validator.py
if grep -RInE 'toast\(['"'"'](Beveilig|Backup|Rapport|Herstel|PDF|Scan|Ongeldige)' app/src/main/java app/src/main/assets/site --include='*.java' --include='*.js' >/tmp/safescan-runtime-string-audit.txt; then
  echo 'Runtime hardcoded UI strings found:' >&2
  cat /tmp/safescan-runtime-string-audit.txt >&2
  exit 2
fi
./build-apk.sh
test -s SafeScan.apk
sha256sum SafeScan.apk
