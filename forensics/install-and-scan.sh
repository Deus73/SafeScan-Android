#!/usr/bin/env bash
set -euo pipefail
out="${1:-./safescan-forensics-$(date +%Y%m%d-%H%M%S)}"
command -v adb >/dev/null || { echo "Installeer eerst Android SDK Platform Tools."; exit 1; }
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
  echo "Download AndroidQF via de officiële releasepagina en voeg het bestand toe aan PATH."
  echo "https://github.com/mvt-project/androidqf/releases"
  exit 1
fi
androidqf -fast -output "$out"
"$venv/bin/mvt-android" check-androidqf "$out" | tee "$out/mvt-summary.txt"
echo "Klaar. Bewaar $out als forensisch bewijsmateriaal."
