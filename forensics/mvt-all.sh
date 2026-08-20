#!/usr/bin/env bash
set -Eeuo pipefail

# SafeScan: one-command AndroidQF + MVT collection and analysis.
# Usage: ./forensics/mvt-all.sh [output-directory]
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$PWD/SafeScan-MVT-$(date +%Y%m%d-%H%M%S)}"

command -v adb >/dev/null || { echo "ADB ontbreekt. Installeer Android SDK Platform Tools." >&2; exit 1; }
command -v python3 >/dev/null || { echo "Python 3 ontbreekt." >&2; exit 1; }
mkdir -p "$OUT"
adb start-server >/dev/null
adb devices
echo "Controleer USB-debugging op het toestel en typ JA om door te gaan."
read -r answer
[[ "$answer" == "JA" ]] || { echo "Geannuleerd."; exit 1; }

"$ROOT/forensics/install-and-scan.sh" "$OUT"
