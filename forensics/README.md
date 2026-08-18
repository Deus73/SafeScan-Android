# AndroidQF + MVT

```text
 ____        __      ____
/ __/__  ___/ /_____/ __/  DEUS_GROUP
\ \/ _ \/ _  / __/ _ \__ \   SafeScan
/_/\___/\_,_/\__/\___/___/   AndroidQF + MVT
```

Gebruik dit alleen op je eigen toestel of met uitdrukkelijke toestemming.

## Linux — kopiëren en plakken

1. Open een terminal. Op Debian/Ubuntu installeert het script ADB, Python en AndroidQF automatisch; je wordt om je sudo-wachtwoord gevraagd.
2. Zet op de telefoon tijdelijk **USB-debugging** aan en accepteer de computer.
3. Voer uit vanuit deze map:

```bash
./install-and-scan.sh
```

Typ `JA` wanneer daarom wordt gevraagd. De resultaten komen in een nieuwe map `safescan-forensics-*`.

## Windows — PowerShell

1. Installeer WSL2 Ubuntu: `wsl --install -d Ubuntu`, herstart Windows en open Ubuntu.
2. Installeer ADB in Ubuntu: `sudo apt update && sudo apt install -y adb python3-venv curl`.
3. Zet USB-debugging aan en controleer in Ubuntu met `adb devices` of het toestel zichtbaar is.
4. Open PowerShell in deze map:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\install-and-scan.ps1
```

Typ `JA`. De resultaten worden in `safescan-forensics-*` opgeslagen.

AndroidQF verzamelt; MVT analyseert. De scan verwijdert niets en maakt geen automatische wijzigingen. Controleer rapporten vóór delen op toestel- en accountgegevens.

ASCII-logo: [`safescan-logo.ascii`](safescan-logo.ascii)
