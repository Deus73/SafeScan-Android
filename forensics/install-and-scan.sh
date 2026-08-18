#!/usr/bin/env bash
set -euo pipefail
out="${1:-./safescan-forensics-$(date +%Y%m%d-%H%M%S)}"
if command -v apt-get >/dev/null; then sudo apt-get update && sudo apt-get install -y adb python3 python3-venv curl; fi
command -v adb >/dev/null || { echo "ADB ontbreekt. Installeer Android SDK Platform Tools."; exit 1; }
command -v python3 >/dev/null || { echo "Python 3 is vereist."; exit 1; }
mkdir -p "$out"
adb start-server >/dev/null
echo "Controleer het toestel en accepteer de USB-debugging-vingerafdruk."
adb devices
read -r -p "Doorgaan met forensische verzameling naar $out? (typ JA): " ok
[[ "$ok" == "JA" ]] || { echo "Geannuleerd."; exit 1; }
venv="$out/.venv"
python3 -m venv "$venv"
"$venv/bin/python" -m pip install --upgrade pip mvt
if ! command -v androidqf >/dev/null; then
  arch=$(uname -m); [ "$arch" = x86_64 ] && asset=androidqf_linux_amd64_1.8.3 || asset=androidqf_linux_arm64_1.8.3
  curl -fL "https://github.com/mvt-project/androidqf/releases/download/v1.8.3/$asset" -o "$out/androidqf"
  chmod +x "$out/androidqf"; androidqf="$out/androidqf"
else androidqf=$(command -v androidqf); fi
"$androidqf" -fast -output "$out"
"$venv/bin/mvt-android" check-androidqf "$out" | tee "$out/mvt-summary.txt"
echo "Klaar. Bewaar $out als forensisch bewijsmateriaal."
