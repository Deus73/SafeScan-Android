$ErrorActionPreference = 'Stop'
$logo = Join-Path $PSScriptRoot 'safescan-logo.ascii'
if (Test-Path $logo) { Get-Content $logo | Write-Host }
$out = if ($args.Count -gt 0) { $args[0] } else { "safescan-forensics-$((Get-Date).ToString('yyyyMMdd-HHmmss'))" }
Write-Host '[1/6] Windows/WSL en ADB controleren'
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Host 'ADB ontbreekt. Installeer Android SDK Platform-Tools en voeg platform-tools toe aan PATH.'; exit 1 }
if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) { Write-Host 'WSL ontbreekt. Installeer WSL (bij voorkeur Ubuntu) en start dit script opnieuw.'; exit 1 }
New-Item -ItemType Directory -Force -Path $out | Out-Null
adb start-server | Out-Null
Write-Host 'Verbonden toestellen:'
adb devices
$ok = Read-Host "Doorgaan met forensische verzameling naar $out? Typ JA"
if ($ok -ne 'JA') { Write-Host 'Geannuleerd.'; exit 1 }
Write-Host '[2/6] MVT-omgeving en AndroidQF voorbereiden'
$full = (Resolve-Path $out).Path
$linuxOut = (wsl.exe wslpath -a "$full").Trim()
$cmd = "set -e; command -v adb >/dev/null || { echo 'ADB ontbreekt binnen WSL; voer sudo apt update && sudo apt install -y adb uit.'; exit 1; }; mkdir -p '$linuxOut'; python3 -m venv '$linuxOut/.venv'; '$linuxOut/.venv/bin/python' -m pip install --upgrade pip mvt; curl -fL https://github.com/mvt-project/androidqf/releases/download/v1.8.3/androidqf_linux_amd64_1.8.3 -o '$linuxOut/androidqf'; chmod +x '$linuxOut/androidqf'; '$linuxOut/androidqf' -fast -output '$linuxOut' && '$linuxOut/.venv/bin/mvt-android' check-androidqf '$linuxOut' | tee '$linuxOut/mvt-summary.txt'"
wsl.exe bash -lc $cmd
Write-Host '[6/6] KLAAR — resultaten staan in' $full -ForegroundColor Green
