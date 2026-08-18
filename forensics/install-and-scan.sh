#!/usr/bin/env bash
set -euo pipefail
step(){ echo "[$(date '+%H:%M:%S')] $1"; }
logo="$(dirname "$0")/safescan-logo.ascii"
[ -f "$logo" ] && cat "$logo"
out="${1:-./safescan-forensics-$(date +%Y%m%d-%H%M%S)}"
missing=(); command -v adb >/dev/null || missing+=(adb); command -v python3 >/dev/null || missing+=(python3); command -v curl >/dev/null || missing+=(curl); python3 -m venv --help >/dev/null 2>&1 || missing+=(python3-venv)
if [ "${#missing[@]}" -gt 0 ]; then
  command -v apt-get >/dev/null || { echo "Ontbrekende dependencies: ${missing[*]}"; exit 1; }
  step "1/6 Dependencies installeren: ${missing[*]}"
  sudo apt-get update && sudo apt-get install -y adb python3 python3-venv curl || { echo "Dependency-installatie mislukt; controleer je APT-bronnen."; exit 1; }
fi
step "2/6 Android-toestel controleren"
command -v adb >/dev/null || { echo "ADB ontbreekt. Installeer Android SDK Platform Tools."; exit 1; }
command -v python3 >/dev/null || { echo "Python 3 is vereist."; exit 1; }
mkdir -p "$out"
adb start-server >/dev/null
echo "Controleer het toestel en accepteer de USB-debugging-vingerafdruk."
adb devices
read -r -p "Doorgaan met forensische verzameling naar $out? (typ JA): " ok
[[ "$ok" == "JA" ]] || { echo "Geannuleerd."; exit 1; }
step "3/6 MVT-omgeving voorbereiden"
venv="$out/.venv"
python3 -m venv "$venv"
"$venv/bin/python" -m pip install --upgrade pip mvt
chmod +x "$venv/bin/mvt-android" 2>/dev/null || true
step "4/6 AndroidQF ophalen/controleren"
if ! command -v androidqf >/dev/null; then
  arch=$(uname -m); [ "$arch" = x86_64 ] && asset=androidqf_linux_amd64_1.8.3 || asset=androidqf_linux_arm64_1.8.3
  curl -fL "https://github.com/mvt-project/androidqf/releases/download/v1.8.3/$asset" -o "$out/androidqf"
  chmod +x "$out/androidqf"; androidqf="$out/androidqf"
else androidqf=$(command -v androidqf); fi
step "5/6 AndroidQF verzamelt gegevens (dit kan lang duren)"
"$androidqf" -fast -output "$out"
step "6/6 MVT analyseert de verzamelde gegevens"
if [ -x "$venv/bin/mvt-android" ]; then
  "$venv/bin/mvt-android" check-androidqf "$out" | tee "$out/mvt-summary.txt"
else
  "$venv/bin/python" -m mvt.android.cli check-androidqf "$out" | tee "$out/mvt-summary.txt"
fi
step "KLAAR — rapport: $out/mvt-summary.txt"
