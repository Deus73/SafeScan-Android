$ErrorActionPreference = 'Stop'
$out = if ($args.Count -gt 0) { $args[0] } else { "safescan-forensics-$((Get-Date).ToString('yyyyMMdd-HHmmss'))" }
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Host 'ADB ontbreekt. Installeer Android SDK Platform-Tools en voeg platform-tools toe aan PATH.'; exit 1 }
if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) { Write-Host 'WSL ontbreekt. Installeer WSL (bij voorkeur Ubuntu) en start dit script opnieuw.'; exit 1 }
New-Item -ItemType Directory -Force -Path $out | Out-Null
adb start-server | Out-Null
Write-Host 'Verbonden toestellen:'
adb devices
$ok = Read-Host "Doorgaan met forensische verzameling naar $out? Typ JA"
if ($ok -ne 'JA') { Write-Host 'Geannuleerd.'; exit 1 }
$full = (Resolve-Path $out).Path
$linuxOut = (wsl.exe wslpath -a "$full").Trim()
$cmd = "set -e; command -v adb >/dev/null || { echo 'ADB ontbreekt binnen WSL; installeer platform-tools in WSL of gebruik de officiële Windows AndroidQF binary.'; exit 1; }; mkdir -p '$linuxOut'; python3 -m venv '$linuxOut/.venv'; '$linuxOut/.venv/bin/python' -m pip install --upgrade pip mvt; command -v androidqf || { echo 'Installeer AndroidQF vanuit https://github.com/mvt-project/androidqf/releases en voeg het toe aan PATH.'; exit 1; }; androidqf -fast -output '$linuxOut' && '$linuxOut/.venv/bin/mvt-android' check-androidqf '$linuxOut' | tee '$linuxOut/mvt-summary.txt'"
wsl.exe bash -lc $cmd
Write-Host "Klaar. Resultaten staan in $full" -ForegroundColor Green
