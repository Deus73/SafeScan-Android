# AndroidQF + MVT

Gebruik dit alleen op je eigen toestel of met uitdrukkelijke toestemming.

## Linux — kopiëren en plakken

1. Installeer ADB, Python en AndroidQF. Download AndroidQF via de [officiële releases](https://github.com/mvt-project/androidqf/releases) en zet het programma in `PATH`.
2. Zet op de telefoon tijdelijk **USB-debugging** aan en accepteer de computer.
3. Voer uit vanuit deze map:

```bash
./install-and-scan.sh
```

Typ `JA` wanneer daarom wordt gevraagd. De resultaten komen in een nieuwe map `safescan-forensics-*`.

## Windows — PowerShell

1. Installeer **WSL2 Ubuntu**, Android SDK Platform-Tools en AndroidQF.
2. Installeer ADB ook binnen WSL (`sudo apt install adb python3-venv`).
3. Zet USB-debugging aan en controleer in WSL met `adb devices` of het toestel zichtbaar is.
4. Open PowerShell in deze map:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\install-and-scan.ps1
```

Typ `JA`. De resultaten worden in `safescan-forensics-*` opgeslagen.

AndroidQF verzamelt; MVT analyseert. De scan verwijdert niets en maakt geen automatische wijzigingen. Controleer rapporten vóór delen op toestel- en accountgegevens.
